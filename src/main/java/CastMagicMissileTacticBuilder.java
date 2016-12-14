import model.ActionType;
import model.CircularUnit;
import model.ProjectileType;
import model.Unit;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    private static final double PROJECTILE_MIN_DISTANCE_WIZARD_RADIUS_RATIO = 2.0 / 3;
    private static final int FUTURE_TARGET_RANGE_BOOST = 100;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();

        if (CastProjectileTacticBuilders.shouldSaveUpMana(turnContainer, ActionType.MAGIC_MISSILE)) {
            return Optional.empty();
        }
        Optional<Unit> bestTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer,
                ProjectileType.MAGIC_MISSILE,
                turnContainer.getMemory().getExpectedPushDuration() *
                        self.getWizardForwardSpeed(turnContainer.getGame()));
        Optional<Unit> bestFutureTargetOpt;
        int untilCast = CastProjectileTacticBuilders.untilNextProjectile(self,
                ProjectileType.MAGIC_MISSILE,
                turnContainer.getGame());
        if (bestTargetOpt.isPresent()) {
            Point aimPoint = CastProjectileTacticBuilders.targetAimPoint(turnContainer,
                    bestTargetOpt.get(),
                    ProjectileType.MAGIC_MISSILE);
            MoveBuilder moveBuilder = new MoveBuilder();
            if (CastProjectileTacticBuilders.isInCastRange(turnContainer,
                    self,
                    bestTargetOpt.get(),
                    ProjectileType.MAGIC_MISSILE) &&
                    CastProjectileTacticBuilders.inCastSector(turnContainer, aimPoint) && untilCast == 0) {
                moveBuilder.setAction(ActionType.MAGIC_MISSILE);
                moveBuilder.setCastAngle(self.getAngleTo(aimPoint.getX(), aimPoint.getY()));
                moveBuilder.setMinCastDistance(self.getDistanceTo(aimPoint.getX(), aimPoint.getY()) -
                        ((CircularUnit) bestTargetOpt.get()).getRadius() * PROJECTILE_MIN_DISTANCE_WIZARD_RADIUS_RATIO);
                return assembleTactic(moveBuilder);
            }
            bestFutureTargetOpt = bestTargetOpt;
        } else {
            bestFutureTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer,
                    ProjectileType.MAGIC_MISSILE,
                    FUTURE_TARGET_RANGE_BOOST);
        }
        if (!bestFutureTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Point aimPoint = CastProjectileTacticBuilders.targetAimPoint(turnContainer,
                bestFutureTargetOpt.get(),
                ProjectileType.MAGIC_MISSILE);
        MoveBuilder moveBuilder = new MoveBuilder();
        Optional<Double> turn = CastProjectileTacticBuilders.justInTimeTurn(self,
                new Point(aimPoint.getX(), aimPoint.getY()),
                untilCast,
                turnContainer.getGame());
        if (turn.isPresent()) {
            moveBuilder.setTurn(turn.get());
            return assembleTactic(moveBuilder);
        }
        return Optional.empty();
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }
}
