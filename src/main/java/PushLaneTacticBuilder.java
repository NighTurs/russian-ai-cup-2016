import model.*;

import java.util.*;
import java.util.stream.Stream;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int TOWER_TARGETS_THRESHOLD = 1;
    private static final double RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.9;
    private static final double TOWER_RETREAT_BACKWARD_SPEED_MULTIPLIER = 0.5;
    private static final int TOWER_RETREAT_SPARE_TICKS = 15;
    private static final int IGNORE_RANGE = 800;
    private static final int POTENTIAL_ATTACK_RANGE = 810;
    private static final int LIFE_ADVANTAGE_FORWARD_STEPS = 5;
    private static final int ENEMY_PREV_TURN_FORWARD_STEPS = 7;
    private static final int DEFAULT_ENEMY_FORWARD_STEPS_WITH_COOLDOWN_ADVANTAGE = 7;
    private static final int DEFAULT_ENEMY_FORWARD_STEPS_WITH_TEAM_ADVANTAGE = 5;
    private static final int MAX_PUSH_EXPECTATIONS = 30;
    private static final int ENEMY_INTENTIONAL_MOVE_THRESHOLD = 2;
    private static final double TEAM_HEALTH_ADVANTAGE_RATIO = 1.5;
    private static final double TEAM_HEALTH_BIG_ADVANTAGE_RATIO = 2.3;
    private static final double TEAM_LEVEL_ADVANTAGE_RATIO = 2;
    private static final double TANK_TOWER_HIT_LIFE_THRESHOLD = 0.9;
    private static final int GO_HAM_WIZARDS_THRESHOLD = 3;
    private static final double GO_HAM_LIFE_RATIO_THRESHOLD = 0.3;
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

        Point pushWaypoint = null;
        Point retreatWaypoint = null;
        LocationType myLocation = mapUtils.getLocationType(self.getId());

        if (enemiesNearby) {
            Action buildingAction = actionBecauseOfBuildings(turnContainer);
            Action minionAction = actionBecauseOfMinions(turnContainer);
            Action wizardsAction = actionBecauseOfWizards(turnContainer);
            if (buildingAction.getActionType() == ActionType.RETREAT ||
                    minionAction.getActionType() == ActionType.RETREAT ||
                    wizardsAction.getActionType() == ActionType.RETREAT) {
                action = RETREAT_ACTION;
                if (turnContainer.getGame().isRawMessagesEnabled() && wizardsAction.getCustomRetreatPoint() != null &&
                        lane == myLocation) {
                    retreatWaypoint = wizardsAction.getCustomRetreatPoint();
                }
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
                if (turnContainer.getGame().isRawMessagesEnabled() && wizardsAction.getCustomPushPoint() != null &&
                        lane == myLocation) {
                    pushWaypoint = wizardsAction.getCustomPushPoint();
                }
            }
        } else {
            action = actionBecauseOfBuildings(turnContainer);
            if (action.getActionType() == ActionType.NONE) {
                action = NONE_ACTION;
            }
        }

        if (pushWaypoint == null) {
            pushWaypoint = mapUtils.pushWaypoint(self.getX(), self.getY(), lane, turnContainer.getMemory());
        }
        if (retreatWaypoint == null) {
            retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane, turnContainer.getMemory());
        }
        Movement mov;

        turnContainer.getMemory().setExpectedPushDuration(0);
        Optional<Point> goHam = goHam(turnContainer);
        if (goHam.isPresent()) {
            mov = turnContainer.getPathFinder().findPath(self, goHam.get().getX(), goHam.get().getY(), 0, true);
        } else {
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
                    if (enemy.isPresent() && (myLocation == lane || myLocation == LocationType.ENEMY_BASE ||
                            myLocation == LocationType.ALLY_BASE)) {
                        Point pushPoint = enemyUnitPushPoint(turnContainer, enemy.get());
                        mov = turnContainer.getPathFinder()
                                .findPath(self,
                                        pushPoint.getX(),
                                        pushPoint.getY(),
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
            if (!turnContainer.isOffensiveUnit(unit) || (unit instanceof Building &&
                    turnContainer.getMapUtils().isIgnorableBuilding(turnContainer.getSelf(), (Building) unit))) {
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
        double minDistToMinion = Double.MAX_VALUE;
        for (Minion minion : turnContainer.getWorldProxy().getMinions()) {
            if (!turnContainer.isOffensiveUnit(minion)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(minion);
            if (dist < minDistToMinion) {
                minDistToMinion = dist;
            }
            if (dist > (minion.getType() == MinionType.FETISH_BLOWDART ?
                    game.getFetishBlowdartAttackRange() :
                    game.getOrcWoodcutterAttackRange()) + self.getRadius() + self.getWizardForwardSpeed(game) +
                    game.getMinionSpeed()) {
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
        if (minTriggerTargetDist < self.getWizardForwardSpeed(turnContainer.getGame()) ||
                minDistToMinion <= game.getStaffRange()) {
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
        if (turnContainer.getMapUtils().isIgnorableBuilding(self, building)) {
            return NONE_ACTION;
        }
        TeamAdvantageService teamAdvantageService = turnContainer.getTeamAdvantageService();
        if (turnContainer.getGame().isRawMessagesEnabled() &&
                teamAdvantageService.getHealthAlly() / TEAM_HEALTH_ADVANTAGE_RATIO >
                        teamAdvantageService.getHealthEnemy() &&
                self.getLife() / (double) self.getMaxLife() >= TANK_TOWER_HIT_LIFE_THRESHOLD) {
            return NONE_ACTION;
        }
        int c = 0;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (!(unit instanceof LivingUnit)) {
                continue;
            }
            int life = ((LivingUnit) unit).getLife();
            if (turnContainer.isAllyUnit(unit) &&
                    !(turnContainer.getGame().isRawMessagesEnabled() && unit instanceof WizardProxy) &&
                    building.getDistanceTo(unit) <= building.getAttackRange() && life >= building.getDamage() &&
                    self.getLife() >= life) {
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
                return new Action(ActionType.PUSH, MAX_PUSH_EXPECTATIONS, new Point(enemy.getX(), enemy.getY()), null);
            }
            if (self.getLife() -
                    (int) Math.ceil(enemy.getLife() / self.getMagicMissileDirectDamage(game)) * enemies.size() *
                            enemy.getMagicMissileDirectDamage(game) > enemy.getMagicMissileDirectDamage(game) * 3) {
                return new Action(ActionType.PUSH, MAX_PUSH_EXPECTATIONS, new Point(enemy.getX(), enemy.getY()), null);
            }
            Action actionMissle = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.MAGIC_MISSILE);
            Action actionFrostBolt = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FROST_BOLT);
            Action actionFireball = actionBecauseOfWizardSpell(turnContainer, enemy, ProjectileType.FIREBALL);

            if (actionMissle.getActionType() == ActionType.RETREAT ||
                    actionFrostBolt.getActionType() == ActionType.RETREAT ||
                    actionFireball.getActionType() == ActionType.RETREAT) {
                return new Action(ActionType.RETREAT,
                        0,
                        null,
                        MathMethods.distPoint(enemy.getX(), enemy.getY(), self.getX(), self.getY(), IGNORE_RANGE));
            } else if (actionMissle.getActionType() == ActionType.STAY ||
                    actionFrostBolt.getActionType() == ActionType.STAY ||
                    actionFireball.getActionType() == ActionType.STAY) {
                shouldStay = true;
            }
            if (self.getDistanceTo(enemy) < turnContainer.getCastRangeService()
                    .castRangeToWizardPessimistic(self, enemy, game, ProjectileType.MAGIC_MISSILE)
                    .getDistToCenter() - enemy.getWizardForwardSpeed(game) * 2) {
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
                new Action(ActionType.NONE,
                        minNoneDuration == Integer.MAX_VALUE ? 0 : minNoneDuration,
                        enemies.stream()
                                .min(Comparator.comparingDouble(self::getDistanceTo))
                                .map(x -> new Point(x.getX(), x.getY()))
                                .orElse(null),
                        null);
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
        ActionType enemyPrevAction = enemyPreviousTurnAction(turnContainer, enemy);
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
        int lifeAdvantage = enemy.getLife() - self.getLife();
        int lifeAdvantageForwardSteps = Math.min(maxForwardSteps,
                (lifeAdvantage / game.getMagicMissileDirectDamage()) * LIFE_ADVANTAGE_FORWARD_STEPS);
        int enemyPreviousTurnForwardSteps = enemyPrevAction == ActionType.RETREAT ?
                -ENEMY_PREV_TURN_FORWARD_STEPS :
                enemyPrevAction == ActionType.PUSH ? ENEMY_PREV_TURN_FORWARD_STEPS : 0;
        TeamAdvantageService teamAdvantageService = turnContainer.getTeamAdvantageService();
        int enemyTeamAdvangateForwardSteps = 0;
        if (teamAdvantageService.getHealthAlly() / TEAM_HEALTH_ADVANTAGE_RATIO >
                teamAdvantageService.getHealthEnemy()) {
            enemyTeamAdvangateForwardSteps = -DEFAULT_ENEMY_FORWARD_STEPS_WITH_TEAM_ADVANTAGE;
        } else if (teamAdvantageService.getHealthEnemy() / TEAM_HEALTH_ADVANTAGE_RATIO >
                teamAdvantageService.getHealthAlly()) {
            enemyTeamAdvangateForwardSteps = DEFAULT_ENEMY_FORWARD_STEPS_WITH_TEAM_ADVANTAGE;
        }
        if (untilProjectileCast >= untilSameOrBetterProjectileCast) {
            return lifeAdvantageForwardSteps + enemyPreviousTurnForwardSteps + enemyTeamAdvangateForwardSteps;
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
            return DEFAULT_ENEMY_FORWARD_STEPS_WITH_COOLDOWN_ADVANTAGE + lifeAdvantageForwardSteps +
                    enemyPreviousTurnForwardSteps + enemyTeamAdvangateForwardSteps;
        }
    }

    private static boolean hasEnemyInUnignorableRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, IGNORE_RANGE);
    }

    private ActionType enemyPreviousTurnAction(TurnContainer turnContainer, WizardProxy enemy) {
        WizardProxy self = turnContainer.getSelf();
        Memory memory = turnContainer.getMemory();
        Point selfPrevious = memory.getWizardPreviousPosition().get(self.getId());
        Point enemyPrevious = memory.getWizardPreviousPosition().get(enemy.getId());
        if (selfPrevious == null || enemyPrevious == null) {
            return ActionType.STAY;
        }
        double distDiff =
                Math.hypot(enemyPrevious.getX() - selfPrevious.getX(), enemyPrevious.getY() - selfPrevious.getY()) -
                        enemy.getDistanceTo(selfPrevious.getX(), selfPrevious.getY());
        if (distDiff > ENEMY_INTENTIONAL_MOVE_THRESHOLD) {
            return ActionType.PUSH;
        } else if (distDiff < -ENEMY_INTENTIONAL_MOVE_THRESHOLD) {
            return ActionType.RETREAT;
        } else {
            return ActionType.STAY;
        }
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

    private Optional<Point> goHam(TurnContainer turnContainer) {
        TeamAdvantageService teamAdvantageService = turnContainer.getTeamAdvantageService();
        WizardProxy self = turnContainer.getSelf();
        WorldProxy world = turnContainer.getWorldProxy();
        if (!turnContainer.getGame().isRawMessagesEnabled()) {
            return Optional.empty();
        }
        MapUtils mapUtils = turnContainer.getMapUtils();
        if (mapUtils.enemyBaseInfluenceDist(self.getX(), self.getY()) > mapUtils.getWaypointBaseInfluence() ||
                self.getShieldedLifeRatio(turnContainer.getGame()) < GO_HAM_LIFE_RATIO_THRESHOLD) {
            return Optional.empty();
        }
        if (!(teamAdvantageService.getHealthAlly() / TEAM_HEALTH_BIG_ADVANTAGE_RATIO >
                teamAdvantageService.getHealthEnemy() &&
                teamAdvantageService.getLevelAlly() / TEAM_LEVEL_ADVANTAGE_RATIO >
                        teamAdvantageService.getLevelEnemy())) {
            return Optional.empty();
        }

        Building enemyBase = null;
        for (Building building : world.getBuildings()) {
            if (building.getType() == BuildingType.FACTION_BASE && building.getFaction() != self.getFaction()) {
                enemyBase = building;
            }
        }
        if (enemyBase == null) {
            throw new RuntimeException("If enemy doesn't have base, then game is ended");
        }

        if (self.getX() >= enemyBase.getX() - enemyBase.getRadius() &&
                self.getY() <= enemyBase.getY() + enemyBase.getRadius()) {
            return Optional.empty();
        }

        int allyWizardsReadyToGo = 0;
        for (WizardProxy wizard : world.getWizards()) {
            if (wizard.getFaction() != self.getFaction()) {
                continue;
            }
            if (wizard.getDistanceTo(enemyBase) <= enemyBase.getAttackRange() &&
                    wizard.getShieldedLifeRatio(turnContainer.getGame()) >= GO_HAM_LIFE_RATIO_THRESHOLD) {
                allyWizardsReadyToGo++;
            }
        }
        if (allyWizardsReadyToGo >= GO_HAM_WIZARDS_THRESHOLD) {

            return Optional.of(new Point(world.getWidth(), 0));
        }
        return Optional.empty();
    }

    private static Point enemyUnitPushPoint(TurnContainer turnContainer, Unit unit) {
        if (unit instanceof Building) {
            Building building = (Building) unit;
            if (building.getType() == BuildingType.FACTION_BASE) {
                return MathMethods.distPoint(building.getX(),
                        building.getY(),
                        turnContainer.getWorldProxy().getWidth(),
                        0,
                        building.getRadius() / 2);
            }
        }
        return new Point(unit.getX(), unit.getY());
    }

    public static class Action {

        private final ActionType actionType;
        private final Integer duration;
        private final Point customPushPoint;
        private final Point customRetreatPoint;

        public Action(ActionType actionType) {
            this(actionType, null);
        }

        public Action(ActionType actionType, Integer duration) {
            this(actionType, duration, null, null);
        }

        public Action(ActionType actionType, Integer duration, Point customPushPoint, Point customRetreatPoint) {
            this.actionType = actionType;
            this.duration = duration;
            this.customPushPoint = customPushPoint;
            this.customRetreatPoint = customRetreatPoint;
        }

        public ActionType getActionType() {
            return actionType;
        }

        public Integer getDuration() {
            return duration;
        }

        public Point getCustomPushPoint() {
            return customPushPoint;
        }

        public Point getCustomRetreatPoint() {
            return customRetreatPoint;
        }
    }

    public enum ActionType {
        RETREAT,
        PUSH,
        STAY,
        NONE
    }
}
