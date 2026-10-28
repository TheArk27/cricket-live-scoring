package com.cricket.live.repository;

import com.cricket.live.model.Innings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InningsRepository extends JpaRepository<Innings, Long> {
    List<Innings> findByMatchIdOrderByNumber(Long matchId);

    Optional<Innings> findByMatchIdAndNumber(Long matchId, int number);
}
