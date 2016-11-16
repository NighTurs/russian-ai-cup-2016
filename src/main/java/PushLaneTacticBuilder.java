import model.*;

import java.util.Optional;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int ENEMY_MINION_MAX_DIST = 350;
    private static final int ENEMY_MIN_DIST_VS_XP_RANGE = 200;
    private static final int TOWER_DANGER_RANGE_INC = 20;
    private static final int TOWER_TARGETS_THRESHOLD = 1;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        Wizard self = turnContainer.getSelf();

        boolean enemiesNearby = hasEnemyInVisibilityRange(turnContainer);
        Action action = Action.NONE;

        if (enemiesNearby) {
            if (shouldRetreatBecauseOfMinions(lane, turnContainer) ||
                    shouldRetreatBecauseOfBuildings(lane, turnContainer)) {
                action = Action.RETREAT;
            } else if (shouldPushBecauseTooFarAway(lane, turnContainer)) {
                action = Action.PUSH;
            }
        } else {
            if (shouldRetreatBecauseOfBuildings(lane, turnContainer)) {
                action = Action.RETREAT;
            } else {
                Unit avantgard = findAllyAvantgardMinionOrBuilding(lane, turnContainer);
                Point waypointAvant = mapUtils.pushWaypoint(avantgard.getX(), avantgard.getY(), lane);
                if (avantgard.getDistanceTo(waypointAvant.getX(), waypointAvant.getY()) >
                        self.getDistanceTo(waypointAvant.getX(), waypointAvant.getY())) {
                    action = Action.RETREAT;
                } else {
                    action = Action.PUSH;
                }
            }
        }

        Point pushWaypoint = mapUtils.pushWaypoint(self.getX(), self.getY(), lane);
        Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane);
        Movement mov;

        switch (action) {
            case NONE:
                return Optional.empty();
            case RETREAT:
                mov = turnContainer.getPathFinder().findPath(self, retreatWaypoint.getX(), retreatWaypoint.getY());
                break;
            case PUSH:
                mov = turnContainer.getPathFinder().findPath(self, pushWaypoint.getX(), pushWaypoint.getY());
                break;
            default:
                throw new RuntimeException("Unexpected action " + action);
        }

        MoveBuilder moveBuilder = new MoveBuilder();
        moveBuilder.setSpeed(mov.getSpeed());
        moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
        if (!hasEnemyInAttackRange(turnContainer)) {
            moveBuilder.setTurn(mov.getTurn());
        }

        return Optional.of(new TacticImpl("PushLane", moveBuilder, Tactics.PUSH_LANE_TACTIC_PRIORITY));
    }

    private boolean shouldPushBecauseTooFarAway(LocationType lane, TurnContainer turnContainer) {
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!(unit instanceof Minion || unit instanceof Building) || !turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(unit);
            if (dist < ENEMY_MIN_DIST_VS_XP_RANGE) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldRetreatBecauseOfMinions(LocationType lane, TurnContainer turnContainer) {
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!(unit instanceof Minion) || !turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(unit);
            if (dist < ENEMY_MINION_MAX_DIST) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRetreatBecauseOfBuildings(LocationType lane, TurnContainer turnContainer) {
        Building building = null;
        for (Building unit : turnContainer.getWorldProxy().getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(unit)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(unit);
            if (dist <= unit.getAttackRange() + turnContainer.getSelf().getRadius() + TOWER_DANGER_RANGE_INC) {
                building = unit;
            }
        }
        if (building == null) {
            return false;
        }
        int c = 0;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (turnContainer.isAllyUnit(unit) && building.getDistanceTo(unit) <=
                    building.getAttackRange() + ((CircularUnit) unit).getRadius()) {
                c++;
            }
        }
        return c <= TOWER_TARGETS_THRESHOLD;
    }

    private Unit findAllyAvantgardMinionOrBuilding(LocationType lane, TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        MapUtils mapUtils = turnContainer.getMapUtils();
        Point allyPoint = mapUtils.getLaneAllyWaypoint();

        Optional<Unit> avantgardEnemy = findEnemyAvantgardMinionOrBuilding(lane, turnContainer);

        double bestDist = Double.MAX_VALUE;
        Unit bestUnit = null;
        for (Unit unit : world.allUnitsWoTrees()) {
            LocationType curLane = mapUtils.getLocationType(unit.getId());
            if (!(curLane == lane || curLane == LocationType.ALLY_BASE || curLane == LocationType.ENEMY_BASE) ||
                    !(unit instanceof Building || unit instanceof Minion) || !turnContainer.isAllyUnit(unit)) {
                continue;
            }
            double dist =
                    unit.getDistanceTo(mapUtils.getLaneEnemyWaypoint().getX(), mapUtils.getLaneEnemyWaypoint().getY());
            if (dist < bestDist && (!avantgardEnemy.isPresent() ||
                    unit.getDistanceTo(allyPoint.getX(), allyPoint.getY()) <
                            avantgardEnemy.get().getDistanceTo(allyPoint.getX(), allyPoint.getY()))) {
                bestDist = dist;
                bestUnit = unit;
            }
        }
        return bestUnit == null ? world.allyBase() : bestUnit;
    }

    private Optional<Unit> findEnemyAvantgardMinionOrBuilding(LocationType lane, TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        MapUtils mapUtils = turnContainer.getMapUtils();

        double bestDist = Double.MAX_VALUE;
        Unit bestUnit = null;
        for (Unit unit : world.allUnitsWoTrees()) {
            LocationType curLane = mapUtils.getLocationType(unit.getId());
            if (curLane != lane || !(unit instanceof Building || unit instanceof Minion) ||
                    !turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist =
                    unit.getDistanceTo(mapUtils.getLaneAllyWaypoint().getX(), mapUtils.getLaneAllyWaypoint().getY());
            if (dist < bestDist) {
                bestDist = dist;
                bestUnit = unit;
            }
        }
        return Optional.ofNullable(bestUnit);
    }

    private boolean hasEnemyInVisibilityRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, turnContainer.getSelf().getVisionRange());
    }

    private boolean hasEnemyInAttackRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer,
                WizardTraits.getWizardCastRange(turnContainer.getSelf(), turnContainer.getGame()));
    }

    private boolean hasEnemyInRange(TurnContainer turnContainer, double range) {
        Wizard self = turnContainer.getSelf();
        Faction opposingFaction = turnContainer.opposingFaction();
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (unit.getFaction() != opposingFaction) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            if (dist < range) {
                return true;
            }
        }
        return false;
    }

    private enum Action {
        RETREAT,
        PUSH,
        NONE
    }
}
