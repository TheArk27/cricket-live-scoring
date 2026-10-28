package com.cricket.live.service;

import com.cricket.live.dto.Dtos.PlayerStats;
import com.cricket.live.repository.BattingEntryRepository;
import com.cricket.live.repository.BowlingEntryRepository;
import com.cricket.live.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.cricket.live.service.MatchMapper.round;

/** Career statistics aggregated in the database (GROUP BY), so it stays cheap as matches grow. */
@Service
@RequiredArgsConstructor
public class StatsService {
    private final PlayerRepository players;
    private final BattingEntryRepository batting;
    private final BowlingEntryRepository bowling;

    @Transactional(readOnly = true)
    public List<PlayerStats> stats(String sort, int limit) {
        Map<Long, long[]> bat = toMap(batting.aggregateByPlayer());
        Map<Long, long[]> bowl = toMap(bowling.aggregateByPlayer());
        List<PlayerStats> all = players.findAll().stream()
                .filter(p -> bat.containsKey(p.getId()) || bowl.containsKey(p.getId()))
                .map(p -> {
                    long[] b = bat.getOrDefault(p.getId(), new long[7]);
                    long[] w = bowl.getOrDefault(p.getId(), new long[4]);
                    long runs = b[1], balls = b[2], outs = b[6];
                    return new PlayerStats(p.getId(), p.getName(), p.getTeam().getShortName(), p.getTeam().getColor(),
                            p.getRole().name(), Math.max(b[0], w[0]), runs, balls, b[3], b[4], b[5],
                            round(outs == 0 ? runs : (double) runs / outs),
                            balls == 0 ? 0 : round(runs * 100.0 / balls),
                            w[3], MatchMapper.overs((int) w[1]), w[2], w[1] == 0 ? 0 : round(w[2] * 6.0 / w[1]));
                }).toList();

        Comparator<PlayerStats> order = switch (sort == null ? "runs" : sort) {
            case "wickets" -> Comparator.comparingLong(PlayerStats::wickets).reversed()
                    .thenComparingDouble(PlayerStats::economy);
            case "strikeRate" -> Comparator.comparingDouble(PlayerStats::strikeRate).reversed();
            case "economy" -> Comparator.comparingDouble(PlayerStats::economy);
            case "sixes" -> Comparator.comparingLong(PlayerStats::sixes).reversed();
            default -> Comparator.comparingLong(PlayerStats::runs).reversed();
        };
        return all.stream()
                .filter(s -> !"strikeRate".equals(sort) || s.balls() >= 10)
                .filter(s -> !"economy".equals(sort) || Double.parseDouble(s.overs()) >= 2)
                .sorted(order)
                .limit(Math.max(1, limit))
                .toList();
    }

    // row[0] is the player id, the rest are the aggregate columns
    private static Map<Long, long[]> toMap(List<Object[]> rows) {
        Map<Long, long[]> map = new HashMap<>();
        for (Object[] row : rows) {
            long[] values = new long[row.length - 1];
            for (int i = 1; i < row.length; i++) values[i - 1] = row[i] == null ? 0 : ((Number) row[i]).longValue();
            map.put(((Number) row[0]).longValue(), values);
        }
        return map;
    }
}
