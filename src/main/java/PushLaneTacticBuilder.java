import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int TOWER_TARGETS_THRESHOLD = 3;
    private static final double TOWER_DANGER_LIFE_RATIO_THRESHOLD = 0.8;
    private static final double RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.9;
    private static final double TOWER_RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.7;
    private static final int EXPECT_STEPS_FORWARD_FROM_ENEMY = 15;
    private static final int TOWER_RETREAT_SPARE_TICKS = 2;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        WizardProxy self = turnContainer.getSelf();

        boolean enemiesNearby = hasEnemyInVisibilityRange(turnContainer);
        Action action;

        if (enemiesNearby) {
            Action buildingAction = actionBecauseOfBuildings(turnContainer);
            Action minionAction = actionBecauseOfMinions(turnContainer);
            Action wizardsAction = actionBecauseOfWizards(turnContainer);
            if (buildingAction == Action.RETREAT || minionAction == Action.RETREAT || wizardsAction == Action.RETREAT) {
                action = Action.RETREAT;
            } else if (buildingAction == Action.STAY || minionAction == Action.STAY || wizardsAction == Action.STAY) {
                action = Action.STAY;
            } else {
                action = Action.PUSH;
            }
        } else {
            action = actionBecauseOfBuildings(turnContainer);
            if (action == Action.NONE) {
                action = Action.PUSH;
            }
        }

        Point pushWaypoint = mapUtils.pushWaypoint(self.getX(), self.getY(), lane);
        Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane);
        Movement mov;

        switch (action) {
            case STAY:
                return Optional.empty();
            case RETREAT:
                mov = turnContainer.getPathFinder().findPath(self, retreatWaypoint.getX(), retreatWaypoint.getY());
                break;
            case PUSH:
                Optional<Unit> enemy = nearestEnemy(turnContainer);
                //noinspection OptionalIsPresent
                if (enemy.isPresent()) {
                    mov = turnContainer.getPathFinder().findPath(self, enemy.get().getX(), enemy.get().getY());
                } else {
                    mov = turnContainer.getPathFinder().findPath(self, pushWaypoint.getX(), pushWaypoint.getY());
                }
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

    private static Optional<Unit> nearestEnemy(TurnContainer turnContainer) {
        double minDist = Double.MAX_VALUE;
        Unit nearestEnemy = null;
        for (Unit unit : turnContainer.getWorldProxy().getAllUnitsNearby()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = unit.getDistanceTo(turnContainer.getSelf());
            if (minDist > dist) {
                minDist = dist;
                nearestEnemy = unit;
            }
        }
        return Optional.ofNullable(nearestEnemy);
    }

    public static Action actionBecauseOfMinions(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        double minTriggerTargetDist = Double.MAX_VALUE;
        for (Minion minion : turnContainer.getWorldProxy().getMinions()) {
            if (!turnContainer.isOffensiveUnit(minion)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(minion);
            if (dist > (minion.getType() == MinionType.FETISH_BLOWDART ?
                    game.getFetishBlowdartAttackRange() :
                    game.getOrcWoodcutterAttackRange()) + self.getWizardForwardSpeed(game) + self.getRadius()) {
                continue;
            }
            double minDist = Double.MAX_VALUE;
            for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
                if (turnContainer.isAllyUnit(unit) && minion.getDistanceTo(unit) < minDist) {
                    minDist = minion.getDistanceTo(unit);
                }
            }
            if (dist < minDist) {
                return Action.RETREAT;
            }
            if (minTriggerTargetDist > dist - minDist) {
                minTriggerTargetDist = dist - minDist;
            }
        }
        if (minTriggerTargetDist < self.getWizardForwardSpeed(turnContainer.getGame())) {
            return Action.STAY;
        } else {
            return Action.NONE;
        }
    }

    private Action actionBecauseOfBuildings(TurnContainer turnContainer) {
        List<Building> buildings = new ArrayList<>();
        List<Building> moveOutsideOfRange = new ArrayList<>();
        for (Building unit : turnContainer.getWorldProxy().getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(unit)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(unit);
            if (dist <= unit.getAttackRange()) {
                buildings.add(unit);
            }
            if (dist <=
                    unit.getAttackRange() + turnContainer.getSelf().getWizardForwardSpeed(turnContainer.getGame())) {
                moveOutsideOfRange.add(unit);
            }
        }
        for (Building building : buildings) {
            if (actionBecauseOfBuilding(turnContainer, building) == Action.RETREAT) {
                return Action.RETREAT;
            }
        }
        for (Building building : moveOutsideOfRange) {
            if (actionBecauseOfBuilding(turnContainer, building) == Action.RETREAT) {
                return Action.STAY;
            }
        }
        return Action.NONE;
    }

    private Action actionBecauseOfBuilding(TurnContainer turnContainer, Building building) {
        WizardProxy self = turnContainer.getSelf();
        int c = 0;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (turnContainer.isAllyUnit(unit) && building.getDistanceTo(unit) <= building.getAttackRange()) {
                c++;
            }
        }
        if ((c <= TOWER_TARGETS_THRESHOLD ||
                (double) turnContainer.getSelf().getLife() / turnContainer.getSelf().getMaxLife() <
                        TOWER_DANGER_LIFE_RATIO_THRESHOLD) &&
                self.getDistanceTo(building) - self.getWizardForwardSpeed(turnContainer.getGame()) +
                        Math.max((building.getRemainingActionCooldownTicks() - TOWER_RETREAT_SPARE_TICKS), 0) *
                                self.getWizardBackwardSpeed(turnContainer.getGame()) *
                                TOWER_RETREAT_BACKWARD_SPEED_MULTIPLIER <= building.getAttackRange()) {
            return Action.RETREAT;
        }
        return Action.NONE;
    }

    private Action actionBecauseOfWizards(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        List<WizardProxy> enemies = new ArrayList<>();
        for (WizardProxy wizard : turnContainer.getWorldProxy().getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard) || self.getDistanceTo(wizard) > self.getVisionRange()) {
                continue;
            }
            enemies.add(wizard);
        }

        for (WizardProxy enemy : enemies) {
            if (enemies.size() == 1 && enemy.getLife() + enemy.getMagicMissileDirectDamage(game) * 3 < self.getLife()) {
                return Action.PUSH;
            }
            double distToEnemy = self.getDistanceTo(enemy);
            double untilNextMissile =
                    Math.max(enemy.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()],
                            enemy.getRemainingActionCooldownTicks());
            double enemyCastRange =
                    CastProjectileTacticBuilders.castRangeToWizardOptimistic(enemy, self, turnContainer.getGame());

            double distToKeep = (distToEnemy +
                    (untilNextMissile - 1) * self.getWizardBackwardSpeed(game) * RETREAT_BACKWARD_SPEED_MULTIPLIER -
                    Math.min(untilNextMissile + 1, EXPECT_STEPS_FORWARD_FROM_ENEMY) *
                            enemy.getWizardForwardSpeed(game)) - enemyCastRange;

            if (distToKeep <= 0) {
                return Action.RETREAT;
            } else if (distToKeep <= self.getWizardForwardSpeed(game)) {
                return Action.STAY;
            }
        }
        return Action.NONE;
    }

    private static boolean hasEnemyInVisibilityRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, turnContainer.getSelf().getVisionRange());
    }

    public static boolean hasEnemyInAttackRange(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            double range = 0;
            if (unit instanceof Building) {
                range = CastProjectileTacticBuilders.castRangeToBuilding(self, (Building) unit, game);
            } else if (unit instanceof Minion) {
                range = CastProjectileTacticBuilders.castRangeToMinion(self, (Minion) unit, game);
            } else if (unit instanceof WizardProxy) {
                range = CastProjectileTacticBuilders.castRangeToWizardPessimistic(self, (WizardProxy) unit, game);
            }
            if (dist <= range) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEnemyInRange(TurnContainer turnContainer, double range) {
        WizardProxy self = turnContainer.getSelf();
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!turnContainer.isOffensiveUnit(unit)) {
                continue;
            }
            double dist = unit.getDistanceTo(self);
            if (dist <= range) {
                return true;
            }
        }
        return false;
    }

    public enum Action {
        RETREAT,
        PUSH,
        STAY,
        NONE
    }
}
