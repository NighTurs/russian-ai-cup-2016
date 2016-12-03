import model.*;

import java.util.EnumSet;
import java.util.Set;

public final class WizardTraits {

    private static final Set<SkillType> RANGE_SKILLS = EnumSet.of(SkillType.RANGE_BONUS_PASSIVE_1,
            SkillType.RANGE_BONUS_AURA_1,
            SkillType.RANGE_BONUS_PASSIVE_2,
            SkillType.RANGE_BONUS_AURA_2);
    private static final Set<SkillType> MAGICAL_DAMAGE_SKILLS = EnumSet.of(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
            SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
    private static final Set<SkillType> MOVE_SPEED_SKILLS = EnumSet.of(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_2);

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
        return countRangeSkills(wizard) * game.getRangeBonusPerSkillLevel() + game.getWizardCastRange();
    }

    public static double getWizardCastSector(Game game) {
        return game.getStaffSector() / 2;
    }

    public static double getWizardStaffSector(Game game) {
        return game.getStaffSector() / 2;
    }

    public static double getMagicMissileDirectDamage(Wizard wizard, Game game) {
        return (hasEmpowerBonus(wizard) ? 1 + game.getEmpoweredDamageFactor() : 1) *
                (game.getMagicMissileDirectDamage() +
                        countMagicalDamageSkills(wizard) * game.getMagicalDamageBonusPerSkillLevel());
    }

    public static double getWizardManaPerTurn(Wizard wizard, Game game) {
        return game.getWizardBaseManaRegeneration() +
                game.getWizardManaRegenerationGrowthPerLevel() * wizard.getLevel();
    }

    private static double movementFactor(Wizard wizard, Game game) {
        return 1 + countMoveSpeedSkills(wizard) * game.getMovementBonusFactorPerSkillLevel() +
                (hasHasteBonus(wizard) ? game.getHastenedMovementBonusFactor() : 0);
    }

    private static boolean hasEmpowerBonus(Wizard wizard) {
        return hasBonus(wizard, StatusType.EMPOWERED);
    }

    private static boolean hasHasteBonus(Wizard wizard) {
        return hasBonus(wizard, StatusType.HASTENED);
    }

    private static boolean hasBonus(Wizard wizard, StatusType bonusStatusType) {
        for (Status status : wizard.getStatuses()) {
            if (status.getType() == bonusStatusType) {
                return true;
            }
        }
        return false;
    }

    private static int countRangeSkills(Wizard wizard) {
        return countSkills(wizard, RANGE_SKILLS);
    }

    private static int countMagicalDamageSkills(Wizard wizard) {
        return countSkills(wizard, MAGICAL_DAMAGE_SKILLS);
    }

    private static int countMoveSpeedSkills(Wizard wizard) {
        return countSkills(wizard, MOVE_SPEED_SKILLS);
    }

    private static int countSkills(Wizard wizard, Set<SkillType> skills) {
        int count = 0;
        for (SkillType skill : wizard.getSkills()) {
            if (skills.contains(skill)) {
                count++;
            }
        }
        return count;
    }
}
