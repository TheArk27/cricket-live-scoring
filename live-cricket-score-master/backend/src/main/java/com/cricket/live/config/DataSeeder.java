package com.cricket.live.config;

import com.cricket.live.dto.Dtos.MatchSummary;
import com.cricket.live.model.CricketMatch;
import com.cricket.live.model.Player;
import com.cricket.live.model.Player.Role;
import com.cricket.live.model.Team;
import com.cricket.live.repository.MatchRepository;
import com.cricket.live.repository.PlayerRepository;
import com.cricket.live.repository.TeamRepository;
import com.cricket.live.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

import static com.cricket.live.service.MatchService.VENUES;

/** Fills an empty database with 6 teams, 66 players, finished results, live matches and a fixture. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cricket.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {
    private static final String[][] TEAMS = {
            {"Mumbai Mavericks", "MUM", "#1d4ed8"}, {"Chennai Chargers", "CHE", "#ca8a04"},
            {"Delhi Dynamos", "DEL", "#0891b2"}, {"Kolkata Kings", "KOL", "#7c3aed"},
            {"Hyderabad Hurricanes", "HYD", "#ea580c"}, {"Bengaluru Blasters", "BLR", "#be123c"}};
    private static final String[] FIRST = {"Aarav", "Rohan", "Vikram", "Arjun", "Kabir", "Ishaan", "Dev", "Rahul",
            "Siddharth", "Karan", "Aditya", "Yash", "Nikhil", "Pranav", "Harsh", "Manav", "Aryan", "Varun", "Kunal",
            "Rishi", "Tanmay", "Sahil", "Ayaan", "Rudra", "Neel", "Parth", "Dhruv", "Shaurya", "Vihaan", "Om"};
    private static final String[] LAST = {"Sharma", "Patil", "Iyer", "Khan", "Reddy", "Nair", "Singh", "Deshmukh",
            "Kulkarni", "Menon", "Gill", "Pandey", "Joshi", "Rao", "Chauhan", "Bhat", "Shetty", "Verma", "Mishra", "Kapoor"};
    private static final Role[] ROLES = {Role.BATTER, Role.BATTER, Role.BATTER, Role.BATTER, Role.WICKET_KEEPER,
            Role.ALL_ROUNDER, Role.ALL_ROUNDER, Role.BOWLER, Role.BOWLER, Role.BOWLER, Role.BOWLER};
    private static final int[] BATTING = {84, 80, 82, 76, 72, 66, 60, 40, 34, 28, 24};

    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final MatchRepository matchRepo;
    private final ScoringService scoring;

    @Override
    public void run(String... args) {
        if (teamRepo.count() > 0) return;
        Random r = new Random(42);
        Set<String> used = new HashSet<>();
        List<Team> teams = new ArrayList<>();
        for (String[] t : TEAMS) {
            Team team = teamRepo.save(new Team(t[0], t[1], t[2]));
            teams.add(team);
            for (int i = 0; i < 11; i++) {
                String name;
                do {
                    name = FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
                } while (!used.add(name));
                Player p = new Player();
                p.setName(name);
                p.setTeam(team);
                p.setRole(ROLES[i]);
                p.setBattingOrder(i + 1);
                p.setBattingSkill(BATTING[i] + r.nextInt(9) - 4);
                p.setBowlingSkill(switch (ROLES[i]) {
                    case BOWLER -> 74 + r.nextInt(16);
                    case ALL_ROUNDER -> 58 + r.nextInt(14);
                    default -> 15 + r.nextInt(20);
                });
                playerRepo.save(p);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        play(fixture(teams.get(0), teams.get(1), 10, VENUES.get(0), now.minusHours(3)), Integer.MAX_VALUE);
        play(fixture(teams.get(2), teams.get(3), 5, VENUES.get(1), now.minusHours(1)), Integer.MAX_VALUE);
        play(fixture(teams.get(4), teams.get(5), 20, VENUES.get(5), now.minusMinutes(40)), 150);
        play(fixture(teams.get(0), teams.get(2), 10, VENUES.get(2), now.minusMinutes(5)), 9);
        fixture(teams.get(1), teams.get(3), 10, VENUES.get(3), now.plusMinutes(2));
        log.info("Seeded {} teams, {} players and {} matches", teams.size(), playerRepo.count(), matchRepo.count());
    }

    private CricketMatch fixture(Team a, Team b, int overs, String venue, LocalDateTime at) {
        return matchRepo.save(CricketMatch.of(a, b, overs, venue, at, true));
    }

    private void play(CricketMatch m, int deliveries) {
        MatchSummary s = scoring.startMatch(m.getId());
        for (int i = 0; i < deliveries && !"COMPLETED".equals(s.status()); i++) {
            s = scoring.simulateBall(m.getId());
        }
    }
}
