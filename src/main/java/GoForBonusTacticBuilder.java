import model.Bonus;
import model.Building;
import model.BuildingType;
import model.Game;

import java.util.Optional;

public class GoForBonusTacticBuilder implements TacticBuilder {

    private static final int ALLY_HALF_MANHATTAN_BASES = 9;
    private static final int ARRIVE_BEFORE_TICKS = 80;
    private static final int EXPECTED_TICKS_TO_BONUS_ERROR = 400;
    private static final int ACCEPTABLE_TICKS_TO_TAKE_BONUS = 1000;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        BonusControl bonusControl = turnContainer.getBonusControl();
        WizardProxy self = turnContainer.getSelf();
        PathFinder pathFinder = turnContainer.getPathFinder();
        WorldProxy world = turnContainer.getWorldProxy();
        Game game = turnContainer.getGame();

        if (self.getX() + world.getHeight() - self.getY() <=
                turnContainer.getWorldProxy().allyBase().getX() * ALLY_HALF_MANHATTAN_BASES) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }

        Point topBonus = bonusControl.topBonusPosition();
        Point bottomBonus = bonusControl.bottomBonusPosition();
        Point goForBonus;
        double ticksUntilBonus;
        if (shouldGoForBottomBonus(turnContainer)) {
            goForBonus = bottomBonus;
            ticksUntilBonus = bonusControl.ticksUntilBottomBonus();
        } else {
            goForBonus = topBonus;
            ticksUntilBonus = bonusControl.ticksUntilTopBonus();
        }

        double ticksToBonus =
                roughDistToBonus(self, pathFinder, goForBonus) / self.getWizardForwardSpeed(turnContainer.getGame()) +
                        ARRIVE_BEFORE_TICKS;
        if (game.isSkillsEnabled() && ticksToBonus * 2 > ACCEPTABLE_TICKS_TO_TAKE_BONUS) {
            return Optional.empty();
        }
        if (ticksToBonus < ticksUntilBonus && (!turnContainer.getMemory().isWentForBonusPrevTurn() ||
                ticksToBonus + EXPECTED_TICKS_TO_BONUS_ERROR < ticksUntilBonus)) {
            turnContainer.getMemory().setWentForBonusPrevTurn(false);
            return Optional.empty();
        }
        turnContainer.getMemory().setWentForBonusPrevTurn(true);

        boolean haveBonusInVisibilityRange = false;
        for (Bonus bonus : world.getBonuses()) {
            if (bonus.getDistanceTo(self) <= self.getVisionRange()) {
                haveBonusInVisibilityRange = true;
            }
        }
        if (haveBonusInVisibilityRange || self.getDistanceTo(goForBonus.getX(), goForBonus.getY()) >
                game.getBonusRadius() + self.getRadius() + self.getWizardForwardSpeed(game)) {
            Movement mov = pathFinder.findPath(self, goForBonus.getX(), goForBonus.getY());
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(mov.getSpeed());
            moveBuilder.setStrafeSpeed(mov.getStrafeSpeed());
            if (!PushLaneTacticBuilder.hasEnemyInAttackRange(turnContainer)) {
                moveBuilder.setTurn(mov.getTurn());
            }
            return tactic(moveBuilder);
        } else {
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setSpeed(0);
            moveBuilder.setStrafeSpeed(0);
            return tactic(moveBuilder);
        }
    }

    private Optional<Tactic> tactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("GoForBonus", moveBuilder, Tactics.GO_FOR_BONUS_TACTIC_PRIORITY));
    }

    private double roughDistToBonus(WizardProxy self, PathFinder pathFinder, Point goForBonus) {
        return pathFinder.roughDistanceTo(self, goForBonus.getX(), goForBonus.getY());
    }

    private boolean shouldGoForBottomBonus(TurnContainer turnContainer) {
        WizardProxy self = turnContainer.getSelf();
        Point topBonus = turnContainer.getBonusControl().topBonusPosition();
        Point bottomBonus = turnContainer.getBonusControl().bottomBonusPosition();
        // Going for top bonus on middle is dangerous if middle enemy tower is up
        if (turnContainer.getLanePicker().myLane() == LocationType.MIDDLE_LANE) {
            double unit = turnContainer.getWorldProxy().allyBase().getX();
            for (Building building : turnContainer.getWorldProxy().getBuildings()) {
                if (building.getType() == BuildingType.GUARDIAN_TOWER &&
                        building.getFaction() == turnContainer.opposingFaction() && building.getX() >= unit * 4 &&
                        building.getX() <= unit * 6 && building.getY() >= unit * 3 && building.getY() <= unit * 5) {
                    return true;
                }
            }
        }
        return self.getDistanceTo(topBonus.getX(), topBonus.getY()) >
                self.getDistanceTo(bottomBonus.getX(), bottomBonus.getY());
    }
}
