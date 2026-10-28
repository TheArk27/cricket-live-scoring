package com.cricket.live.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(nullable = false, length = 5)
    private String shortName;

    @Column(length = 9)
    private String color;

    public Team(String name, String shortName, String color) {
        this.name = name;
        this.shortName = shortName;
        this.color = color;
    }
}
