import model.Building;
import model.Game;
import model.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitLocationType {

    private final static double EDGLE_LINE_WIDTH_MULTIPLIER = 2.1;
    private final Map<Long, LocationType> store;
    private final double allyBaseXRight;
    private final double allyBaseYTop;
    private final double enemyBaseXLeft;
    private final double enemyBaseYBottom;
    private final double laneWidth;
    private final double worldWidth;
    private final double worldHeight;

    public UnitLocationType(WorldProxy world, Game game) {
        this.store = new HashMap<>();
        List<Unit> units = world.allUnits();

        Building allyBase = world.allyBase();
        this.allyBaseXRight = allyBase.getX() * 2;
        this.allyBaseYTop = world.getHeight() - (world.getHeight() - allyBase.getY()) * 2;
        this.enemyBaseXLeft = world.getWidth() - allyBaseXRight;
        this.enemyBaseYBottom = (world.getHeight() - allyBase.getY()) * 2;
        this.laneWidth = allyBaseXRight * 2 / 3;
        this.worldWidth = world.getWidth();
        this.worldHeight = world.getHeight();

        for (Unit unit : units) {

            store.put(unit.getId(), getLocationType(unit.getX(), unit.getY()));
        }
    }

    public LocationType getLocationType(long id) {
        LocationType type = store.get(id);
        if (type == null) {
            throw new RuntimeException("Unit not found");
        }
        return type;
    }

    public LocationType getLocationType(double x, double y) {
        if (x <= allyBaseXRight && y >= allyBaseYTop) {
            return LocationType.ALLY_BASE;
        } else if (x >= enemyBaseXLeft && y <= enemyBaseYBottom) {
            return LocationType.ENEMY_BASE;
        } else if (worldHeight - y <= laneWidth || worldWidth - x <= laneWidth ||
                Math.hypot(x - worldWidth, y - worldHeight) < laneWidth * EDGLE_LINE_WIDTH_MULTIPLIER) {
            return LocationType.BOTTOM_LANE;
        } else if (y <= laneWidth || x <= laneWidth || Math.hypot(x, y) < laneWidth * EDGLE_LINE_WIDTH_MULTIPLIER) {
            return LocationType.TOP_LANE;
        } else if (Math.abs((worldHeight - y) - x) <= laneWidth) {
            return LocationType.MIDDLE_LANE;
        } else if (Math.abs((worldHeight - y) - (worldWidth - x)) <= laneWidth) {
            return LocationType.RIVER;
        } else {
            return LocationType.FOREST;
        }
    }
}
