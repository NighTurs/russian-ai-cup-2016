import model.ActionType;
import model.CircularUnit;
import model.Unit;
import model.Wizard;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();

        Optional<Unit> bestTargetOpt = findBestTarget(turnContainer);
        if (!bestTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Unit bestTarget = bestTargetOpt.get();
        MoveBuilder moveBuilder = new MoveBuilder();
        if (inCastSector(turnContainer, bestTarget)) {
            castWithMove(moveBuilder, bestTarget, turnContainer);
            return assembleTactic(moveBuilder);
        } else {
            moveBuilder.setTurn(self.getAngleTo(bestTarget));
        }

        Optional<Unit> immediateTargetOpt = findImmediateTraget(turnContainer);
        if (!immediateTargetOpt.isPresent()) {
            return assembleTactic(moveBuilder);
        }
        Unit immediateTarget = immediateTargetOpt.get();
        castWithMove(moveBuilder, immediateTarget, turnContainer);

        return assembleTactic(moveBuilder);
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Unit unit, TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0) {
            moveBuilder.setAction(ActionType.MAGIC_MISSILE);
            moveBuilder.setCastAngle(self.getAngleTo(unit));
            moveBuilder.setMinCastDistance(self.getDistanceTo(unit) - ((CircularUnit) unit).getRadius());
        }
    }

    private Optional<Unit> findBestTarget(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Wizard self = turnContainer.getSelf();

        double bestDist = Double.MAX_VALUE;
        Unit bestUnit = null;
        for (Unit unit : world.allUnits()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            if (dist <= WizardTraits.getWizardCastRange(self, turnContainer.getGame()) && bestDist > dist) {
                bestDist = dist;
                bestUnit = unit;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    private Optional<Unit> findImmediateTraget(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Wizard self = turnContainer.getSelf();

        for (Unit unit : world.allUnits()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            if (dist <= WizardTraits.getWizardCastRange(self, turnContainer.getGame()) &&
                    inCastSector(turnContainer, unit)) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }

    private boolean inCastSector(TurnContainer turnContainer, Unit unit) {
        Wizard self = turnContainer.getSelf();
        double angle = self.getAngleTo(unit);
        return WizardTraits.getWizardCastSector(turnContainer.getGame()) > Math.abs(angle);
    }
}
