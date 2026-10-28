package com.cricket.live.controller;

import com.cricket.live.dto.Dtos.PlayerDto;
import com.cricket.live.dto.Dtos.PlayerStats;
import com.cricket.live.dto.Dtos.TeamDto;
import com.cricket.live.repository.PlayerRepository;
import com.cricket.live.repository.TeamRepository;
import com.cricket.live.service.LiveUpdateService;
import com.cricket.live.service.MatchMapper;
import com.cricket.live.service.MatchService;
import com.cricket.live.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CricketController {
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final MatchMapper mapper;
    private final StatsService stats;
    private final MatchService matchService;
    private final LiveUpdateService live;

    @GetMapping("/teams")
    public List<TeamDto> teams() {
        return teams.findAll().stream().map(mapper::team).toList();
    }

    @GetMapping("/teams/{id}/players")
    public List<PlayerDto> players(@PathVariable Long id) {
        return players.findByTeamIdOrderByBattingOrder(id).stream()
                .map(p -> new PlayerDto(p.getId(), p.getName(), p.getRole().name(), p.getBattingSkill(), p.getBowlingSkill()))
                .toList();
    }

    @GetMapping("/players/stats")
    public List<PlayerStats> playerStats(@RequestParam(defaultValue = "runs") String sort,
                                         @RequestParam(defaultValue = "25") int limit) {
        return stats.stats(sort, limit);
    }

    /** Real-time feed: the browser keeps this open and receives a "match" event after every delivery. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return live.subscribe();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "OK", "liveMatches", matchService.liveCount(), "subscribers", live.subscribers());
    }
}
