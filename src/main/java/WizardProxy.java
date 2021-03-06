import model.*;

import java.util.*;

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
    private static final Set<SkillType> AURAS = EnumSet.of(SkillType.STAFF_DAMAGE_BONUS_AURA_1,
            SkillType.STAFF_DAMAGE_BONUS_AURA_2,
            SkillType.RANGE_BONUS_AURA_1,
            SkillType.RANGE_BONUS_AURA_2,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
            SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
            SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
    private final Wizard wizard;
    private final int tickIndex;
    private final List<SkillType> skills;
    private final List<SkillType> affectedBySkills;
    private final boolean isShadow;
    private final int shadowRemainingActionCooldownTicks;
    private final int[] shadowRemainingCooldownTicksByAction;
    private final double shadowMana;
    private final int shadowImageTick;
    private final Message[] messages;

    public static WizardProxy wizardProxy(Wizard wizard, World world, Game game, Memory memory) {
        return new WizardProxy(wizard, false, 0, 0, 0, 0, null, 0, world, game, memory);
    }

    public static WizardProxy shadowWizard(Wizard wizard,
                                           int imageTick,
                                           double x,
                                           double y,
                                           int remainingActionCooldownTicks,
                                           int[] remainingCooldownTicksByAction,
                                           double mana,
                                           World world,
                                           Game game) {
        return new WizardProxy(wizard,
                true,
                imageTick,
                x,
                y,
                remainingActionCooldownTicks,
                remainingCooldownTicksByAction,
                mana,
                world,
                game,
                null);
    }

    private WizardProxy(Wizard wizard,
                        boolean isShadow,
                        int imageTick,
                        double x,
                        double y,
                        int remainingActionCooldownTicks,
                        int[] remainingCooldownTicksByAction,
                        double mana,
                        World world,
                        Game game,
                        Memory memory) {
        super(wizard.getId(),
                isShadow ? x : wizard.getX(),
                isShadow ? y : wizard.getY(),
                wizard.getSpeedX(),
                wizard.getSpeedY(),
                wizard.getAngle(),
                wizard.getFaction(),
                wizard.getRadius(),
                wizard.getLife(),
                wizard.getMaxLife(),
                wizard.getStatuses());
        this.wizard = wizard;
        this.tickIndex = world.getTickIndex();
        this.isShadow = isShadow;
        this.shadowRemainingActionCooldownTicks = remainingActionCooldownTicks;
        this.shadowRemainingCooldownTicksByAction = remainingCooldownTicksByAction;
        this.shadowMana = mana;
        this.shadowImageTick = imageTick;
        this.messages =
                game.isRawMessagesEnabled() && wizard.isMaster() && wizard.isMe() && memory.getSelfMessage() != null ?
                        new Message[]{memory.getSelfMessage()} :
                        wizard.getMessages();

        this.skills = Arrays.asList(wizard.getSkills());
        Set<SkillType> affectedBySet = EnumSet.noneOf(SkillType.class);
        affectedBySet.addAll(skills);
        for (Wizard ally : world.getWizards()) {
            if (ally.getFaction() != wizard.getFaction() || ally.getId() == wizard.getId()) {
                continue;
            }
            if (ally.getDistanceTo(wizard) <= game.getAuraSkillRange()) {
                for (SkillType skill : ally.getSkills()) {
                    if (AURAS.contains(skill)) {
                        affectedBySet.add(skill);
                    }
                }
            }
        }
        this.affectedBySkills = new ArrayList<>(affectedBySet);
    }

    public long getOwnerPlayerId() {
        return wizard.getOwnerPlayerId();
    }

    public boolean isMe() {
        return wizard.isMe();
    }

    public double getMana() {
        if (isShadow) {
            return shadowMana;
        } else {
            return wizard.getMana();
        }
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

    public List<SkillType> getSkills() {
        return skills;
    }

    public int getRemainingActionCooldownTicks() {
        if (isShadow) {
            return shadowRemainingActionCooldownTicks;
        } else {
            return wizard.getRemainingActionCooldownTicks();
        }
    }

    public int[] getRemainingCooldownTicksByAction() {
        if (isShadow) {
            return Arrays.copyOf(shadowRemainingCooldownTicksByAction, shadowRemainingCooldownTicksByAction.length);
        } else {
            return wizard.getRemainingCooldownTicksByAction();
        }
    }

    public boolean isMaster() {
        return wizard.isMaster();
    }

    public Message[] getMessages() {
        return Arrays.copyOf(messages, messages.length);
    }

    public boolean isShadow() {
        return isShadow;
    }

    public int getShadowImageTick() {
        return shadowImageTick;
    }

    public Wizard getWizard() {
        return wizard;
    }

    public boolean isRealOrFreshShadow() {
        return !isShadow || (tickIndex - shadowImageTick) <= 1;
    }

    public List<SkillType> affectedBySkills() {
        return affectedBySkills;
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

    public int countRangeSkills() {
        return countSkills(this, RANGE_SKILLS);
    }

    public int countMoveSpeedSkills() {
        return countMoveSpeedSkills(this);
    }

    public boolean hasBonus(StatusType bonusStatusType) {
        return hasBonus(this, bonusStatusType);
    }

    public Point faceOffsetPoint(double faceOffsetDist) {
        return new Point(getX() + faceOffsetDist * Math.cos(getAngle()),
                getY() + faceOffsetDist * Math.sin(getAngle()));
    }

    public boolean isSkillLearned(SkillType skillType) {
        for (SkillType skill : this.getSkills()) {
            if (skill == skillType) {
                return true;
            }
        }
        return false;
    }

    public double getShieldedLife(Game game) {
        return getLife() * (hasBonus(StatusType.SHIELDED) ? 1 + game.getShieldedDirectDamageAbsorptionFactor() : 1);
    }

    public double getShieldedLifeRatio(Game game) {
        return getShieldedLife(game) / getMaxLife();
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

    static double getMagicMissileDirectDamage(WizardProxy wizard, Game game) {
        return (hasEmpowerBonus(wizard) ? game.getEmpoweredDamageFactor() : 1) * (game.getMagicMissileDirectDamage() +
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

    private static int countMagicalDamageSkills(WizardProxy wizard) {
        return countSkills(wizard, MAGICAL_DAMAGE_SKILLS);
    }

    private static int countMoveSpeedSkills(WizardProxy wizard) {
        return countSkills(wizard, MOVE_SPEED_SKILLS);
    }

    private static int countSkills(WizardProxy wizard, Set<SkillType> skills) {
        int count = 0;
        for (SkillType skill : wizard.affectedBySkills()) {
            if (skills.contains(skill)) {
                count++;
            }
        }
        return count;
    }
}