package com.cricket.live;

import com.cricket.live.dto.Dtos.BallRequest;
import com.cricket.live.dto.Dtos.CreateMatchRequest;
import com.cricket.live.dto.Dtos.MatchSummary;
import com.cricket.live.model.BallEvent.Dismissal;
import com.cricket.live.model.BallEvent.Extra;
import com.cricket.live.model.BattingEntry;
import com.cricket.live.model.Innings;
import com.cricket.live.model.Team;
import com.cricket.live.repository.BattingEntryRepository;
import com.cricket.live.repository.InningsRepository;
import com.cricket.live.repository.TeamRepository;
import com.cricket.live.service.MatchService;
import com.cricket.live.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "cricket.simulation.enabled=false")
@ActiveProfiles("dev")
class ScoringServiceTest {
    @Autowired MatchService matchService;
    @Autowired ScoringService scoring;
    @Autowired TeamRepository teams;
    @Autowired InningsRepository innings;
    @Autowired BattingEntryRepository batting;

    private Long newMatch(int overs) {
        List<Team> t = teams.findAll();
        return matchService.create(new CreateMatchRequest(t.get(0).getId(), t.get(1).getId(), overs, "Test Ground", true, false)).id();
    }

    private Innings inn(Long matchId, int number) {
        return innings.findByMatchIdAndNumber(matchId, number).orElseThrow();
    }

    @Test
    void appliesRunsExtrasStrikeAndInningsChange() {
        Long id = newMatch(1);
        Innings start = inn(id, 1);
        Long opener = start.getStrikerId();
        Long partner = start.getNonStrikerId();

        scoring.recordBall(id, new BallRequest(4, Extra.NONE, false, null));
        scoring.recordBall(id, new BallRequest(0, Extra.WIDE, false, null));
        scoring.recordBall(id, new BallRequest(1, Extra.NONE, false, null));

        Innings i = inn(id, 1);
        assertEquals(6, i.getRuns());
        assertEquals(2, i.getLegalBalls(), "a wide is not a legal delivery");
        assertEquals(1, i.getWides());
        assertEquals(partner, i.getStrikerId(), "a single rotates the strike");
        BattingEntry b = batting.findByInningsIdAndPlayerId(i.getId(), opener).orElseThrow();
        assertEquals(5, b.getRuns());
        assertEquals(2, b.getBalls());
        assertEquals(1, b.getFours());

        scoring.recordBall(id, new BallRequest(0, Extra.NONE, true, Dismissal.BOWLED));
        assertEquals(1, inn(id, 1).getWickets());

        for (int k = 0; k < 3; k++) scoring.recordBall(id, new BallRequest(0, Extra.NONE, false, null));
        assertTrue(inn(id, 1).isCompleted(), "innings ends after 6 legal balls in a 1-over match");
        assertEquals(7, inn(id, 2).getTarget());
    }

    @Test
    void rejectsImpossibleWicket() {
        Long id = newMatch(2);
        assertThrows(IllegalArgumentException.class,
                () -> scoring.recordBall(id, new BallRequest(0, Extra.NO_BALL, true, Dismissal.BOWLED)));
    }

    @Test
    void simulatedMatchFinishesWithResult() {
        Long id = newMatch(3);
        MatchSummary s = null;
        for (int k = 0; k < 500; k++) {
            s = scoring.simulateBall(id);
            if ("COMPLETED".equals(s.status())) break;
        }
        assertEquals("COMPLETED", s.status());
        Innings second = inn(id, 2);
        if (second.getRuns() >= second.getTarget()) assertTrue(s.statusText().contains("wicket"));
        else assertTrue(s.statusText().contains("run") || s.statusText().contains("tied"));
    }
}
