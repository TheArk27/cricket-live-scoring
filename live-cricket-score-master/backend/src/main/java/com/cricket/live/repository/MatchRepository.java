package com.cricket.live.repository;

import com.cricket.live.model.CricketMatch;
import com.cricket.live.model.CricketMatch.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<CricketMatch, Long> {
    List<CricketMatch> findByStatus(Status status);

    List<CricketMatch> findByStatusAndScheduledAtBefore(Status status, LocalDateTime time);

    List<CricketMatch> findByStatusAndAutoPlayTrue(Status status);

    long countByStatus(Status status);
}
