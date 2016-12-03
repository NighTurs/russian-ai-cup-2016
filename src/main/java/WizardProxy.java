import model.*;

import java.util.EnumSet;
import java.util.Set;

public class WizardProxy extends LivingUnit {

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
    private final Wizard wizard;

    public WizardProxy(Wizard wizard, World world, Game game) {
        super(wizard.getId(),
                wizard.getX(),
                wizard.getY(),
                wizard.getSpeedX(),
                wizard.getSpeedY(),
                wizard.getAngle(),
                wizard.getFaction(),
                wizard.getRadius(),
                wizard.getLife(),
                wizard.getMaxLife(),
                wizard.getStatuses());
        this.wizard = wizard;
    }

    public long getOwnerPlayerId() {
        return wizard.getOwnerPlayerId();
    }

    public boolean isMe() {
        return wizard.isMe();
    }

    public int getMana() {
        return wizard.getMana();
    }

    public int getMaxMana() {
        return wizard.getMaxMana();
    }

    public double getVisionRange() {
        return wizard.getVisionRange();
    }

    public double getCastRange() {
        return wizard.getCastRange();
    }

    public int getXp() {
        return wizard.getXp();
    }

    public int getLevel() {
        return wizard.getLevel();
    }

    public SkillType[] getSkills() {
        return wizard.getSkills();
    }

    public int getRemainingActionCooldownTicks() {
        return wizard.getRemainingActionCooldownTicks();
    }

    public int[] getRemainingCooldownTicksByAction() {
        return wizard.getRemainingCooldownTicksByAction();
    }

    public boolean isMaster() {
        return wizard.isMaster();
    }

    public Message[] getMessages() {
        return wizard.getMessages();
    }

    public double getWizardForwardSpeed(Game game) {
        return getWizardForwardSpeed(this, game);
    }

    public double getWizardBackwardSpeed(Game game) {
        return getWizardBackwardSpeed(this, game);
    }

    public double getWizardStrafeSpeed(Game game) {
        return getWizardStrafeSpeed(this, game);
    }

    public double getWizardMaxTurnAngle(Game game) {
        return getWizardMaxTurnAngle(this, game);
    }

    public double getWizardCastRange(Game game) {
        return getWizardCastRange(this, game);
    }

    public static double getWizardCastSector(Game game) {
        return game.getStaffSector() / 2;
    }

    public static double getWizardStaffSector(Game game) {
        return game.getStaffSector() / 2;
    }

    public double getMagicMissileDirectDamage(Game game) {
        return getMagicMissileDirectDamage(this, game);
    }

    public double getWizardManaPerTurn(Game game) {
        return getWizardManaPerTurn(this, game);
    }

    static double getWizardForwardSpeed(WizardProxy wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardForwardSpeed();
    }

    static double getWizardBackwardSpeed(WizardProxy wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardBackwardSpeed();
    }

    static double getWizardStrafeSpeed(WizardProxy wizard, Game game) {
        return movementFactor(wizard, game) * game.getWizardStrafeSpeed();
    }

    static double getWizardMaxTurnAngle(WizardProxy wizard, Game game) {
        return (hasHasteBonus(wizard) ? 1 + game.getHastenedRotationBonusFactor() : 1) * game.getWizardMaxTurnAngle();
    }

    static double getWizardCastRange(WizardProxy wizard, Game game) {
        return countRangeSkills(wizard) * game.getRangeBonusPerSkillLevel() + game.getWizardCastRange();
    }

    static double getMagicMissileDirectDamage(WizardProxy wizard, Game game) {
        return (hasEmpowerBonus(wizard) ? 1 + game.getEmpoweredDamageFactor() : 1) *
                (game.getMagicMissileDirectDamage() +
                        countMagicalDamageSkills(wizard) * game.getMagicalDamageBonusPerSkillLevel());
    }

    static double getWizardManaPerTurn(WizardProxy wizard, Game game) {
        return game.getWizardBaseManaRegeneration() +
                game.getWizardManaRegenerationGrowthPerLevel() * wizard.getLevel();
    }

    private static double movementFactor(WizardProxy wizard, Game game) {
        return 1 + countMoveSpeedSkills(wizard) * game.getMovementBonusFactorPerSkillLevel() +
                (hasHasteBonus(wizard) ? game.getHastenedMovementBonusFactor() : 0);
    }

    private static boolean hasEmpowerBonus(WizardProxy wizard) {
        return hasBonus(wizard, StatusType.EMPOWERED);
    }

    private static boolean hasHasteBonus(WizardProxy wizard) {
        return hasBonus(wizard, StatusType.HASTENED);
    }

    private static boolean hasBonus(WizardProxy wizard, StatusType bonusStatusType) {
        for (Status status : wizard.getStatuses()) {
            if (status.getType() == bonusStatusType) {
                return true;
            }
        }
        return false;
    }

    private static int countRangeSkills(WizardProxy wizard) {
        return countSkills(wizard, RANGE_SKILLS);
    }

    private static int countMagicalDamageSkills(WizardProxy wizard) {
        return countSkills(wizard, MAGICAL_DAMAGE_SKILLS);
    }

    private static int countMoveSpeedSkills(WizardProxy wizard) {
        return countSkills(wizard, MOVE_SPEED_SKILLS);
    }

    private static int countSkills(WizardProxy wizard, Set<SkillType> skills) {
        int count = 0;
        for (SkillType skill : wizard.getSkills()) {
            if (skills.contains(skill)) {
                count++;
            }
        }
        return count;
    }
}