import model.Game;
import model.Wizard;

public final class WizardTraits {

    private WizardTraits() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static double getWizardForwardSpeed(Wizard wizard, Game game) {
        return game.getWizardForwardSpeed();
    }

    public static double getWizardBackwardSpeed(Wizard wizard, Game game) {
        return game.getWizardBackwardSpeed();
    }

    public static double getWizardStrafeSpeed(Wizard wizard, Game game) {
        return game.getWizardStrafeSpeed();
    }

    public static double getWizardMaxTurnAngle(Wizard wizard, Game game) {
        return game.getWizardMaxTurnAngle();
    }

    public static double getWizardCastRange(Wizard wizard, Game game) {
        return game.getWizardCastRange();
    }
}
