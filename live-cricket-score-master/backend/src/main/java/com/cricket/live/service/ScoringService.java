package com.cricket.live.service;

import com.cricket.live.dto.Dtos.BallRequest;
import com.cricket.live.dto.Dtos.MatchSummary;
import com.cricket.live.exception.NotFoundException;
import com.cricket.live.model.*;
import com.cricket.live.model.BallEvent.Dismissal;
import com.cricket.live.model.BallEvent.Extra;
import com.cricket.live.model.CricketMatch.Status;
import com.cricket.live.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.cricket.live.service.MatchMapper.plural;

/**
 * Applies the rules of cricket to every delivery: runs, extras, strike rotation, overs,
 * bowler changes, wickets, innings changes and the final result.
 * Each match is updated under its own lock so the simulator and manual scorers never clash.
 */
@Service
@RequiredArgsConstructor
public class ScoringService {
    private static final String[] FOURS = {"%s drives it through the covers", "%s pulls it hard to the fence",
            "%s cuts it past point", "%s flicks it off the pads to the boundary"};
    private static final String[] SIXES = {"%s launches it over long-on", "%s clears deep midwicket with ease",
            "%s lofts it straight back over the bowler", "%s slogs it into the stands"};
    private static final String[] DOTS = {"no run, solid defence", "beaten outside off!", "straight to the fielder, no run",
            "left alone outside off"};

    private final MatchRepository matches;
    private final InningsRepository inningsRepo;
    private final BattingEntryRepository battingRepo;
    private final BowlingEntryRepository bowlingRepo;
    private final BallEventRepository ballRepo;
    private final PlayerRepository playerRepo;
    private final MatchMapper mapper;
    private final LiveUpdateService live;
    private final TransactionTemplate tx;

    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public MatchSummary startMatch(Long matchId) {
        return run(matchId, () -> doStart(matchId));
    }

    public MatchSummary recordBall(Long matchId, BallRequest ball) {
        return run(matchId, () -> doBall(matchId, ball));
    }

    public MatchSummary simulateBall(Long matchId) {
        return run(matchId, () -> doBall(matchId, null));
    }

    public MatchSummary setAutoPlay(Long matchId, boolean enabled) {
        return run(matchId, () -> {
            CricketMatch m = load(matchId);
            m.setAutoPlay(enabled);
            touch(m);
            return mapper.summary(m);
        });
    }

    private MatchSummary run(Long matchId, Supplier<MatchSummary> work) {
        MatchSummary summary;
        synchronized (locks.computeIfAbsent(matchId, k -> new Object())) {
            summary = tx.execute(status -> work.get());
        }
        live.broadcast("match", summary);
        return summary;
    }

    private CricketMatch load(Long id) {
        return matches.findById(id).orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }

    private void touch(CricketMatch m) {
        m.setUpdatedAt(LocalDateTime.now());
    }

    // ---------------------------------------------------------------- start

    private MatchSummary doStart(Long id) {
        CricketMatch m = load(id);
        if (m.getStatus() != Status.SCHEDULED) throw new IllegalStateException("Match has already started");
        boolean aWinsToss = random.nextBoolean();
        Team toss = aWinsToss ? m.getTeamA() : m.getTeamB();
        Team other = aWinsToss ? m.getTeamB() : m.getTeamA();
        boolean bat = random.nextBoolean();
        m.setTossWinner(toss);
        m.setTossDecision(bat ? "bat" : "bowl");
        m.setStatus(Status.LIVE);
        m.setStartedAt(LocalDateTime.now());
        m.setCurrentInnings(1);
        touch(m);
        openInnings(m, 1, bat ? toss : other, bat ? other : toss, null);
        return mapper.summary(m);
    }

