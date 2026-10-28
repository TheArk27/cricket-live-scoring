package com.cricket.live.repository;

import com.cricket.live.model.BallEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {
    List<BallEvent> findByInningsMatchIdOrderByIdDesc(Long matchId, Pageable page);

    List<BallEvent> findByInningsIdOrderByIdDesc(Long inningsId, Pageable page);
}
