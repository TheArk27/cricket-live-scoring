package com.cricket.live.repository;

import com.cricket.live.model.BattingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BattingEntryRepository extends JpaRepository<BattingEntry, Long> {
    List<BattingEntry> findByInningsIdOrderByPosition(Long inningsId);

    Optional<BattingEntry> findByInningsIdAndPlayerId(Long inningsId, Long playerId);

    // [playerId, matches, runs, balls, fours, sixes, highest, outs]
    @Query("select b.player.id, count(distinct b.innings.match.id), sum(b.runs), sum(b.balls), sum(b.fours), "
            + "sum(b.sixes), max(b.runs), sum(case when b.dismissed = true then 1 else 0 end) "
            + "from BattingEntry b group by b.player.id")
    List<Object[]> aggregateByPlayer();
}
