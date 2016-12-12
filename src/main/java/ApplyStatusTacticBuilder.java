import model.ActionType;
import model.Game;
import model.StatusType;

import java.util.Optional;
import java.util.function.Function;

public abstract class ApplyStatusTacticBuilder implements TacticBuilder {

    protected static Optional<WizardProxy> bestTarget(TurnContainer turnContainer,
                                                      Function<WizardProxy, Double> score,
                                                      StatusType statusType) {
        WizardProxy self = turnContainer.getSelf();
        double bestScore = Double.MIN_VALUE;
        WizardProxy bestWizard = null;
        for (WizardProxy wizard : turnContainer.getWorldProxy().getWizards()) {
            if (wizard.getFaction() != self.getFaction() || wizard.getId() == self.getId() ||
                    wizard.hasBonus(statusType)) {
                continue;
            }
            if (wizard.getDistanceTo(self) <= self.getCastRange() && bestScore < score.apply(wizard)) {
                bestScore = score.apply(wizard);
                bestWizard = wizard;
            }
        }
        if (bestWizard == null && !self.hasBonus(statusType)) {
            return Optional.of(self);
        } else {
            return Optional.ofNullable(bestWizard);
        }
    }

    protected static int untilNextStatus(WizardProxy wizard, StatusType statusType, Game game) {
        ActionType actionType = statusActionType(statusType);
        int manaCost = statusManaCost(statusType, game);
        int untilCooldown = Math.max(wizard.getRemainingCooldownTicksByAction()[actionType.ordinal()],
                wizard.getRemainingActionCooldownTicks());
        return untilCooldown + (int) Math.ceil(
                Math.max(0.0, manaCost - (wizard.getMana() + untilCooldown * wizard.getWizardManaPerTurn(game))) /
                        wizard.getWizardManaPerTurn(game));
    }

    private static ActionType statusActionType(StatusType statusType) {
        switch (statusType) {
            case HASTENED:
                return ActionType.HASTE;
            case SHIELDED:
                return ActionType.SHIELD;
            default:
                throw new RuntimeException("Unexpected status type " + statusType);
        }
    }

    private static int statusManaCost(StatusType statusType, Game game) {
        switch (statusType) {
            case HASTENED:
                return game.getHasteManacost();
            case SHIELDED:
                return game.getShieldManacost();
            default:
                throw new RuntimeException("Unexpected status type " + statusType);
        }
    }
}
