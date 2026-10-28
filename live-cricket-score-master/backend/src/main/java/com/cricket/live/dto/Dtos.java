package com.cricket.live.dto;

import com.cricket.live.model.BallEvent.Dismissal;
import com.cricket.live.model.BallEvent.Extra;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public record TeamDto(Long id, String name, String shortName, String color) {
    }

    public record PlayerDto(Long id, String name, String role, int battingSkill, int bowlingSkill) {
    }

    public record InningsScore(int number, Long battingTeamId, String battingTeam, int runs, int wickets,
                               String overs, Integer target, boolean completed) {
    }

    public record MatchSummary(Long id, TeamDto teamA, TeamDto teamB, String venue, int overs, String status,
                               boolean autoPlay, int currentInnings, List<InningsScore> innings, String statusText,
                               String toss, Double crr, Double rrr, LocalDateTime scheduledAt,
                               LocalDateTime updatedAt) {
    }

    public record BattingRow(Long playerId, String name, int runs, int balls, int fours, int sixes,
                             double strikeRate, boolean out, String dismissal, boolean onStrike, boolean atCrease) {
    }

    public record BowlingRow(Long playerId, String name, String overs, int maidens, int runs, int wickets,
                             double economy, boolean bowling) {
    }

    public record InningsDetail(InningsScore score, int extras, String extrasText, List<BattingRow> batting,
                                List<BowlingRow> bowling, List<String> yetToBat, String partnership) {
    }

    public record BallDto(Long id, int innings, String over, String symbol, int runs, boolean wicket,
                          String commentary, LocalDateTime time) {
    }

    public record MatchDetail(MatchSummary summary, List<InningsDetail> innings, List<String> recentBalls,
                              List<BallDto> commentary) {
    }

    public record PlayerStats(Long playerId, String name, String team, String teamColor, String role, long matches,
                              long runs, long balls, long fours, long sixes, long highest, double average,
                              double strikeRate, long wickets, String overs, long runsConceded, double economy) {
    }

    public record BallRequest(@Min(0) @Max(6) int runs, Extra extra, boolean wicket, Dismissal dismissal) {
    }

    public record CreateMatchRequest(@NotNull Long teamAId, @NotNull Long teamBId, @Min(1) @Max(50) int overs,
                                     String venue, boolean startNow, boolean autoPlay) {
    }
}
