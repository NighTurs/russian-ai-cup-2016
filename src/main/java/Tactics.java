public final class Tactics {

    private Tactics() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static final int PUSH_LANE_TACTIC_PRIORITY = 10;
    public static final int CAST_MAGIC_MISSILE_TACTIC_PRIORITY = 12;
    public static final int CAST_FIREBALL_TACTIC_PRIORITY = 16;
    public static final int CAST_FROST_BOLT_TACTIC_PRIORITY = 17;
    public static final int SURVIVE_TACTIC_PRIORITY = 100;
    public static final int GO_FOR_BONUS_TACTIC_PRIORITY = 30;
    public static final int STAFF_HIT_TACTIC_PRIORITY = 13;
    public static final int DODGE_PROJECTILE_TACTIC_PRIORITY = 150;
    public static final int LEARN_SKILLS_TACTIC_BUILDER = 200;
    public static final int NEUTRAL_TURN_TACTIC_BUILDER = 1;
    public static final int DIRECTION_OPTIONAL_TACTIC_BUILDER = 2;
    public static final int MASTER_WIZARD_TACTIC_BUILDER = 300;
    public static final int APPLY_HASTE_TACTIC_BUILDER = 40;
    public static final int APPLY_SHIELD_TACTIC_BUILDER = 41;
}
