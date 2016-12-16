import model.Faction;
import model.Game;

public class TeamAdvantageService {
    private static final int SURROUNDINGS_THRESHOLD = 1000;
    private final int healthAlly;
    private final int levelAlly;
    private final int healthEnemy;
    private final int levelEnemy;

    public TeamAdvantageService(WizardProxy self, WorldProxy world, Game game) {
        int healthAcademySum = 0;
        int healthRenegadesSum = 0;
        int levelAcademySum = 0;
        int levelRenegadesSum = 0;
        for (WizardProxy wizard : world.getWizards()) {
            if (self.getDistanceTo(wizard) > SURROUNDINGS_THRESHOLD) {
                continue;
            }
            if (wizard.getFaction() == Faction.ACADEMY) {
                healthAcademySum += wizard.getShieldedLife(game);
                levelAcademySum += wizard.getLevel();
            } else {
                healthRenegadesSum += wizard.getShieldedLife(game);
                levelRenegadesSum += wizard.getLevel();
            }
        }
        if (self.getFaction() == Faction.ACADEMY) {
            this.healthEnemy = healthRenegadesSum;
            this.levelEnemy = levelRenegadesSum;
            this.healthAlly = healthAcademySum;
            this.levelAlly = levelAcademySum;
        } else {
            this.healthEnemy = healthAcademySum;
            this.levelEnemy = levelAcademySum;
            this.healthAlly = healthRenegadesSum;
            this.levelAlly = levelRenegadesSum;
        }
    }

    public int getHealthAlly() {
        return healthAlly;
    }

    public int getHealthEnemy() {
        return healthEnemy;
    }

    public int getLevelAlly() {
        return levelAlly;
    }

    public int getLevelEnemy() {
        return levelEnemy;
    }
}
