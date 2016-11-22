import model.*;

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
            return assembleTactic(moveBuilder);
        }
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

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= turnContainer.getGame().getStaffRange() + minion.getRadius()) {
                return Optional.of(minion);
            }
        }

        Unit bestUnit = null;
        int lowestLife = Integer.MAX_VALUE;
        for (Wizard wizard : world.getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            double dist = wizard.getDistanceTo(self);
            if (dist <= castRangeToWizard(self, wizard, turnContainer.getGame()) && lowestLife > wizard.getLife()) {
                lowestLife = wizard.getLife();
                bestUnit = wizard;
            }
        }
        if (bestUnit != null) {
            return Optional.of(bestUnit);
        }

        for (Building building : world.getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(building)) {
                continue;
            }
            double dist = building.getDistanceTo(self);
            if (dist <= WizardTraits.getWizardCastRange(self, turnContainer.getGame()) + building.getRadius() +
                    turnContainer.getGame().getMagicMissileRadius()) {
                return Optional.of(building);
            }
        }

        for (Minion minion : world.getMinions()) {
            if (!turnContainer.isOffensiveMinion(minion)) {
                continue;
            }
            double dist = minion.getDistanceTo(self);
            if (dist <= WizardTraits.getWizardCastRange(self, turnContainer.getGame()) &&
                    lowestLife > minion.getLife()) {
                lowestLife = minion.getLife();
                bestUnit = minion;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    private boolean inCastSector(TurnContainer turnContainer, Unit unit) {
        Wizard self = turnContainer.getSelf();
        double angle = self.getAngleTo(unit);
        return WizardTraits.getWizardCastSector(turnContainer.getGame()) > Math.abs(angle);
    }

    public static double castRangeToWizard(Wizard self, Wizard wizard, Game game) {
        double undodgebaleDistance = game.getWizardRadius() + game.getMagicMissileRadius() -
                ((int) Math.ceil(self.getCastRange() / game.getMagicMissileSpeed()) - 1) *
                        WizardTraits.getWizardBackwardSpeed(wizard, game);
        return WizardTraits.getWizardCastRange(self, game) + undodgebaleDistance;
    }
}
