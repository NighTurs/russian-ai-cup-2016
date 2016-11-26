import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BuildingControl {

    private static final int SAME_BUILDINGS_THRESHOLD = 1;
    private static final int VISION_RANGE_DEC_TO_BE_SURE = 5;

    private final World world;
    private final Memory memory;

    public BuildingControl(Memory memory, World world) {
        this.memory = memory;
        this.world = world;
    }

    public List<Building> buildingsIncludingEnemy() {
        if (memory.getAllyGuardianTowers().isEmpty()) {
            for (Building building : world.getBuildings()) {
                if (building.getType() == BuildingType.GUARDIAN_TOWER) {
                    memory.getAllyGuardianTowers().add(building);
                }
            }
        }
        List<Building> allBuildings = new ArrayList<>();
        List<Unit> allUnitsWithVision = new ArrayList<>();
        allUnitsWithVision.addAll(Arrays.asList(world.getMinions()));
        allUnitsWithVision.addAll(Arrays.asList(world.getWizards()));
        allUnitsWithVision.addAll(Arrays.asList(world.getBuildings()));

        outerLoop:
        for (Building ally : memory.getAllyGuardianTowers()) {
            Building enemyGuardianTower = enemyMirrorStructure(ally);
            for (Building building : world.getBuildings()) {
                if (building.getDistanceTo(enemyGuardianTower) < SAME_BUILDINGS_THRESHOLD) {
                    continue outerLoop;
                }
            }
            for (Building building : memory.getDestroyedEnemyGuardianTowers()) {
                if (building.getDistanceTo(enemyGuardianTower) < SAME_BUILDINGS_THRESHOLD) {
                    continue outerLoop;
                }
            }
            boolean shouldBeSeen = false;
            for (Unit unit : allUnitsWithVision) {
                if (unit.getFaction() != ally.getFaction()) {
                    continue;
                }
                double visionRange;
                if (unit instanceof Minion) {
                    visionRange = ((Minion) unit).getVisionRange();
                } else if (unit instanceof Wizard) {
                    visionRange = ((Wizard) unit).getVisionRange();
                } else if (unit instanceof Building) {
                    visionRange = ((Building) unit).getVisionRange();
                } else {
                    continue;
                }

                if (unit.getDistanceTo(enemyGuardianTower) <= visionRange - VISION_RANGE_DEC_TO_BE_SURE) {
                    shouldBeSeen = true;
                }
            }
            if (!shouldBeSeen) {
                allBuildings.add(enemyGuardianTower);
            } else {
                memory.getDestroyedEnemyGuardianTowers().add(enemyGuardianTower);
            }
        }
        Building enemyBase =
                enemyMirrorStructure(WorldProxy.allyBase(world.getBuildings(), world.getMyPlayer()));
        boolean alreadyInVision = false;
        for (Building building : world.getBuildings()) {
            if (building.getDistanceTo(enemyBase) < SAME_BUILDINGS_THRESHOLD) {
                alreadyInVision = true;
            }
        }
        if (!alreadyInVision) {
            allBuildings.add(enemyBase);
        }
        allBuildings.addAll(unmodifiableList(world.getBuildings()));
        return allBuildings;
    }

    private Building enemyMirrorStructure(Building allyTower) {
        return new Building(-allyTower.getId(),
                world.getWidth() - allyTower.getX(),
                world.getHeight() - allyTower.getY(),
                allyTower.getSpeedX(),
                allyTower.getSpeedY(),
                allyTower.getAngle(),
                allyTower.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY,
                allyTower.getRadius(),
                allyTower.getMaxLife(),
                allyTower.getMaxLife(),
                allyTower.getStatuses(),
                allyTower.getType(),
                allyTower.getVisionRange(),
                allyTower.getAttackRange(),
                allyTower.getDamage(),
                allyTower.getCooldownTicks(),
                0);
    }

    private static <T> List<T> unmodifiableList(T[] array) {
        return Collections.unmodifiableList(Arrays.asList(array));
    }
}