    private void openInnings(CricketMatch m, int number, Team batting, Team bowling, Integer target) {
        Innings inn = new Innings();
        inn.setMatch(m);
        inn.setNumber(number);
        inn.setBattingTeam(batting);
        inn.setBowlingTeam(bowling);
        inn.setTarget(target);
        inn = inningsRepo.save(inn);
        List<Player> order = playerRepo.findByTeamIdOrderByBattingOrder(batting.getId());
        inn.setStrikerId(addBatter(inn, order.get(0), 1));
        inn.setNonStrikerId(addBatter(inn, order.get(1), 2));
        inn.setBowlerId(nextBowler(inn, m.getOvers()));
    }

    private Long addBatter(Innings inn, Player p, int position) {
        BattingEntry e = new BattingEntry();
        e.setInnings(inn);
        e.setPlayer(p);
        e.setPosition(position);
        battingRepo.save(e);
        return p.getId();
    }

    /** Picks the next bowler: not the one who bowled the last over, within the over quota, least used first. */
    private Long nextBowler(Innings inn, int overs) {
        List<Player> team = playerRepo.findByTeamIdOrderByBattingOrder(inn.getBowlingTeam().getId());
        Map<Long, Integer> balls = new HashMap<>();
        bowlingRepo.findByInningsIdOrderById(inn.getId()).forEach(b -> balls.put(b.getPlayer().getId(), b.getLegalBalls()));
        int quota = (int) Math.ceil(overs / 5.0) * 6;
        Long previous = inn.getPreviousBowlerId();
        Comparator<Player> leastUsed = Comparator.<Player>comparingInt(p -> balls.getOrDefault(p.getId(), 0))
                .thenComparing(Comparator.comparingInt(Player::getBowlingSkill).reversed());
        Player pick = team.stream()
                .filter(p -> p.getBowlingSkill() >= 50 && !p.getId().equals(previous))
                .filter(p -> balls.getOrDefault(p.getId(), 0) < quota)
                .min(leastUsed)
                .orElseGet(() -> team.stream().filter(p -> !p.getId().equals(previous)).min(leastUsed).orElseThrow());
        if (!balls.containsKey(pick.getId())) {
            BowlingEntry e = new BowlingEntry();
            e.setInnings(inn);
            e.setPlayer(pick);
            bowlingRepo.save(e);
        }
        return pick.getId();
    }

    // ---------------------------------------------------------------- delivery

