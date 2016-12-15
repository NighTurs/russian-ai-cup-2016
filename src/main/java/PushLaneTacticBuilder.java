import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int TOWER_TARGETS_THRESHOLD = 1;
    private static final double RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.9;
    private static final double TOWER_RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.5;
    private static final int TOWER_RETREAT_SPARE_TICKS = 15;
    private static final int IGNORE_RANGE = 800;
    private static final int POTENTIAL_ATTACK_RANGE = 810;
    private static final int LIFE_ADVANTAGE_FORWARD_STEPS = 5;
    private static final int MAX_PUSH_EXPECTATIONS = 30;
    private static final Action RETREAT_ACTION = new Action(ActionType.RETREAT);
    private static final Action STAY_ACTION = new Action(ActionType.STAY);
    private static final Action NONE_ACTION = new Action(ActionType.PUSH);
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
            if (buildingAction.getActionType() == ActionType.RETREAT ||
                    minionAction.getActionType() == ActionType.RETREAT ||
                    wizardsAction.getActionType() == ActionType.RETREAT) {
                action = RETREAT_ACTION;
            } else if (buildingAction.getActionType() == ActionType.STAY ||
                    minionAction.getActionType() == ActionType.STAY ||
                    wizardsAction.getActionType() == ActionType.STAY) {
                action = STAY_ACTION;
            } else {
                Optional<Integer> minDuration = Stream.of(buildingAction, minionAction, wizardsAction)
                        .map(Action::getDuration)
                        .filter(Objects::nonNull)
                        .min(Integer::compare);
                action = new Action(ActionType.PUSH, minDuration.isPresent() ? minDuration.get() : 0);
            }
        } else {
            action = actionBecauseOfBuildings(turnContainer);
            if (action.getActionType() == ActionType.NONE) {
                action = NONE_ACTION;
            }
        }

        Point pushWaypoint = mapUtils.pushWaypoint(self.getX(), self.getY(), lane);
        Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane);
        Movement mov;

        turnContainer.getMemory().setExpectedPushDuration(0);
        switch (action.getActionType()) {
            case STAY:
                return Optional.empty();
            case RETREAT:
                mov = turnContainer.getPathFinder()
                        .findPath(self, retreatWaypoint.getX(), retreatWaypoint.getY(), 0, true);
                break;
            case PUSH:
                Optional<Unit> enemy = nearestEnemy(turnContainer);
                //noinspection OptionalIsPresent
                if (enemy.isPresent() && mapUtils.getLocationType(self.getId()) != LocationType.RIVER) {
                    mov = turnContainer.getPathFinder()
                            .findPath(self,
                                    enemy.get().getX(),
                                    enemy.get().getY(),
                                    ((CircularUnit) enemy.get()).getRadius(),
                                    true);
                    turnContainer.getMemory()
                            .setExpectedPushDuration(action.getDuration() == null ? 0 : action.getDuration());
                } else {
                    mov = turnContainer.getPathFinder()
                            .findPath(self, pushWaypoint.getX(), pushWaypoint.getY(), 0, true);
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
                return RETREAT_ACTION;
            }
            if (minTriggerTargetDist > dist - minDist) {
                minTriggerTargetDist = dist - minDist;
            }
        }
        if (minTriggerTargetDist < self.getWizardForwardSpeed(turnContainer.getGame())) {
            return STAY_ACTION;
        } else {
            return new Action(ActionType.NONE,
                    (int) (minTriggerTargetDist / self.getWizardForwardSpeed(turnContainer.getGame())));
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
            if (actionBecauseOfBuilding(turnContainer, building).getActionType() == ActionType.RETREAT) {
                return RETREAT_ACTION;
            }
        }
        for (Building building : moveOutsideOfRange) {
            if (actionBecauseOfBuilding(turnContainer, building).getActionType() == ActionType.RETREAT) {
                return STAY_ACTION;
            }
        }
        return NONE_ACTION;
    }

    private Action actionBecauseOfBuilding(TurnContainer turnContainer, Building building) {
        WizardProxy self = turnContainer.getSelf();
        // Middle tower near enemy base prevents me from moving forward, just ignore it
        if (turnContainer.getMapUtils().getLocationType(self.getId()) == LocationType.BOTTOM_LANE &&
                turnContainer.getMapUtils().getLocationType(building.getId()) == LocationType.MIDDLE_LANE) {
            return NONE_ACTION;
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
            return RETREAT_ACTION;
        }
        return NONE_ACTION;
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
        int minNoneDuration = Integer.MAX_VALUE;
        for (WizardProxy enemy : enemies) {
            if (enemies.size() == 1 && enemy.getLife() + enemy.getMagicMissileDirectDamage(game) * 2 < self.getLife()) {
                return new Action(ActionType.PUSH, MAX_PUSH_EXPECTATIONS);
            }
            if (self.getLife() -
                    (int) Math.ceil(enemy.getLife() / self.getMagicMissileDirectDamage(game)) * enemies.size() *
                            enemy.getMagicMissileDirectDamage(game) > enemy.getMagicMissileDirectDamage(game) * 3) {
                return new Action(ActionType.PUSH, MAX_PUSH_EXPECTATIONS);
            }
            Action actionMissle = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.MAGIC_MISSILE);
            Action actionFrostBolt = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FROST_BOLT);
            Action actionFireball = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FIREBALL);

            if (actionMissle.getActionType() == ActionType.RETREAT ||
                    actionFrostBolt.getActionType() == ActionType.RETREAT ||
                    actionFireball.getActionType() == ActionType.RETREAT) {
                return RETREAT_ACTION;
            } else if (actionMissle.getActionType() == ActionType.STAY ||
                    actionFrostBolt.getActionType() == ActionType.STAY ||
                    actionFireball.getActionType() == ActionType.STAY) {
                shouldStay = true;
            }
            //noinspection OptionalGetWithoutIsPresent
            minNoneDuration = Stream.of(actionMissle.getDuration(),
                    actionFireball.getDuration(),
                    actionFrostBolt.getDuration(),
                    minNoneDuration).filter(Objects::nonNull).min(Integer::compare).get();
        }
        return shouldStay ?
                STAY_ACTION :
                new Action(ActionType.NONE, minNoneDuration == Integer.MAX_VALUE ? 0 : minNoneDuration);
    }

    private Action actionBecauseOfWizardSpell(TurnContainer turnContainer,
                                              WizardProxy enemy,
                                              ProjectileType projectileType) {
        if (!CastProjectileTacticBuilders.isProjectileLearned(turnContainer, enemy, projectileType)) {
            return NONE_ACTION;
        }
        WizardProxy self = turnContainer.getSelf();
        Game game = turnContainer.getGame();

        double distToEnemy = self.getDistanceTo(enemy);
        int untilNextMissile = CastProjectileTacticBuilders.untilNextProjectile(enemy, projectileType, game);
        double enemyCastRange = turnContainer.getCastRangeService()
                .castRangeToWizardOptimistic(enemy, self, turnContainer.getGame(), projectileType)
                .getDistToCenter();

        int expectedStepsForward = enemyExpectedStepsForward(turnContainer, self, enemy, projectileType);
        double distToKeep = (distToEnemy +
                Math.max(untilNextMissile - 1, expectedStepsForward) * self.getWizardBackwardSpeed(game) *
                        RETREAT_BACKWARD_SPEED_MULTIPLIER - expectedStepsForward * enemy.getWizardForwardSpeed(game)) -
                enemyCastRange;

        if (distToKeep <= 0) {
            return RETREAT_ACTION;
        } else if (distToKeep <= self.getWizardForwardSpeed(game)) {
            return STAY_ACTION;
        }
        return new Action(ActionType.NONE, (int) Math.floor(distToKeep / self.getWizardForwardSpeed(game)));
    }

    private int enemyExpectedStepsForward(TurnContainer turnContainer,
                                          WizardProxy self,
                                          WizardProxy enemy,
                                          ProjectileType projectileType) {
        Game game = turnContainer.getGame();
        int maxForwardSteps = (int) Math.ceil(Math.PI / self.getWizardMaxTurnAngle(game));
        int untilProjectileCast = CastProjectileTacticBuilders.untilNextProjectile(enemy, projectileType, game);
        int untilSameOrBetterProjectileCast = Integer.MAX_VALUE;
        if (self.isSkillLearned(SkillType.FROST_BOLT)) {
            untilSameOrBetterProjectileCast =
                    CastProjectileTacticBuilders.untilNextProjectile(self, ProjectileType.FROST_BOLT, game);
        }
        if (self.isSkillLearned(SkillType.FIREBALL) &&
                (projectileType == ProjectileType.MAGIC_MISSILE || projectileType == ProjectileType.FIREBALL)) {
            untilSameOrBetterProjectileCast =
                    Math.min(CastProjectileTacticBuilders.untilNextProjectile(self, ProjectileType.FIREBALL, game),
                            untilSameOrBetterProjectileCast);
        }
        if (projectileType == ProjectileType.MAGIC_MISSILE) {
            untilSameOrBetterProjectileCast =
                    Math.min(CastProjectileTacticBuilders.untilNextProjectile(self, ProjectileType.MAGIC_MISSILE, game),
                            untilSameOrBetterProjectileCast);
        }
        int lifeAdvantage = Math.max(0, enemy.getLife() - self.getLife());
        int lifeAdvantageForwardSteps = Math.min(maxForwardSteps,
                (lifeAdvantage / game.getMagicMissileDirectDamage()) * LIFE_ADVANTAGE_FORWARD_STEPS);
        if (untilProjectileCast > untilSameOrBetterProjectileCast) {
            return lifeAdvantageForwardSteps;
        }
        boolean hasAlternativeTargets = false;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!turnContainer.isAllyUnit(unit) ||
                    ((projectileType == ProjectileType.FIREBALL || projectileType == ProjectileType.FROST_BOLT) &&
                            unit instanceof Minion) || !(unit instanceof LivingUnit)) {
                continue;
            }
            if (CastProjectileTacticBuilders.isInCastRange(turnContainer, enemy, unit, projectileType) &&
                    ((LivingUnit) unit).getLife() > game.getMagicMissileDirectDamage()) {
                hasAlternativeTargets = true;
            }
        }
        if (!hasAlternativeTargets) {
            return maxForwardSteps;
        } else {
            return lifeAdvantageForwardSteps;
        }
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

    public static class Action {

        private final ActionType actionType;
        private final Integer duration;

        public Action(ActionType actionType) {
            this(actionType, null);
        }

        public Action(ActionType actionType, Integer duration) {
            this.actionType = actionType;
            this.duration = duration;
        }

        public ActionType getActionType() {
            return actionType;
        }

        public Integer getDuration() {
            return duration;
        }
    }

    public enum ActionType {
        RETREAT,
        PUSH,
        STAY,
        NONE
    }
}
