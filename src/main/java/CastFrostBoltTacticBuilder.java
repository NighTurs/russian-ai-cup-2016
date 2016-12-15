import model.*;

import java.util.Optional;

public class CastFrostBoltTacticBuilder implements TacticBuilder {

    private static final double PROJECTILE_MIN_DISTANCE_WIZARD_RADIUS_RATIO = 2.0 / 3;
    private static final int SECONDARY_TARGETS_SAVE_UP_BOLTS_COUNT = 3;
    private static final int FUTURE_TARGET_RANGE_BOOST = 10;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();

        Optional<Unit> bestTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer,
                ProjectileType.FROST_BOLT,
                turnContainer.getMemory().getExpectedPushDuration() * self.getWizardForwardSpeed(game));
        Optional<Unit> bestFutureTargetOpt;
        int untilCast = CastProjectileTacticBuilders.untilNextProjectile(self,
                ProjectileType.FROST_BOLT,
                turnContainer.getGame());
        if (bestTargetOpt.isPresent() && (bestTargetOpt.get() instanceof WizardProxy ||
                self.getMana() >= game.getFrostBoltManacost() * SECONDARY_TARGETS_SAVE_UP_BOLTS_COUNT)) {
            Point aimPoint = CastProjectileTacticBuilders.targetAimPoint(turnContainer,
                    bestTargetOpt.get(),
                    ProjectileType.FROST_BOLT);
            MoveBuilder moveBuilder = new MoveBuilder();
            if (CastProjectileTacticBuilders.isInCastRange(turnContainer,
                    self,
                    bestTargetOpt.get(),
                    ProjectileType.FROST_BOLT) &&
                    CastProjectileTacticBuilders.inCastSector(turnContainer, self, aimPoint) && untilCast == 0) {
                moveBuilder.setAction(ActionType.FROST_BOLT);
                moveBuilder.setCastAngle(self.getAngleTo(aimPoint.getX(), aimPoint.getY()));
                moveBuilder.setMinCastDistance(self.getDistanceTo(aimPoint.getX(), aimPoint.getY()) -
                        ((CircularUnit) bestTargetOpt.get()).getRadius() * PROJECTILE_MIN_DISTANCE_WIZARD_RADIUS_RATIO);
                return assembleTactic(moveBuilder);
            }
            bestFutureTargetOpt = bestTargetOpt;
        } else {
            bestFutureTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer,
                    ProjectileType.FROST_BOLT,
                    FUTURE_TARGET_RANGE_BOOST);
        }
        if (!bestFutureTargetOpt.isPresent() || (!(bestFutureTargetOpt.get() instanceof WizardProxy) &&
                self.getMana() < game.getFrostBoltManacost() * SECONDARY_TARGETS_SAVE_UP_BOLTS_COUNT)) {
            return Optional.empty();
        }
        Point aimPoint = CastProjectileTacticBuilders.targetAimPoint(turnContainer,
                bestFutureTargetOpt.get(),
                ProjectileType.FROST_BOLT);
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
        return Optional.of(new TacticImpl("CastFrostBolt", moveBuilder, Tactics.CAST_FROST_BOLT_TACTIC_PRIORITY));
    }
}