    private MatchSummary doBall(Long id, BallRequest input) {
        CricketMatch m = load(id);
        if (m.getStatus() != Status.LIVE) throw new IllegalStateException("Match is not live");
        Innings inn = inningsRepo.findByMatchIdAndNumber(id, m.getCurrentInnings()).orElseThrow();
        BattingEntry striker = battingRepo.findByInningsIdAndPlayerId(inn.getId(), inn.getStrikerId()).orElseThrow();
        BowlingEntry bowler = bowlingRepo.findByInningsIdAndPlayerId(inn.getId(), inn.getBowlerId()).orElseThrow();
        BallRequest ball = input != null ? input
                : BallSimulator.next(striker.getPlayer(), bowler.getPlayer(), inn.getLegalBalls(), m.getOvers(), random);

        Extra extra = ball.extra() == null ? Extra.NONE : ball.extra();
        validate(ball, extra);
        int runs = ball.runs();
        boolean legal = extra != Extra.WIDE && extra != Extra.NO_BALL;
        int total = runs + (legal ? 0 : 1);
        String overLabel = inn.getLegalBalls() / 6 + "." + (inn.getLegalBalls() % 6 + 1);
        String batName = striker.getPlayer().getName();
        String bowlName = bowler.getPlayer().getName();

        inn.setRuns(inn.getRuns() + total);
        inn.setPartnershipRuns(inn.getPartnershipRuns() + total);
        switch (extra) {
            case WIDE -> inn.setWides(inn.getWides() + total);
            case NO_BALL -> inn.setNoBalls(inn.getNoBalls() + 1);
            case BYE -> inn.setByes(inn.getByes() + runs);
            case LEG_BYE -> inn.setLegByes(inn.getLegByes() + runs);
            default -> { }
        }
        if (extra == Extra.NONE || extra == Extra.NO_BALL) {
            striker.setRuns(striker.getRuns() + runs);
            if (runs == 4) striker.setFours(striker.getFours() + 1);
            if (runs == 6) striker.setSixes(striker.getSixes() + 1);
        }
        if (extra != Extra.WIDE) striker.setBalls(striker.getBalls() + 1);
        if (extra != Extra.BYE && extra != Extra.LEG_BYE) {
            bowler.setRuns(bowler.getRuns() + total);
            inn.setCurrentOverRuns(inn.getCurrentOverRuns() + total);
        }
        if (legal) {
            bowler.setLegalBalls(bowler.getLegalBalls() + 1);
            inn.setLegalBalls(inn.getLegalBalls() + 1);
            inn.setPartnershipBalls(inn.getPartnershipBalls() + 1);
        }

        String text;
        if (ball.wicket()) {
            Dismissal how = ball.dismissal() == null ? Dismissal.BOWLED : ball.dismissal();
            String desc = describe(how, inn, bowler.getPlayer());
            striker.setDismissed(true);
            striker.setDismissal(desc);
            inn.setWickets(inn.getWickets() + 1);
            if (how != Dismissal.RUN_OUT) bowler.setWickets(bowler.getWickets() + 1);
            inn.setPartnershipRuns(0);
            inn.setPartnershipBalls(0);
            text = "OUT! " + batName + " " + desc + " - " + striker.getRuns() + " (" + striker.getBalls() + ")";
            inn.setStrikerId(nextBatter(inn));
        } else {
            text = describeRuns(extra, runs, batName);
            if (runs % 2 == 1) swapStrike(inn);
        }

        BallEvent event = new BallEvent();
        event.setInnings(inn);
        event.setOverLabel(overLabel);
        event.setSymbol(symbol(extra, runs, ball.wicket()));
        event.setTotalRuns(total);
        event.setWicket(ball.wicket());
        event.setCommentary(bowlName + " to " + batName + ", " + text);
        event.setCreatedAt(LocalDateTime.now());
        ballRepo.save(event);

        int teamSize = playerRepo.findByTeamIdOrderByBattingOrder(inn.getBattingTeam().getId()).size();
        boolean allOut = inn.getWickets() >= teamSize - 1 || inn.getStrikerId() == null;
        boolean oversDone = inn.getLegalBalls() >= m.getOvers() * 6;
        boolean chased = inn.getTarget() != null && inn.getRuns() >= inn.getTarget();
        boolean overEnd = legal && inn.getLegalBalls() % 6 == 0;
        if (overEnd) {
            if (inn.getCurrentOverRuns() == 0) bowler.setMaidens(bowler.getMaidens() + 1);
            inn.setCurrentOverRuns(0);
        }

        if (allOut || oversDone || chased) {
            inn.setCompleted(true);
            if (inn.getNumber() == 1) {
                m.setCurrentInnings(2);
                openInnings(m, 2, inn.getBowlingTeam(), inn.getBattingTeam(), inn.getRuns() + 1);
            } else {
                finish(m, inn, teamSize);
            }
        } else if (overEnd) {
            swapStrike(inn);
            inn.setPreviousBowlerId(inn.getBowlerId());
            inn.setBowlerId(nextBowler(inn, m.getOvers()));
        }
        touch(m);
        return mapper.summary(m);
    }

    private void validate(BallRequest b, Extra extra) {
        if (b.runs() < 0 || b.runs() > 6) throw new IllegalArgumentException("Runs must be between 0 and 6");
        if ((extra == Extra.BYE || extra == Extra.LEG_BYE) && b.runs() == 0) {
            throw new IllegalArgumentException("Byes and leg byes need at least 1 run");
        }
        if (b.wicket() && extra == Extra.NO_BALL && b.dismissal() != Dismissal.RUN_OUT) {
            throw new IllegalArgumentException("Only a run out is possible off a no-ball");
        }
        if (b.wicket() && extra == Extra.WIDE && b.dismissal() != Dismissal.RUN_OUT && b.dismissal() != Dismissal.STUMPED) {
            throw new IllegalArgumentException("Only run out or stumped is possible off a wide");
        }
    }

