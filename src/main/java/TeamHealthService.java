import model.Faction;
import model.Game;
import model.StatusType;

public class TeamHealthService {
    private static final int SURROUNDINGS_THRESHOLD = 1000;
    private final int healthAlly;
    private final int healthEnemy;

    public TeamHealthService(WizardProxy self, WorldProxy world, Game game) {
        int healthAcademySum = 0;
        int healthRenegadesSum = 0;
        for (WizardProxy wizard : world.getWizards()) {
            if (self.getDistanceTo(wizard) > SURROUNDINGS_THRESHOLD) {
                continue;
            }
            double shieldFactor =
                    wizard.hasBonus(StatusType.SHIELDED) ? 1 + game.getShieldedDirectDamageAbsorptionFactor() : 1;
            if (wizard.getFaction() == Faction.ACADEMY) {
                healthAcademySum += wizard.getLife() * shieldFactor;
            } else {
                healthRenegadesSum += wizard.getLife() * shieldFactor;
            }
        }
        if (self.getFaction() == Faction.ACADEMY) {
            this.healthEnemy = healthRenegadesSum;
            this.healthAlly = healthAcademySum;
        } else {
            this.healthEnemy = healthAcademySum;
            this.healthAlly = healthRenegadesSum;
        }
    }

    public int getHealthAlly() {
        return healthAlly;
    }

    public int getHealthEnemy() {
        return healthEnemy;
    }
}
