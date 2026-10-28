package com.cricket.live.controller;

import com.cricket.live.dto.Dtos.*;
import com.cricket.live.model.CricketMatch.Status;
import com.cricket.live.service.MatchService;
import com.cricket.live.service.ScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;
    private final ScoringService scoring;

    @GetMapping
    public List<MatchSummary> list(@RequestParam(required = false) Status status) {
        return matchService.list(status);
    }

    @GetMapping("/{id}")
    public MatchDetail detail(@PathVariable Long id) {
        return matchService.detail(id);
    }

    @GetMapping("/{id}/commentary")
    public List<BallDto> commentary(@PathVariable Long id,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "30") int size) {
        return matchService.commentary(id, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchSummary create(@Valid @RequestBody CreateMatchRequest request) {
        return matchService.create(request);
    }

    @PostMapping("/{id}/start")
    public MatchSummary start(@PathVariable Long id) {
        return scoring.startMatch(id);
    }

    @PostMapping("/{id}/balls")
    public MatchSummary recordBall(@PathVariable Long id, @Valid @RequestBody BallRequest ball) {
        return scoring.recordBall(id, ball);
    }

    @PutMapping("/{id}/autoplay")
    public MatchSummary autoplay(@PathVariable Long id, @RequestParam boolean enabled) {
        return scoring.setAutoPlay(id, enabled);
    }
}
