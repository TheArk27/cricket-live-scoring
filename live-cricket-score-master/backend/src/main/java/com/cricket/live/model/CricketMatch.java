package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cricket_match", indexes = @Index(columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
public class CricketMatch {
    public enum Status { SCHEDULED, LIVE, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Team teamA;

    @ManyToOne(optional = false)
    private Team teamB;

    private String venue;
    private int overs;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private Status status = Status.SCHEDULED;

    private boolean autoPlay = true;

    @ManyToOne
    private Team tossWinner;
    private String tossDecision;
    private int currentInnings;

    @ManyToOne
    private Team winner;
    private String resultText;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public static CricketMatch of(Team a, Team b, int overs, String venue, LocalDateTime at, boolean autoPlay) {
        CricketMatch m = new CricketMatch();
        m.teamA = a;
        m.teamB = b;
        m.overs = overs;
        m.venue = venue;
        m.scheduledAt = at;
        m.autoPlay = autoPlay;
        m.updatedAt = LocalDateTime.now();
        return m;
    }
}
