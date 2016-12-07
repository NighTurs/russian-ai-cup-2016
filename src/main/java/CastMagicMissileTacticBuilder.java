import model.*;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    private static final int FUTURE_TARGET_RANGE_BOOST = 100;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();

        if (CastProjectileTacticBuilders.shouldSaveUpMana(turnContainer, ActionType.MAGIC_MISSILE)) {
            return Optional.empty();
        }
        Optional<Unit> bestTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer, 0);
        Optional<Unit> bestFutureTargetOpt;
        int untilCast = CastProjectileTacticBuilders.untilNextProjectile(self,
                ProjectileType.MAGIC_MISSILE,
                turnContainer.getGame());
        if (bestTargetOpt.isPresent()) {
            Point aimPoint =
                    targetAimPoint(self, bestTargetOpt.get(), turnContainer.getGame(), turnContainer.getWorldProxy());
            MoveBuilder moveBuilder = new MoveBuilder();
            if (CastProjectileTacticBuilders.inCastSector(turnContainer, aimPoint) && untilCast == 0) {
                moveBuilder.setAction(ActionType.MAGIC_MISSILE);
                moveBuilder.setCastAngle(self.getAngleTo(aimPoint.getX(), aimPoint.getY()));
                moveBuilder.setMinCastDistance(self.getDistanceTo(aimPoint.getX(), aimPoint.getY()) -
                        ((CircularUnit) bestTargetOpt.get()).getRadius());
                return assembleTactic(moveBuilder);
            }
            bestFutureTargetOpt = bestTargetOpt;
        } else {
            bestFutureTargetOpt =
                    CastProjectileTacticBuilders.bestFocusTarget(turnContainer, FUTURE_TARGET_RANGE_BOOST);
        }
        if (!bestFutureTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Unit futureTarget = bestFutureTargetOpt.get();
        MoveBuilder moveBuilder = new MoveBuilder();
        Optional<Double> turn = CastProjectileTacticBuilders.justInTimeTurn(self,
                new Point(futureTarget.getX(), futureTarget.getY()),
                untilCast,
                turnContainer.getGame());
        if (turn.isPresent()) {
            moveBuilder.setTurn(turn.get());
            return assembleTactic(moveBuilder);
        }
        return Optional.empty();
    }

    private Point targetAimPoint(WizardProxy self, Unit target, Game game, WorldProxy world) {
        return new Point(target.getX(), target.getY());
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Point point, double radius, TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (CastProjectileTacticBuilders.untilNextProjectile(self,
                ProjectileType.MAGIC_MISSILE,
                turnContainer.getGame()) == 0) {
            moveBuilder.setAction(ActionType.MAGIC_MISSILE);
            moveBuilder.setCastAngle(self.getAngleTo(point.getX(), point.getY()));
            moveBuilder.setMinCastDistance(self.getDistanceTo(point.getX(), point.getY()) - radius);
        }
    }
}
