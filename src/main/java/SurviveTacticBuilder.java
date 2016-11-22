import model.Building;
import model.Wizard;

import java.util.Optional;

public class SurviveTacticBuilder implements TacticBuilder {

    private static final double LIFE_HAZZARD_THRESHOLD = 0.5;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        LocationType lane = turnContainer.getLanePicker().myLane();
        MapUtils mapUtils = turnContainer.getMapUtils();
        Wizard self = turnContainer.getSelf();
        if (shouldRunFromTower(turnContainer) || shouldRunFromWizards(turnContainer)) {
            Point retreatWaypoint = mapUtils.retreatWaypoint(self.getX(), self.getY(), lane);
            Movement mov = turnContainer.getPathFinder().findPath(self, retreatWaypoint.getX(), retreatWaypoint.getY());
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(mov.getSpeed());
            moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
            moveBuilder.setTurn(mov.getTurn());
            return Optional.of(new TacticImpl("Survive", moveBuilder, Tactics.SURVIVE_TACTIC_PRIORITY));
        } else {
            return Optional.empty();
        }
    }

    private boolean shouldRunFromTower(TurnContainer turnContainer) {
        Building building = null;
        Wizard self = turnContainer.getSelf();
        for (Building unit : turnContainer.getWorldProxy().getBuildings()) {
            if (!turnContainer.isOffensiveBuilding(unit)) {
                continue;
            }
            double dist = self.getDistanceTo(unit);
            if (dist <= unit.getAttackRange() + self.getRadius()) {
                building = unit;
            }
        }
        return building != null && self.getLife() <= building.getDamage();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean shouldRunFromWizards(TurnContainer turnContainer) {
        Wizard self = turnContainer.getSelf();
        if ((double) self.getLife() / self.getMaxLife() >= LIFE_HAZZARD_THRESHOLD) {
            return false;
        }
        int counter = 0;
        double enemyLife = 0;
        for (Wizard wizard : turnContainer.getWorldProxy().getWizards()) {
            if (!turnContainer.isOffensiveWizard(wizard)) {
                continue;
            }
            double dist = turnContainer.getSelf().getDistanceTo(wizard);
            if (dist <= CastMagicMissileTacticBuilder.castRangeToWizard(wizard, self, turnContainer.getGame()) +
                    self.getRadius()) {
                counter++;
                enemyLife = wizard.getLife();
            }
        }
        if (counter == 0) {
            return false;
        } else if (counter > 1) {
            return true;
        } else {
            return self.getLife() - enemyLife < turnContainer.getGame().getMagicMissileDirectDamage();
        }
    }

}
