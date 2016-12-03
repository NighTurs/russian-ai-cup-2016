import model.*;

import java.util.Optional;

public class CastMagicMissileTacticBuilder implements TacticBuilder {

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();

        if (CastProjectileTacticBuilders.shouldSaveUpMana(turnContainer, ActionType.MAGIC_MISSILE)) {
            return Optional.empty();
        }
        Optional<Unit> bestTargetOpt = findBestTarget(turnContainer);
        if (!bestTargetOpt.isPresent()) {
            return Optional.empty();
        }
        Unit bestTarget = bestTargetOpt.get();
        MoveBuilder moveBuilder = new MoveBuilder();
        if (CastProjectileTacticBuilders.inCastSector(turnContainer, new Point(bestTarget.getX(), bestTarget.getY()))) {
            castWithMove(moveBuilder, bestTarget, turnContainer);
            return assembleTactic(moveBuilder);
        } else {
            moveBuilder.setTurn(self.getAngleTo(bestTarget));
            return assembleTactic(moveBuilder);
        }
    }

    private Optional<Tactic> assembleTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("CastMagicMissile", moveBuilder, Tactics.CAST_MAGIC_MISSILE_TACTIC_PRIORITY));
    }

    private void castWithMove(MoveBuilder moveBuilder, Unit unit, TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 &&
                self.getRemainingActionCooldownTicks() == 0) {
            moveBuilder.setAction(ActionType.MAGIC_MISSILE);
            moveBuilder.setCastAngle(self.getAngleTo(unit));
            moveBuilder.setMinCastDistance(self.getDistanceTo(unit) - ((CircularUnit) unit).getRadius());
        }
    }

    private Optional<Unit> findBestTarget(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        WizardProxy self = turnContainer.getSelf();

        Unit bestUnit = null;
        int lowestLife = Integer.MAX_VALUE;
        for (WizardProxy wizard : world.getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            double dist = wizard.getDistanceTo(self);
            if (dist <=
                    CastProjectileTacticBuilders.castRangeToWizardPessimistic(self, wizard, turnContainer.getGame()) &&
                    lowestLife > wizard.getLife()) {
                lowestLife = wizard.getLife();
                bestUnit = wizard;
            }
        }
        if (bestUnit != null) {
            return Optional.of(bestUnit);
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= turnContainer.getGame().getStaffRange() + minion.getRadius()) {
                return Optional.of(minion);
            }
        }

        for (Building building : world.getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(building)) {
                continue;
            }
            double dist = building.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToBuilding(self, building, turnContainer.getGame())) {
                return Optional.of(building);
            }
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= CastProjectileTacticBuilders.castRangeToMinion(self, minion, turnContainer.getGame()) &&
                    lowestLife > minion.getLife()) {
                lowestLife = minion.getLife();
                bestUnit = minion;
            }
        }
        return Optional.ofNullable(bestUnit);
    }
}
