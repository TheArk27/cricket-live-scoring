package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "number"}))
@Getter
@Setter
@NoArgsConstructor
public class Innings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private CricketMatch match;

    private int number;

    @ManyToOne(optional = false)
    private Team battingTeam;

    @ManyToOne(optional = false)
    private Team bowlingTeam;

    private int runs;
    private int wickets;
    private int legalBalls;
    private int wides;
    private int noBalls;
    private int byes;
    private int legByes;
    private Integer target;
    private boolean completed;

    private Long strikerId;
    private Long nonStrikerId;
    private Long bowlerId;
    private Long previousBowlerId;
    private int partnershipRuns;
    private int partnershipBalls;
    private int currentOverRuns;

    public int getExtras() {
        return wides + noBalls + byes + legByes;
    }
}