    private Long nextBatter(Innings inn) {
        List<BattingEntry> batted = battingRepo.findByInningsIdOrderByPosition(inn.getId());
        Set<Long> ids = new HashSet<>();
        batted.forEach(b -> ids.add(b.getPlayer().getId()));
        return playerRepo.findByTeamIdOrderByBattingOrder(inn.getBattingTeam().getId()).stream()
                .filter(p -> !ids.contains(p.getId()))
                .findFirst()
                .map(p -> addBatter(inn, p, batted.size() + 1))
                .orElse(null);
    }

    private void swapStrike(Innings inn) {
        Long s = inn.getStrikerId();
        inn.setStrikerId(inn.getNonStrikerId());
        inn.setNonStrikerId(s);
    }

    private void finish(CricketMatch m, Innings second, int teamSize) {
        Innings first = inningsRepo.findByMatchIdAndNumber(m.getId(), 1).orElseThrow();
        String result;
        if (second.getRuns() >= second.getTarget()) {
            m.setWinner(second.getBattingTeam());
            int balls = m.getOvers() * 6 - second.getLegalBalls();
            result = second.getBattingTeam().getName() + " won by " + plural(teamSize - 1 - second.getWickets(), "wicket")
                    + " (" + plural(balls, "ball") + " left)";
        } else if (second.getRuns() == first.getRuns()) {
            result = "Match tied";
        } else {
            m.setWinner(first.getBattingTeam());
            result = first.getBattingTeam().getName() + " won by " + plural(first.getRuns() - second.getRuns(), "run");
        }
        m.setResultText(result);
        m.setStatus(Status.COMPLETED);
        m.setCompletedAt(LocalDateTime.now());
        m.setAutoPlay(false);
    }

    private String describe(Dismissal how, Innings inn, Player bowler) {
        List<Player> fielders = playerRepo.findByTeamIdOrderByBattingOrder(inn.getBowlingTeam().getId());
        Player fielder = fielders.get(random.nextInt(fielders.size()));
        Player keeper = fielders.stream().filter(p -> p.getRole() == Player.Role.WICKET_KEEPER).findFirst().orElse(fielder);
        String b = bowler.getName();
        return switch (how) {
            case BOWLED -> "b " + b;
            case LBW -> "lbw b " + b;
            case CAUGHT -> fielder.getId().equals(bowler.getId()) ? "c & b " + b : "c " + fielder.getName() + " b " + b;
            case STUMPED -> "st " + keeper.getName() + " b " + b;
            case RUN_OUT -> "run out (" + fielder.getName() + ")";
        };
    }

    private String describeRuns(Extra extra, int runs, String bat) {
        return switch (extra) {
            case WIDE -> runs == 0 ? "wide." : "wide, and they run " + runs + ".";
            case NO_BALL -> "no-ball!" + (runs > 0 ? " " + plural(runs, "run") + " off the bat." : "");
            case BYE -> plural(runs, "bye") + ".";
            case LEG_BYE -> plural(runs, "leg bye") + ".";
            case NONE -> switch (runs) {
                case 0 -> pick(DOTS) + ".";
                case 4 -> "FOUR! " + String.format(pick(FOURS), bat) + ".";
                case 6 -> "SIX! " + String.format(pick(SIXES), bat) + ".";
                default -> plural(runs, "run") + ".";
            };
        };
    }

    private String symbol(Extra extra, int runs, boolean wicket) {
        if (wicket) return "W";
        return switch (extra) {
            case WIDE -> runs == 0 ? "wd" : (runs + 1) + "wd";
            case NO_BALL -> runs == 0 ? "nb" : runs + "nb";
            case BYE -> runs + "b";
            case LEG_BYE -> runs + "lb";
            case NONE -> String.valueOf(runs);
        };
    }

    private String pick(String[] options) {
        return options[random.nextInt(options.length)];
    }
}
