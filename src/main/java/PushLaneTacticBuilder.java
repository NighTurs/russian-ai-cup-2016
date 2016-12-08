import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int TOWER_TARGETS_THRESHOLD = 1;
    private static final double RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.9;
    private static final double TOWER_RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.7;
    private static final int EXPECT_STEPS_FORWARD_FROM_ENEMY = 15;
    private static final int TOWER_RETREAT_SPARE_TICKS = 2;
    private static final int IGNORE_RANGE = 800;
    private static final int POTENTIAL_ATTACK_RANGE = 810;
    private final DirectionOptionalTacticBuilder directionOptional;

    public PushLaneTacticBuilder(DirectionOptionalTacticBuilder directionOptional) {
        this.directionOptional = directionOptional;
    }

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        WizardProxy self = turnContainer.getSelf();

        boolean enemiesNearby = hasEnemyInUnignorableRange(turnContainer);
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
                if (enemy.isPresent() && mapUtils.getLocationType(self.getId()) != LocationType.RIVER) {
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
        if (!hasEnemyInPotentialAttackRange(turnContainer)) {
            moveBuilder.setTurn(mov.getTurn());
        } else {
            directionOptional.addTurn(turnContainer.getWorldProxy().getTickIndex(), mov.getTurn());
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
        // Middle tower near enemy base prevents me from moving forward, just ignore it
        if (turnContainer.getMapUtils().getLocationType(self.getId()) == LocationType.BOTTOM_LANE &&
                turnContainer.getMapUtils().getLocationType(building.getId()) == LocationType.MIDDLE_LANE) {
            return Action.NONE;
        }
        int c = 0;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!(unit instanceof LivingUnit)) {
                continue;
            }
            int life = ((LivingUnit) unit).getLife();
            if (turnContainer.isAllyUnit(unit) && building.getDistanceTo(unit) <= building.getAttackRange() &&
                    life >= building.getDamage() && self.getLife() >= life) {
                c++;
            }
        }
        if (c <= TOWER_TARGETS_THRESHOLD &&
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
            if (!turnContainer.isOffensiveWizard(wizard) || self.getDistanceTo(wizard) > IGNORE_RANGE) {
                continue;
            }
            enemies.add(wizard);
        }

        boolean shouldStay = false;
        for (WizardProxy enemy : enemies) {
            if (enemies.size() == 1 && enemy.getLife() + enemy.getMagicMissileDirectDamage(game) * 3 < self.getLife()) {
                return Action.PUSH;
            }
            Action actionMissle = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.MAGIC_MISSILE);
            Action actionFrostBolt = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FROST_BOLT);
            Action actionFireball = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FIREBALL);

            if (actionMissle == Action.RETREAT || actionFrostBolt == Action.RETREAT ||
                    actionFireball == Action.RETREAT) {
                return Action.RETREAT;
            } else if (actionMissle == Action.STAY || actionFrostBolt == Action.STAY || actionFireball == Action.STAY) {
                shouldStay = true;
            }
        }
        return shouldStay ? Action.STAY : Action.NONE;
    }

    private Action actionBecauseOfWizardSpell(TurnContainer turnContainer,
                                              WizardProxy enemy,
                                              ProjectileType projectileType) {
        if (!CastProjectileTacticBuilders.isProjectileLearned(turnContainer, enemy, projectileType)) {
            return Action.NONE;
        }
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();

        double distToEnemy = self.getDistanceTo(enemy);
        int untilNextMissile = CastProjectileTacticBuilders.untilNextProjectile(enemy, projectileType, game);
        double enemyCastRange = CastProjectileTacticBuilders.castRangeToWizardOptimistic(enemy,
                self,
                turnContainer.getGame(),
                projectileType);

        double distToKeep = (distToEnemy +
                Math.max(untilNextMissile - 1, EXPECT_STEPS_FORWARD_FROM_ENEMY) * self.getWizardBackwardSpeed(game) *
                        RETREAT_BACKWARD_SPEED_MULTIPLIER -
                EXPECT_STEPS_FORWARD_FROM_ENEMY * enemy.getWizardForwardSpeed(game)) - enemyCastRange;

        if (distToKeep <= 0) {
            return Action.RETREAT;
        } else if (distToKeep <= self.getWizardForwardSpeed(game)) {
            return Action.STAY;
        }
        return Action.NONE;
    }

    private static boolean hasEnemyInUnignorableRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, IGNORE_RANGE);
    }

    public static boolean hasEnemyInPotentialAttackRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, POTENTIAL_ATTACK_RANGE);
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
