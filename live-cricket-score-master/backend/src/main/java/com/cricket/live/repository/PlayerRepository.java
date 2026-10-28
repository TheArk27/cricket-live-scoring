package com.cricket.live.repository;

import com.cricket.live.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeamIdOrderByBattingOrder(Long teamId);
}
