package com.cricket.live.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The live score feed: bowls one delivery in every auto-play match on each tick. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cricket.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationEngine {
    private final MatchService matchService;
    private final ScoringService scoring;

    @Scheduled(fixedDelayString = "${cricket.simulation.tick-ms:2000}", initialDelay = 5000)
    public void tick() {
        try {
            matchService.startDueMatches();
            for (Long id : matchService.liveAutoPlayIds()) {
                try {
                    scoring.simulateBall(id);
                } catch (IllegalStateException e) {
                    // match finished or paused between the query and the delivery
                }
            }
            matchService.ensureUpcoming();
        } catch (Exception e) {
            log.warn("Simulation tick failed: {}", e.getMessage());
        }
    }
}
