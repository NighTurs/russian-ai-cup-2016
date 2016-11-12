import model.*;

public final class MyStrategy implements Strategy {
    WorldProxy worldProxy;
    Game game;
    UnitLocationTypeStore unitLocationTypeStore;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        this.game = game;
        this.worldProxy = new WorldProxy(world);
        this.unitLocationTypeStore = new UnitLocationTypeStore(worldProxy, game);

        move.setSpeed(game.getWizardForwardSpeed());
        move.setStrafeSpeed(game.getWizardStrafeSpeed());
        move.setTurn(game.getWizardMaxTurnAngle());
        move.setAction(ActionType.MAGIC_MISSILE);
    }
}
