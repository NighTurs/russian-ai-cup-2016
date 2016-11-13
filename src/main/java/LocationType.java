public enum LocationType {
    TOP_LANE,
    MIDDLE_LANE,
    BOTTOM_LANE,
    ALLY_BASE,
    ENEMY_BASE,
    FOREST,
    RIVER;

    public boolean isLane() {
        return this == TOP_LANE || this == MIDDLE_LANE || this == BOTTOM_LANE;
    }
}
