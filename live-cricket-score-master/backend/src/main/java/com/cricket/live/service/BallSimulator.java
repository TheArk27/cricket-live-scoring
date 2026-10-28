package com.cricket.live.service;

import com.cricket.live.dto.Dtos.BallRequest;
import com.cricket.live.model.BallEvent.Dismissal;
import com.cricket.live.model.BallEvent.Extra;
import com.cricket.live.model.Player;

import java.util.Random;

/** Generates a realistic delivery outcome from batter vs bowler skill and the match phase. */
final class BallSimulator {
    private BallSimulator() {
    }

    static BallRequest next(Player bat, Player bowl, int legalBalls, int overs, Random r) {
        double f = (bat.getBattingSkill() - bowl.getBowlingSkill()) / 100.0;
        int over = legalBalls / 6;
        boolean death = overs >= 5 && over >= overs - Math.max(1, overs / 5);
        boolean powerplay = over < Math.max(1, overs * 3 / 10);

        double x = r.nextDouble();
        if ((x -= 0.035) < 0) return new BallRequest(r.nextInt(10) == 0 ? 1 : 0, Extra.WIDE, false, null);
        if ((x -= 0.008) < 0) return new BallRequest(r.nextInt(3), Extra.NO_BALL, false, null);
        if ((x -= 0.012) < 0) return new BallRequest(1, r.nextBoolean() ? Extra.LEG_BYE : Extra.BYE, false, null);
        if ((x -= 0.045 * (1 - f) * (death ? 1.5 : 1)) < 0) return new BallRequest(0, Extra.NONE, true, dismissal(r));
        if ((x -= 0.045 * (1 + f) * (death ? 1.8 : powerplay ? 1.2 : 1)) < 0) return runs(6);
        if ((x -= 0.10 * (1 + f) * (death ? 1.3 : powerplay ? 1.4 : 1)) < 0) return runs(4);
        if ((x -= 0.012) < 0) return runs(3);
        if ((x -= 0.08) < 0) return runs(2);
        if ((x -= 0.36) < 0) return runs(1);
        return runs(0);
    }

    private static BallRequest runs(int n) {
        return new BallRequest(n, Extra.NONE, false, null);
    }

    private static Dismissal dismissal(Random r) {
        int d = r.nextInt(100);
        if (d < 55) return Dismissal.CAUGHT;
        if (d < 75) return Dismissal.BOWLED;
        if (d < 90) return Dismissal.LBW;
        if (d < 96) return Dismissal.RUN_OUT;
        return Dismissal.STUMPED;
    }
}
