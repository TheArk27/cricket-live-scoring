package com.cricket.live.service;

import com.cricket.live.dto.Dtos.*;
import com.cricket.live.model.*;
import com.cricket.live.model.CricketMatch.Status;
import com.cricket.live.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

/** Converts entities into the JSON shapes returned by the API. */
@Component
@RequiredArgsConstructor
public class MatchMapper {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final InningsRepository inningsRepo;
    private final BattingEntryRepository battingRepo;
    private final BowlingEntryRepository bowlingRepo;
    private final BallEventRepository ballRepo;
    private final PlayerRepository playerRepo;

    public static String overs(int balls) {
        return balls % 6 == 0 ? String.valueOf(balls / 6) : balls / 6 + "." + balls % 6;
    }

    static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    public TeamDto team(Team t) {
        return t == null ? null : new TeamDto(t.getId(), t.getName(), t.getShortName(), t.getColor());
    }

    public InningsScore score(Innings i) {
        return new InningsScore(i.getNumber(), i.getBattingTeam().getId(), i.getBattingTeam().getShortName(),
                i.getRuns(), i.getWickets(), overs(i.getLegalBalls()), i.getTarget(), i.isCompleted());
    }

    public MatchSummary summary(CricketMatch m) {
        return summary(m, inningsRepo.findByMatchIdOrderByNumber(m.getId()));
    }

    private MatchSummary summary(CricketMatch m, List<Innings> innings) {
        Innings cur = innings.stream().filter(i -> i.getNumber() == m.getCurrentInnings()).findFirst().orElse(null);
        Double crr = null;
        Double rrr = null;
        String text;
        if (m.getStatus() == Status.SCHEDULED) {
            text = "Match starts " + m.getScheduledAt().format(TIME);
        } else if (m.getStatus() == Status.COMPLETED) {
            text = m.getResultText();
        } else if (cur != null && cur.getTarget() != null) {
            int need = cur.getTarget() - cur.getRuns();
            int left = m.getOvers() * 6 - cur.getLegalBalls();
            text = cur.getBattingTeam().getShortName() + " need " + plural(need, "run") + " from " + plural(left, "ball");
            rrr = left > 0 ? round(need * 6.0 / left) : null;
        } else {
            text = toss(m);
        }
        if (cur != null && cur.getLegalBalls() > 0 && m.getStatus() == Status.LIVE) {
            crr = round(cur.getRuns() * 6.0 / cur.getLegalBalls());
        }
        return new MatchSummary(m.getId(), team(m.getTeamA()), team(m.getTeamB()), m.getVenue(), m.getOvers(),
                m.getStatus().name(), m.isAutoPlay(), m.getCurrentInnings(), innings.stream().map(this::score).toList(),
                text, toss(m), crr, rrr, m.getScheduledAt(), m.getUpdatedAt());
    }

    private String toss(CricketMatch m) {
        return m.getTossWinner() == null ? null
                : m.getTossWinner().getName() + " won the toss and chose to " + m.getTossDecision();
    }

    public MatchDetail detail(CricketMatch m) {
        List<Innings> innings = inningsRepo.findByMatchIdOrderByNumber(m.getId());
        List<InningsDetail> details = innings.stream().map(this::inningsDetail).toList();
        List<String> recent = new ArrayList<>();
        innings.stream().filter(i -> i.getNumber() == m.getCurrentInnings()).findFirst().ifPresent(i ->
                ballRepo.findByInningsIdOrderByIdDesc(i.getId(), PageRequest.of(0, 12))
                        .forEach(b -> recent.add(0, b.getSymbol())));
        List<BallDto> commentary = ballRepo.findByInningsMatchIdOrderByIdDesc(m.getId(), PageRequest.of(0, 30))
                .stream().map(this::ball).toList();
        return new MatchDetail(summary(m, innings), details, recent, commentary);
    }

    private InningsDetail inningsDetail(Innings i) {
        List<BattingEntry> bats = battingRepo.findByInningsIdOrderByPosition(i.getId());
        Set<Long> batted = new HashSet<>();
        List<BattingRow> batting = bats.stream().map(b -> {
            Long pid = b.getPlayer().getId();
            batted.add(pid);
            boolean atCrease = !i.isCompleted() && !b.isDismissed()
                    && (pid.equals(i.getStrikerId()) || pid.equals(i.getNonStrikerId()));
            return new BattingRow(pid, b.getPlayer().getName(), b.getRuns(), b.getBalls(), b.getFours(), b.getSixes(),
                    b.getBalls() == 0 ? 0 : round(b.getRuns() * 100.0 / b.getBalls()), b.isDismissed(),
                    b.getDismissal(), atCrease && pid.equals(i.getStrikerId()), atCrease);
        }).toList();
        List<String> yetToBat = playerRepo.findByTeamIdOrderByBattingOrder(i.getBattingTeam().getId()).stream()
                .filter(p -> !batted.contains(p.getId())).map(Player::getName).toList();
        List<BowlingRow> bowling = bowlingRepo.findByInningsIdOrderById(i.getId()).stream().map(w ->
                new BowlingRow(w.getPlayer().getId(), w.getPlayer().getName(), overs(w.getLegalBalls()), w.getMaidens(),
                        w.getRuns(), w.getWickets(), w.getLegalBalls() == 0 ? 0 : round(w.getRuns() * 6.0 / w.getLegalBalls()),
                        !i.isCompleted() && w.getPlayer().getId().equals(i.getBowlerId()))).toList();
        String extras = String.format("wd %d, nb %d, b %d, lb %d", i.getWides(), i.getNoBalls(), i.getByes(), i.getLegByes());
        String partnership = i.isCompleted() ? null : i.getPartnershipRuns() + " (" + i.getPartnershipBalls() + ")";
        return new InningsDetail(score(i), i.getExtras(), extras, batting, bowling, yetToBat, partnership);
    }

    public BallDto ball(BallEvent b) {
        return new BallDto(b.getId(), b.getInnings().getNumber(), b.getOverLabel(), b.getSymbol(), b.getTotalRuns(),
                b.isWicket(), b.getCommentary(), b.getCreatedAt());
    }
}
