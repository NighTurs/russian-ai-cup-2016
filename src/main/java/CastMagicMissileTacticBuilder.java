import model.ActionType;
import model.CircularUnit;
import model.Game;
import model.Unit;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();

        if (CastProjectileTacticBuilders.shouldSaveUpMana(turnContainer, ActionType.MAGIC_MISSILE)) {
            return Optional.empty();
        }
        Optional<Unit> bestTargetOpt = CastProjectileTacticBuilders.bestFocusTarget(turnContainer);
        if (!bestTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Point aimPoint =
                targetAimPoint(self, bestTargetOpt.get(), turnContainer.getGame(), turnContainer.getWorldProxy());
        MoveBuilder moveBuilder = new MoveBuilder();
        if (CastProjectileTacticBuilders.inCastSector(turnContainer, aimPoint)) {
            castWithMove(moveBuilder, aimPoint, ((CircularUnit) bestTargetOpt.get()).getRadius(), turnContainer);
            return assembleTactic(moveBuilder);
        } else {
            moveBuilder.setTurn(self.getAngleTo(aimPoint.getX(), aimPoint.getY()));
            return assembleTactic(moveBuilder);
        }
    }

    private Point targetAimPoint(WizardProxy self, Unit target, Game game, WorldProxy world) {
        if (!(target instanceof WizardProxy)) {
            return new Point(target.getX(), target.getY());
        }
        WizardProxy enemy = (WizardProxy) target;
        double effectiveCastRange = CastProjectileTacticBuilders.effectiveCastRangeToWizard(self, enemy, game, false);
        double forwardUndodgeableRadius = Math.abs(CastProjectileTacticBuilders.undodgeableRadiusPessimistic(
                effectiveCastRange,
                enemy.getWizardForwardSpeed(game),
                game));
        double aimPointX = enemy.getX() + forwardUndodgeableRadius * Math.cos(enemy.getAngle());
        double aimPointY = enemy.getY() + forwardUndodgeableRadius * Math.sin(enemy.getAngle());
        return new Point(aimPointX, aimPointY);
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Point point, double radius, TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0) {
            moveBuilder.setAction(ActionType.MAGIC_MISSILE);
            moveBuilder.setCastAngle(self.getAngleTo(point.getX(), point.getY()));
            moveBuilder.setMinCastDistance(self.getDistanceTo(point.getX(), point.getY()) - radius);
        }
    }
}
