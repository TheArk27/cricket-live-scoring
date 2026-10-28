package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(columnList = "innings_id"))
@Getter
@Setter
@NoArgsConstructor
public class BallEvent {
    public enum Extra { NONE, WIDE, NO_BALL, BYE, LEG_BYE }
    public enum Dismissal { BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Innings innings;

    @Column(length = 8)
    private String overLabel;

    @Column(length = 8)
    private String symbol;

    private int totalRuns;
    private boolean wicket;

    @Column(length = 300)
    private String commentary;

    private LocalDateTime createdAt;
}
