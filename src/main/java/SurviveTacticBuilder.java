import model.Building;
import model.ProjectileType;

import java.util.Optional;

public class SurviveTacticBuilder implements TacticBuilder {

    private static final double LIFE_HAZZARD_THRESHOLD = 0.5;
    private static final double LIFE_HAZZARD_THRESHOLD_BONUS_HUNTING = 0.8;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        WizardProxy self = turnContainer.getSelf();
        Action towerAction = shouldRunFromTower(turnContainer);
        Action wizardsAction = shouldRunFromWizards(turnContainer);
        if (towerAction == Action.RUN || wizardsAction == Action.RUN) {
            turnContainer.getMemory().setExpectedPushDuration(0);
            Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane, turnContainer.getMemory());
            Movement mov = turnContainer.getPathFinder()
                    .findPath(self, retreatWaypoint.getX(), retreatWaypoint.getY(), 0, false);
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(mov.getSpeed());
            moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
            moveBuilder.setTurn(mov.getTurn());
            return buildTactic(moveBuilder);
        } else if ((towerAction == Action.STAY || wizardsAction == Action.STAY) &&
                PushLaneTacticBuilder.actionBecauseOfMinions(turnContainer).getActionType() !=
                        PushLaneTacticBuilder.ActionType.RETREAT) {
            turnContainer.getMemory().setExpectedPushDuration(0);
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(0);
            moveBuilder.setStrafeSpeed(0);
            return buildTactic(moveBuilder);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Tactic> buildTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("Survive", moveBuilder, Tactics.SURVIVE_TACTIC_PRIORITY));
    }

    private Action shouldRunFromTower(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Action action = Action.NONE;
        for (Building unit : turnContainer.getWorldProxy().getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(unit) || self.getLife() > unit.getDamage()) {
                continue;
            }
            double dist = self.getDistanceTo(unit);
            if (dist <= unit.getAttackRange() + self.getRadius()) {
                return Action.RUN;
            } else if (dist - self.getWizardForwardSpeed(turnContainer.getGame()) <=
                    unit.getAttackRange() + self.getRadius()) {
                action = Action.STAY;
            }
        }
        return action;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private Action shouldRunFromWizards(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        if ((double) self.getLife() / self.getMaxLife() >= getLifeHazzardThreshold(turnContainer)) {
            return Action.NONE;
        }
        int counter = 0;
        int stepInCounter = 0;
        double enemyLife = 0;
        double stepInEnemyLife = 0;
        for (WizardProxy wizard : turnContainer.getWorldProxy().getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(wizard);
            if (inDangerousRangeToAnyProjectiles(turnContainer, wizard, dist)) {
                enemyLife = wizard.getLife();
                counter++;
            }
            if (inDangerousRangeToAnyProjectiles(turnContainer,
                    wizard,
                    dist - self.getWizardForwardSpeed(turnContainer.getGame()))) {
                stepInEnemyLife = wizard.getLife();
                stepInCounter++;
            }
        }
        boolean loosingTrade = loosingTrade(turnContainer, enemyLife);
        boolean stepInLoosingTrade = loosingTrade(turnContainer, stepInEnemyLife);

        if (counter > 1 || (counter == 1 && loosingTrade)) {
            return Action.RUN;
        } else if (stepInCounter > 1 || (stepInCounter == 1 && stepInLoosingTrade)) {
            return Action.STAY;
        } else {
            return Action.NONE;
        }
    }

    private boolean loosingTrade(TurnContainer turnContainer, double enemyLife) {
        WizardProxy self = turnContainer.getSelf();
        if (turnContainer.getMemory().isWentForBonusPrevTurn() &&
                (double) self.getLife() / self.getMaxLife() > LIFE_HAZZARD_THRESHOLD) {
            return enemyLife - self.getLife() > turnContainer.getGame().getMagicMissileDirectDamage();
        } else {
            return self.getLife() < enemyLife;
        }
    }

    private double getLifeHazzardThreshold(TurnContainer turnContainer) {
        if (turnContainer.getMemory().isWentForBonusPrevTurn()) {
            return LIFE_HAZZARD_THRESHOLD_BONUS_HUNTING;
        } else {
            return LIFE_HAZZARD_THRESHOLD;
        }
    }

    @SuppressWarnings("IfStatementWithIdenticalBranches")
    private boolean inDangerousRangeToAnyProjectiles(TurnContainer turnContainer, WizardProxy enemy, double dist) {
        WizardProxy self = turnContainer.getSelf();
        CastRangeService castRangeService = turnContainer.getCastRangeService();
        if (dist <= castRangeService.castRangeToWizardOptimistic(enemy,
                self,
                turnContainer.getGame(),
                ProjectileType.MAGIC_MISSILE).getDistToCenter() + self.getRadius()) {
            return true;
        } else if (CastProjectileTacticBuilders.isProjectileLearned(turnContainer, enemy, ProjectileType.FROST_BOLT) &&
                dist <= castRangeService.castRangeToWizardOptimistic(enemy,
                        self,
                        turnContainer.getGame(),
                        ProjectileType.FROST_BOLT).getDistToCenter() + self.getRadius()) {
            return true;
        } else if (CastProjectileTacticBuilders.isProjectileLearned(turnContainer, enemy, ProjectileType.FIREBALL) &&
                dist <= castRangeService.castRangeToWizardOptimistic(enemy,
                        self,
                        turnContainer.getGame(),
                        ProjectileType.FIREBALL).getDistToCenter() + self.getRadius()) {
            return true;
        }
        return false;
    }

    private enum Action {
        STAY,
        RUN,
        NONE,
    }
}
