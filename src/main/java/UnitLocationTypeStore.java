import model.Building;
import model.Game;
import model.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitLocationTypeStore {

    private final Map<Long, LocationType> store;

    public UnitLocationTypeStore(WorldProxy world, Game game) {
        this.store = new HashMap<>();
        List<Unit> units = world.allUnits();

        Building allyBase = world.allyBase();
        double allyBaseXRight = allyBase.getX() * 2;
        double allyBaseYTop = world.getHeight() - (world.getHeight() - allyBase.getY()) * 2;
        double enemyBaseXLeft = world.getWidth() -  allyBaseXRight;
        double enemyBaseYBottom = (world.getHeight() - allyBase.getY()) * 2;

        for (Unit unit : units) {
            LocationType type;
            if (unit.getX() <= allyBaseXRight && unit.getY() >= allyBaseYTop) {
                type = LocationType.ALLY_BASE;
            } else if (unit.getX() >= enemyBaseXLeft && unit.getY() <= enemyBaseYBottom) {
                type = LocationType.ENEMY_BASE;
            } else if (unit.getY() >= allyBaseYTop || unit.getX() >= enemyBaseXLeft) {
                type = LocationType.BOTTOM_LANE;
            } else if (unit.getY() <= enemyBaseYBottom || unit.getX() <= allyBaseXRight) {
                type = LocationType.TOP_LANE;
            } else if (Math.abs((world.getHeight() - unit.getY()) - unit.getX()) <= allyBaseXRight) {
                type = LocationType.MIDDLE_LANE;
            } else {
                type = LocationType.OTHER;
            }
            store.put(unit.getId(), type);
        }
    }

    public LocationType getLocationType(long id) {
        LocationType type = store.get(id);
        if (type == null) {
            throw new RuntimeException("Unit not found");
        }
        return type;
    }
}
