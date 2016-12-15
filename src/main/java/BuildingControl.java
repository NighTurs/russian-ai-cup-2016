import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BuildingControl {

    private static final int SAME_BUILDINGS_THRESHOLD = 1;
    private static final int VISION_RANGE_DEC_TO_BE_SURE = 5;
    private final List<Building> buildingsIncludingEnemy;

    public BuildingControl(Wizard self, Memory memory, World world, Game game) {
        this.buildingsIncludingEnemy = buildingsIncludingEnemy(self, memory, world, game);
    }

    public List<Building> getBuildingsIncludingEnemy() {
        return buildingsIncludingEnemy;
    }

    private static List<Building> buildingsIncludingEnemy(Wizard self, Memory memory, World world, Game game) {
        if (memory.getAllyGuardianTowers().isEmpty()) {
            for (Building building : world.getBuildings()) {
                if (building.getType() == BuildingType.GUARDIAN_TOWER) {
                    memory.getAllyGuardianTowers().add(building);
                }
            }
        }
        List<Building> allBuildings = new ArrayList<>();
        List<LivingUnit> allUnitsWithVision = new ArrayList<>();
        allUnitsWithVision.addAll(Arrays.asList(world.getMinions()));
        allUnitsWithVision.addAll(Arrays.asList(world.getWizards()));
        allUnitsWithVision.addAll(Arrays.asList(world.getBuildings()));
        List<LivingUnit> allAllyUnits = WorldProxy.allyAllyUnits(self, world);

        outerLoop:
        for (Building ally : memory.getAllyGuardianTowers()) {
            updateBuildingCooldown(ally, allAllyUnits, world, memory, game);
            Building enemyGuardianTower = enemyMirrorStructure(ally, memory, world);
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
        Building allyBase = WorldProxy.allyBase(world.getBuildings(), world.getMyPlayer());
        updateBuildingCooldown(allyBase, allAllyUnits, world, memory, game);
        Building enemyBase = enemyMirrorStructure(allyBase, memory, world);
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

        memory.getLifeByLivingUnit().clear();
        for (LivingUnit livingUnit : allUnitsWithVision) {
            memory.getLifeByLivingUnit().put(livingUnit.getId(), livingUnit.getLife());
        }
        return allBuildings;
    }

    private static void updateBuildingCooldown(Building allyBuilding,
                                               List<LivingUnit> allAllyUnits,
                                               World world,
                                               Memory memory,
                                               Game game) {
        double x = world.getWidth() - allyBuilding.getX();
        int key = (int) x;
        double y = world.getHeight() - allyBuilding.getY();
        for (Building building : memory.getDestroyedEnemyGuardianTowers()) {
            if (building.getDistanceTo(x, y) < SAME_BUILDINGS_THRESHOLD) {
                return;
            }
        }
        for (Building building : world.getBuildings()) {
            if (building.getDistanceTo(x, y) < SAME_BUILDINGS_THRESHOLD) {
                memory.getBuildingCooldownByX().put(key, building.getRemainingActionCooldownTicks());
                return;
            }
        }
        memory.getBuildingCooldownByX().putIfAbsent(key, 0);

        if (memory.getBuildingCooldownByX().get(key) == 0) {
            LivingUnit bestUnit = null;
            int bestEffectiveDamage = 0;
            int bestHP = 0;
            for (LivingUnit livingUnit : allAllyUnits) {
                double dist = livingUnit.getDistanceTo(x, y);
                Integer previousLife = memory.getLifeByLivingUnit().get(livingUnit.getId());
                if (previousLife == null) {
                    continue;
                }
                if (dist <= allyBuilding.getAttackRange() &&
                        ((bestEffectiveDamage < Math.min(allyBuilding.getDamage(), previousLife) ||
                                (bestEffectiveDamage == Math.min(allyBuilding.getDamage(), previousLife) &&
                                        bestHP > previousLife)))) {
                    bestEffectiveDamage = Math.min(allyBuilding.getDamage(), previousLife);
                    bestHP = previousLife;
                    bestUnit = livingUnit;
                }
            }
            if (bestUnit != null) {
                int previousLife = memory.getLifeByLivingUnit().get(bestUnit.getId());
                if (previousLife - bestUnit.getLife() >= allyBuilding.getDamage() / 2) {
                    memory.getBuildingCooldownByX().put(key, allyBuilding.getCooldownTicks() - 1);
                    return;
                }
            }
        }
        memory.getBuildingCooldownByX().put(key, Math.max(0, memory.getBuildingCooldownByX().get(key) - 1));
    }

    private static Building enemyMirrorStructure(Building allyTower, Memory memory, World world) {
        double x = world.getWidth() - allyTower.getX();
        return new Building(-allyTower.getId(),
                x,
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
                memory.getBuildingCooldownByX().get((int) x));
    }

    private static <T> List<T> unmodifiableList(T[] array) {
        return Collections.unmodifiableList(Arrays.asList(array));
    }
}
