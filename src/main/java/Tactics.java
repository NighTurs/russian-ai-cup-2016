public final class Tactics {

    private Tactics() {
        throw new UnsupportedOperationException("Instance not supported");
    }

    public static final int PUSH_LANE_TACTIC_PRIORITY = 10;
    public static final int CAST_MAGIC_MISSILE_TACTIC_PRIORITY = 15;
    public static final int SURVIVE_TACTIC_PRIORITY = 100;
    public static final int GO_FOR_BONUS_TACTIC_PRIORITY = 30;
    public static final int STAFF_HIT_TACTIC_PRIORITY = 13;
    public static final int DODGE_PROJECTILE_TACTIC_PRIORITY = 17;
}
