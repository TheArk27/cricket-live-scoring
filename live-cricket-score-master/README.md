# LiveCricket: Real-time Cricket Score Management System

A live cricket scoring platform built with **Spring Boot** (REST API + live feed) and **React** (responsive dashboard).
It runs several matches at once, updates scores ball by ball, pushes every change to the browser instantly,
and keeps scorecards, commentary, match results and career player statistics.

```
React (Vite, :5173) --REST + Server-Sent Events--> Spring Boot (:8080) --JPA--> MySQL (or H2 in dev)
 live dashboard, scorecards,                         scoring engine, live feed simulator,
 commentary, scorer console, stats                   SSE broadcaster, stats aggregation
```

## How it works

- **Live feed simulator** (`SimulationEngine`): every 2 seconds it bowls one delivery in every live match. The outcome depends on batter vs bowler skill and the phase of the innings (powerplay, death overs). It also starts scheduled matches when their time comes and schedules new fixtures, so there is always something live.
- **Scoring engine** (`ScoringService`): applies the rules of cricket to each delivery: runs, wides, no-balls, byes, leg byes, strike rotation, end of over, bowler changes with over quotas, maidens, wickets with dismissal text, partnerships, innings change with target, and the final result (won by runs or wickets, or tied).
- **Real-time updates**: after every delivery the server pushes a `match` event over `GET /api/stream` (Server-Sent Events). The React app keeps one connection open and updates the dashboard and scorecards without refreshing.
- **Manual scoring**: on any live match, the scorer console records the next ball (0 to 6, extras, wicket type). Auto-simulation can be switched off so a person scores every ball.

### Scalability choices
- Stateless REST controllers and DTOs, so the API can run behind a load balancer.
- One lock per match: different matches are scored in parallel, while the same match never gets two balls at once.
- Player statistics are aggregated in the database with `GROUP BY` queries instead of being loaded into memory.
- Indexed foreign keys (`innings_id`, `team_id`, `status`), a paged commentary endpoint, and a HikariCP pool.
- Server-Sent Events push only the small match summary, and clients fetch the full scorecard only for the match they are viewing.

## Run

Prerequisites: JDK 17+ (built with 21), Node.js 18+. Maven is bundled (`mvnw`).

**Backend**
```bash
cd backend
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev   # H2 in-memory, no MySQL needed
mvnw.cmd spring-boot:run                                   # MySQL (set MYSQL_USER / MYSQL_PASSWORD)
mvnw.cmd test                                              # scoring rule tests
```

**Frontend**
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxies /api to :8080)
```

On first start the app seeds 6 teams with 66 players, 2 completed matches, 2 live matches and 1 upcoming fixture.

## REST API

| Method & path | Purpose |
|---|---|
| `GET /api/matches?status=LIVE` | all matches (optionally by status) with live scores |
| `GET /api/matches/{id}` | full scorecard, recent balls, commentary |
| `GET /api/matches/{id}/commentary?page=0&size=30` | ball-by-ball commentary (paged) |
| `POST /api/matches` | create a match `{teamAId, teamBId, overs, venue, startNow, autoPlay}` |
| `POST /api/matches/{id}/start` | toss and start a scheduled match |
| `POST /api/matches/{id}/balls` | record a delivery `{runs, extra, wicket, dismissal}` |
| `PUT /api/matches/{id}/autoplay?enabled=false` | switch the simulator on/off for a match |
| `GET /api/teams`, `GET /api/teams/{id}/players` | teams and squads |
| `GET /api/players/stats?sort=runs` | leaderboard (`runs`, `wickets`, `strikeRate`, `economy`, `sixes`) |
| `GET /api/stream` | Server-Sent Events live feed |
| `GET /api/health` | status, live match count, connected clients |

`extra` is one of `NONE, WIDE, NO_BALL, BYE, LEG_BYE`; `dismissal` is one of `BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED`.

## Project layout

```
backend/src/main/java/com/cricket/live/
  model/        Team, Player, CricketMatch, Innings, BattingEntry, BowlingEntry, BallEvent
  repository/   Spring Data JPA repositories (+ aggregate queries)
  dto/          API request/response records
  service/      ScoringService, SimulationEngine, BallSimulator, MatchService, StatsService,
                LiveUpdateService (SSE), MatchMapper
  controller/   MatchController, CricketController
  config/       WebConfig (CORS), DataSeeder
frontend/src/
  App.jsx       routing + live feed connection
  components/   Dashboard, MatchDetail (scorecard, commentary, scorer console), PlayerStats, NewMatch
```
