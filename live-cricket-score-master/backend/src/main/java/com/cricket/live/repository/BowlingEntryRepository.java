package com.cricket.live.repository;

import com.cricket.live.model.BowlingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BowlingEntryRepository extends JpaRepository<BowlingEntry, Long> {
    List<BowlingEntry> findByInningsIdOrderById(Long inningsId);

    Optional<BowlingEntry> findByInningsIdAndPlayerId(Long inningsId, Long playerId);

    // [playerId, matches, balls, runs, wickets]
    @Query("select w.player.id, count(distinct w.innings.match.id), sum(w.legalBalls), sum(w.runs), sum(w.wickets) "
            + "from BowlingEntry w group by w.player.id")
    List<Object[]> aggregateByPlayer();
}
