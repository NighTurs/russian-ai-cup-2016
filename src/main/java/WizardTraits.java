import model.Game;
import model.Status;
import model.StatusType;
import model.Wizard;

public final class WizardTraits {

    private WizardTraits() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static double getWizardForwardSpeed(Wizard wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardForwardSpeed();
    }

    public static double getWizardBackwardSpeed(Wizard wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardBackwardSpeed();
    }

    public static double getWizardStrafeSpeed(Wizard wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardStrafeSpeed();
    }

    public static double getWizardMaxTurnAngle(Wizard wizard, Game game) {
        return (hasHasteBonus(wizard) ? 1 + game.getHastenedRotationBonusFactor() : 1) * game.getWizardMaxTurnAngle();
    }

    public static double getWizardCastRange(Wizard wizard, Game game) {
        return game.getWizardCastRange();
    }

    public static double getWizardCastSector(Game game) {
        return game.getStaffSector() / 2;
    }

    public static double getWizardStaffSector(Game game) {
        return game.getStaffSector() / 2;
    }

    private static double movementFactor(Wizard wizard, Game game) {
        return hasHasteBonus(wizard) ? 1 + game.getHastenedMovementBonusFactor() : 1;
    }

    private static boolean hasHasteBonus(Wizard wizard) {
        for (Status status : wizard.getStatuses()) {
            if (status.getType() == StatusType.HASTENED) {
                return true;
            }
        }
        return false;
    }
}
