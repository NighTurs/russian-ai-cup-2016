import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PushLaneTacticBuilder implements TacticBuilder {

    private static final int TOWER_TARGETS_THRESHOLD = 1;
    private static final int COOLDOWN_THRESHOLD_TO_STAY_IN_WIZARD_RANGE = 20;
    private static final double TOWER_DANGER_LIFE_RATIO_THRESHOLD = 0.6;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        Wizard self = turnContainer.getSelf();

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

    private Action actionBecauseOfMinions(TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();
        double minTriggerTargetDist = Double.MAX_VALUE;
        for (Minion minion : turnContainer.getWorldProxy().getMinions()) {
            if (!turnContainer.isOffensiveUnit(minion)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(minion);
            if (dist > self.getVisionRange()) {
                continue;
            }
            double minDist = Double.MAX_VALUE;
            for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
                if (turnContainer.isAllyUnit(unit) && minion.getDistanceTo(unit) < minDist) {
                    minDist = minion.getDistanceTo(unit);
                }
            }
            if (minion.getDistanceTo(self) < minDist) {
                return Action.RETREAT;
            }
            if (minTriggerTargetDist > minion.getDistanceTo(self) - minDist) {
                minTriggerTargetDist = minion.getDistanceTo(self) - minDist;
            }
        }
        if (minTriggerTargetDist < WizardTraits.getWizardForwardSpeed(self, turnContainer.getGame())) {
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
            if (dist <= unit.getAttackRange() + turnContainer.getSelf().getRadius()) {
                buildings.add(unit);
            }
            if (dist <= unit.getAttackRange() + turnContainer.getSelf().getRadius() +
                    WizardTraits.getWizardForwardSpeed(turnContainer.getSelf(), turnContainer.getGame())) {
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
        int c = 0;
        for (Unit unit : turnContainer.getWorldProxy().allUnitsWoTrees()) {
            if (turnContainer.isAllyUnit(unit) &&
                    building.getDistanceTo(unit) <= building.getAttackRange() + ((CircularUnit) unit).getRadius()) {
                c++;
            }
        }
        if ((c <= TOWER_TARGETS_THRESHOLD || turnContainer.getSelf().getLife() / turnContainer.getSelf().getMaxLife() <
                TOWER_DANGER_LIFE_RATIO_THRESHOLD) && building.getRemainingActionCooldownTicks() <
                turnContainer.getGame().getGuardianTowerCooldownTicks() / 2) {
            return Action.RETREAT;
        }
        return Action.NONE;
    }

    private Action actionBecauseOfWizards(TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();
        Game game = turnContainer.getGame();
        double minDistToWizard = Double.MAX_VALUE;
        Wizard minDistWizard = null;
        int enemyWizardCount = 0;
        for (Wizard wizard : turnContainer.getWorldProxy().getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard) || self.getDistanceTo(wizard) > self.getVisionRange()) {
                continue;
            }
            enemyWizardCount++;
            double dist = turnContainer.getSelf().getDistanceTo(wizard);
            if (minDistToWizard > dist) {
                minDistToWizard = dist;
                minDistWizard = wizard;
            }
        }
        if (minDistWizard == null) {
            return Action.NONE;
        }

        if (enemyWizardCount == 1 &&
                minDistWizard.getLife() + WizardTraits.getMagicMissileDirectDamage(minDistWizard, game) * 3 <
                        self.getLife()) {
            return Action.PUSH;
        }

        double untilNextMissile = Math.max(self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()],
                self.getRemainingActionCooldownTicks());
        double enemyCastRange =
                CastMagicMissileTacticBuilder.castRangeToWizard(minDistWizard, self, turnContainer.getGame());

        if (minDistToWizard <= enemyCastRange + minDistWizard.getRadius() &&
                untilNextMissile > COOLDOWN_THRESHOLD_TO_STAY_IN_WIZARD_RANGE) {
            return Action.RETREAT;
        }
        if (minDistToWizard > enemyCastRange && untilNextMissile <= COOLDOWN_THRESHOLD_TO_STAY_IN_WIZARD_RANGE) {
            return Action.PUSH;
        } else if (untilNextMissile <= COOLDOWN_THRESHOLD_TO_STAY_IN_WIZARD_RANGE &&
                minDistToWizard >= enemyCastRange - WizardTraits.getWizardBackwardSpeed(self, game)) {
            return Action.STAY;
        } else if (untilNextMissile <= COOLDOWN_THRESHOLD_TO_STAY_IN_WIZARD_RANGE &&
                minDistToWizard < enemyCastRange - WizardTraits.getWizardBackwardSpeed(self, game)) {
            return Action.RETREAT;
        } else {
            return Action.NONE;
        }
    }

    private static boolean hasEnemyInVisibilityRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer, turnContainer.getSelf().getVisionRange());
    }

    public static boolean hasEnemyInAttackRange(TurnContainer turnContainer) {
        return hasEnemyInRange(turnContainer,
                WizardTraits.getWizardCastRange(turnContainer.getSelf(), turnContainer.getGame()));
    }

    private static boolean hasEnemyInRange(TurnContainer turnContainer, double range) {
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
        STAY,
        NONE
    }
}
