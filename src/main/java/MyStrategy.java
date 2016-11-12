import model.*;

public final class MyStrategy implements Strategy {
    WorldProxy worldProxy;
    Game game;
    UnitLocationType unitLocationType;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        this.game = game;
        this.worldProxy = new WorldProxy(world);
        this.unitLocationType = new UnitLocationType(worldProxy, game);

        move.setSpeed(game.getWizardForwardSpeed());
        move.setStrafeSpeed(game.getWizardStrafeSpeed());
        move.setTurn(game.getWizardMaxTurnAngle());
        move.setAction(ActionType.MAGIC_MISSILE);
    }
}
