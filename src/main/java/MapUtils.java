import model.Building;
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

    private final double waypointBaseInfluence;
    private final Point laneAllyWaypoint;
    private final Point laneEnemyWaypoint;
    private final Point bottomLaneMidWaypoint;
    private final Point topLaneMidWaypoint;
    private final Point midLaneMidWaypoint;


    public MapUtils(WorldProxy world) {
        this.store = new HashMap<>();
        List<Unit> units = world.allUnits();

        Building allyBase = world.allyBase();
        double base = allyBase.getX();
        double halfBase = base / 2;

        this.allyBaseXRight = base * 2;
        this.allyBaseYTop = world.getHeight() - (world.getHeight() - allyBase.getY()) * 2;
        this.enemyBaseXLeft = world.getWidth() - allyBaseXRight;
        this.enemyBaseYBottom = (world.getHeight() - allyBase.getY()) * 2;
        this.laneWidth = base * 3 / 2;
        this.worldWidth = world.getWidth();
        this.worldHeight = world.getHeight();

        this.waypointBaseInfluence = base * 2;
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
        if (Math.hypot(x, y - worldHeight) < waypointBaseInfluence) {
            return midPoint(lane);
        }
        if (Math.hypot(x - worldWidth, y) < waypointBaseInfluence) {
            return laneEnemyWaypoint;
        }
        switch (lane) {
            case MIDDLE_LANE:
                if (curLocationType != lane) {
                    return midLaneMidWaypoint;
                }
                return laneEnemyWaypoint;
            case TOP_LANE:
                if (curLocationType != lane) {
                    return topLaneMidWaypoint;
                }
                if (y > laneWidth * 2) {
                    return topLaneMidWaypoint;
                } else {
                    return laneEnemyWaypoint;
                }
            case BOTTOM_LANE:
                if (curLocationType != lane) {
                    return bottomLaneMidWaypoint;
                }
                if (x < worldWidth - laneWidth * 2) {
                    return bottomLaneMidWaypoint;
                } else {
                    return laneEnemyWaypoint;
                }
            default:
                throw new RuntimeException(lane + " is not a lane");
        }
    }

    public Point retreatWaypoint(double x, double y, LocationType lane) {
        if (Math.hypot(x, y - worldHeight) < waypointBaseInfluence) {
            return laneAllyWaypoint;
        }
        if (Math.hypot(x - worldWidth, y) < waypointBaseInfluence) {
            return midPoint(lane);
        }
        switch (lane) {
            case MIDDLE_LANE:
                return laneAllyWaypoint;
            case TOP_LANE:
                if (x < laneWidth * 2) {
                    return laneAllyWaypoint;
                } else {
                    return topLaneMidWaypoint;
                }
            case BOTTOM_LANE:
                if (y > worldHeight - laneWidth * 2) {
                    return laneAllyWaypoint;
                } else {
                    return bottomLaneMidWaypoint;
                }
            default:
                throw new RuntimeException(lane + " is not a lane");
        }
    }

    private Point midPoint(LocationType lane) {
        switch (lane) {
            case MIDDLE_LANE:
                return midLaneMidWaypoint;
            case TOP_LANE:
                return topLaneMidWaypoint;
            case BOTTOM_LANE:
                return bottomLaneMidWaypoint;
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
}
