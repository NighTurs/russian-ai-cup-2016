import model.Building;
import model.Game;
import model.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {

    private static final double EDGLE_LINE_WIDTH_MULTIPLIER = 2.1;
    private final Map<Long, LocationType> store;
    private final double allyBaseXRight;
    private final double allyBaseYTop;
    private final double enemyBaseXLeft;
    private final double enemyBaseYBottom;
    private final double laneWidth;
    private final double worldWidth;
    private final double worldHeight;

    private final Point laneAllyWaypoint;
    private final Point laneEnemyWaypoint;
    private final Point bottomLaneMidWaypoint;
    private final Point topLaneMidWaypoint;
    private final Point midLaneMidWaypoint;


    public MapUtils(WorldProxy world, Game game) {
        this.store = new HashMap<>();
        List<Unit> units = world.allUnits();

        Building allyBase = world.allyBase();
        double halfBase = allyBase.getX();

        this.allyBaseXRight = halfBase * 2;
        this.allyBaseYTop = world.getHeight() - (world.getHeight() - allyBase.getY()) * 2;
        this.enemyBaseXLeft = world.getWidth() - allyBaseXRight;
        this.enemyBaseYBottom = (world.getHeight() - allyBase.getY()) * 2;
        this.laneWidth = halfBase * 3 / 2;
        this.worldWidth = world.getWidth();
        this.worldHeight = world.getHeight();
        this.laneAllyWaypoint = new Point(halfBase, worldHeight - halfBase);
        this.laneEnemyWaypoint = new Point(worldWidth - halfBase, halfBase);
        this.bottomLaneMidWaypoint = new Point(worldWidth - halfBase, worldHeight - halfBase);
        this.topLaneMidWaypoint = new Point(halfBase, halfBase);
        this.midLaneMidWaypoint = new Point(worldWidth / 2, worldHeight / 2);

        for (Unit unit : units) {

            store.put(unit.getId(), getLocationType(unit.getX(), unit.getY()));
        }
    }

    public Point pushWaypoint(double x, double y, LocationType lane) {
        LocationType curLocationType = getLocationType(x, y);
        switch (lane) {
            case MIDDLE_LANE:
                if (curLocationType != lane && curLocationType != LocationType.ENEMY_BASE) {
                    return midLaneMidWaypoint;
                }
                return laneEnemyWaypoint;
            case TOP_LANE:
                if (curLocationType != lane && curLocationType != LocationType.ENEMY_BASE) {
                    return topLaneMidWaypoint;
                }
                if (y > laneWidth) {
                    return topLaneMidWaypoint;
                } else {
                    return laneEnemyWaypoint;
                }
            case BOTTOM_LANE:
                if (curLocationType != lane && curLocationType != LocationType.ENEMY_BASE) {
                    return bottomLaneMidWaypoint;
                }
                if (x < worldWidth - laneWidth) {
                    return bottomLaneMidWaypoint;
                } else {
                    return laneEnemyWaypoint;
                }
            default:
                throw new RuntimeException(lane + " is not a lane");
        }
    }

    public Point retreatWaypoint(double x, double y, LocationType lane) {
        switch (lane) {
            case MIDDLE_LANE:
                return laneAllyWaypoint;
            case TOP_LANE:
                if (x < laneWidth) {
                    return laneAllyWaypoint;
                } else {
                    return topLaneMidWaypoint;
                }
            case BOTTOM_LANE:
                if (y > worldHeight - laneWidth) {
                    return laneAllyWaypoint;
                } else {
                    return bottomLaneMidWaypoint;
                }
            default:
                throw new RuntimeException(lane + " is not a lane");
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

    public Point getLaneAllyWaypoint() {
        return laneAllyWaypoint;
    }

    public Point getLaneEnemyWaypoint() {
        return laneEnemyWaypoint;
    }
}
