package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = @Index(columnList = "innings_id"))
@Getter
@Setter
@NoArgsConstructor
public class BowlingEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Innings innings;

    @ManyToOne(optional = false)
    private Player player;

    private int legalBalls;
    private int runs;
    private int wickets;
    private int maidens;
}
