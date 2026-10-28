package com.cricket.live.service;

import com.cricket.live.dto.Dtos.*;
import com.cricket.live.exception.NotFoundException;
import com.cricket.live.model.CricketMatch;
import com.cricket.live.model.CricketMatch.Status;
import com.cricket.live.model.Team;
import com.cricket.live.repository.BallEventRepository;
import com.cricket.live.repository.MatchRepository;
import com.cricket.live.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchService {
    public static final List<String> VENUES = List.of("Wankhede Stadium, Mumbai", "Eden Gardens, Kolkata",
            "M. Chinnaswamy Stadium, Bengaluru", "MA Chidambaram Stadium, Chennai", "Arun Jaitley Stadium, Delhi",
            "Rajiv Gandhi Stadium, Hyderabad", "MCA Stadium, Pune");

    // live first, then upcoming (soonest first), then results (most recent first)
    private static final Comparator<CricketMatch> DISPLAY_ORDER = Comparator
            .comparingInt((CricketMatch m) -> m.getStatus() == Status.LIVE ? 0 : m.getStatus() == Status.SCHEDULED ? 1 : 2)
            .thenComparingLong(m -> m.getStatus() == Status.COMPLETED ? -m.getId() : m.getId());

    private final MatchRepository matches;
    private final TeamRepository teams;
    private final BallEventRepository balls;
    private final MatchMapper mapper;
    private final ScoringService scoring;
    private final LiveUpdateService live;
    private final Random random = new Random();

    @Value("${cricket.simulation.default-overs:10}")
    private int defaultOvers;

    @Value("${cricket.simulation.min-active:3}")
    private int minActive;

    @Transactional(readOnly = true)
    public List<MatchSummary> list(Status status) {
        List<CricketMatch> all = status == null ? matches.findAll() : matches.findByStatus(status);
        return all.stream().sorted(DISPLAY_ORDER).map(mapper::summary).toList();
    }

    @Transactional(readOnly = true)
    public MatchDetail detail(Long id) {
        return mapper.detail(load(id));
    }

    @Transactional(readOnly = true)
    public List<BallDto> commentary(Long id, int page, int size) {
        load(id);
        return balls.findByInningsMatchIdOrderByIdDesc(id, PageRequest.of(page, Math.min(size, 100)))
                .stream().map(mapper::ball).toList();
    }

    public MatchSummary create(CreateMatchRequest req) {
        if (req.teamAId().equals(req.teamBId())) throw new IllegalArgumentException("Pick two different teams");
        Team a = teams.findById(req.teamAId()).orElseThrow(() -> new NotFoundException("Team not found"));
        Team b = teams.findById(req.teamBId()).orElseThrow(() -> new NotFoundException("Team not found"));
        String venue = req.venue() == null || req.venue().isBlank() ? VENUES.get(random.nextInt(VENUES.size())) : req.venue().trim();
        LocalDateTime at = req.startNow() ? LocalDateTime.now() : LocalDateTime.now().plusMinutes(2);
        CricketMatch m = matches.save(CricketMatch.of(a, b, req.overs(), venue, at, req.autoPlay()));
        if (req.startNow()) return scoring.startMatch(m.getId());
        MatchSummary summary = mapper.summary(m);
        live.broadcast("match", summary);
        return summary;
    }

    /** Starts scheduled matches whose start time has passed. */
    public void startDueMatches() {
        for (CricketMatch m : matches.findByStatusAndScheduledAtBefore(Status.SCHEDULED, LocalDateTime.now())) {
            try {
                scoring.startMatch(m.getId());
            } catch (IllegalStateException ignored) {
                // started by someone else in the meantime
            }
        }
    }

    /** Keeps the platform busy: schedules a new fixture when too few are live or upcoming. */
    public void ensureUpcoming() {
        long active = matches.countByStatus(Status.LIVE) + matches.countByStatus(Status.SCHEDULED);
        if (active >= minActive) return;
        List<Team> all = new ArrayList<>(teams.findAll());
        if (all.size() < 2) return;
        Collections.shuffle(all, random);
        CricketMatch m = matches.save(CricketMatch.of(all.get(0), all.get(1), defaultOvers,
                VENUES.get(random.nextInt(VENUES.size())), LocalDateTime.now().plusMinutes(1), true));
        live.broadcast("match", mapper.summary(m));
    }

    public List<Long> liveAutoPlayIds() {
        return matches.findByStatusAndAutoPlayTrue(Status.LIVE).stream().map(CricketMatch::getId).toList();
    }

    public long liveCount() {
        return matches.countByStatus(Status.LIVE);
    }

    private CricketMatch load(Long id) {
        return matches.findById(id).orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }
}
