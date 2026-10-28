package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = @Index(columnList = "team_id"))
@Getter
@Setter
@NoArgsConstructor
public class Player {
    public enum Role { BATTER, BOWLER, ALL_ROUNDER, WICKET_KEEPER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role;

    @ManyToOne(optional = false)
    private Team team;

    private int battingSkill;
    private int bowlingSkill;
    private int battingOrder;
}
