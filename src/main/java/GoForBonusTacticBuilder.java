import model.Bonus;
import model.Game;
import model.Wizard;

import java.util.Optional;

public class GoForBonusTacticBuilder implements TacticBuilder {

    private static final int ALLY_HALF_MANHATTAN_BASES = 9;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        BonusControl bonusControl = turnContainer.getBonusControl();
        Wizard self = turnContainer.getSelf();
        PathFinder pathFinder = turnContainer.getPathFinder();
        WorldProxy world = turnContainer.getWorldProxy();
        Game game = turnContainer.getGame();

        if (self.getX() + world.getHeight() - self.getY() <=
                turnContainer.getWorldProxy().allyBase().getX() * ALLY_HALF_MANHATTAN_BASES) {
            return Optional.empty();
        }

        Point topBonus = bonusControl.topBonusPosition();
        Point bottomBonus = bonusControl.bottomBonusPosition();
        Point goForBonus;
        if (self.getDistanceTo(topBonus.getX(), topBonus.getY()) >
                self.getDistanceTo(bottomBonus.getX(), bottomBonus.getY()) &&
                roughDistToBonus(self, pathFinder, bottomBonus) /
                        WizardTraits.getWizardForwardSpeed(self, turnContainer.getGame()) >=
                        bonusControl.ticksUntilBottomBonus()) {
            goForBonus = bottomBonus;
        } else if (roughDistToBonus(self, pathFinder, topBonus) /
                WizardTraits.getWizardForwardSpeed(self, turnContainer.getGame()) >=
                bonusControl.ticksUntilTopBonus()) {
            goForBonus = topBonus;
        } else if (roughDistToBonus(self, pathFinder, bottomBonus) /
                WizardTraits.getWizardForwardSpeed(self, turnContainer.getGame()) >=
                bonusControl.ticksUntilBottomBonus()) {
            goForBonus = bottomBonus;
        } else {
            return Optional.empty();
        }

        boolean haveBonusInVisibilityRange = false;
        for (Bonus bonus : world.getBonuses()) {
            if (bonus.getDistanceTo(self) <= self.getVisionRange()) {
                haveBonusInVisibilityRange = true;
            }
        }
        if (haveBonusInVisibilityRange || self.getDistanceTo(goForBonus.getX(), goForBonus.getY()) >
                game.getBonusRadius() + self.getRadius() + WizardTraits.getWizardForwardSpeed(self, game)) {
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

    private double roughDistToBonus(Wizard self, PathFinder pathFinder, Point goForBonus) {
        return pathFinder.roughDistanceTo(self, goForBonus.getX(), goForBonus.getY());
    }
}
