import model.Building;
import model.BuildingType;
import model.Unit;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class UnitLocationTypeTest {

    private UnitLocationTypeTest() {
    }

    public static void test(MyStrategy myStrategy) {
        TurnContainer turnContainer = myStrategy.turnContainer;
        Map<LocationType, Integer> counter = new EnumMap<>(LocationType.class);
        Map<LocationType, Integer> towerCounter = new EnumMap<>(LocationType.class);
        for (LocationType type : LocationType.values()) {
            counter.put(type, 0);
            towerCounter.put(type, 0);
        }
        for (Unit unit : turnContainer.getWorldProxy().allUnits()) {
            LocationType type = turnContainer.getUnitLocationType().getLocationType(unit.getId());
            assertNotNull(type);
            counter.put(type, counter.get(type) + 1);
            if (unit instanceof Building && ((Building) unit).getType() == BuildingType.FACTION_BASE) {
                assertTrue("Base building is withing base",
                        type == LocationType.ENEMY_BASE || type == LocationType.ALLY_BASE);
            }
            if (unit instanceof Building && ((Building) unit).getType() == BuildingType.GUARDIAN_TOWER) {
                assertTrue(String.format("Tower should be on lane, but on %s", type),
                        type == LocationType.TOP_LANE || type == LocationType.BOTTOM_LANE ||
                                type == LocationType.MIDDLE_LANE);
                towerCounter.put(type, towerCounter.get(type) + 1);
            }
        }
        for (Map.Entry<LocationType, Integer> entry : counter.entrySet()) {
            if (entry.getKey() == LocationType.ENEMY_BASE || entry.getKey() == LocationType.RIVER) {
                continue;
            }
            assertTrue(String.format("Location %s can't be empty", entry.getKey()), entry.getValue() > 0);
        }
        if (turnContainer.getWorldProxy().getTickIndex() < 100) {
            for (Map.Entry<LocationType, Integer> entry : counter.entrySet()) {
                if (!Arrays.asList(LocationType.TOP_LANE, LocationType.BOTTOM_LANE, LocationType.MIDDLE_LANE)
                        .contains(entry.getKey())) {
                    continue;
                }
                assertTrue("At start of the game every lane has towers",
                        entry.getValue() > 0);
            }
        }
    }
}
