import model.*;

public final class MyStrategy implements Strategy {
    TurnContainer turnContainer;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        this.turnContainer = new TurnContainer(self, world, game);

        new TurnStrategy(turnContainer, move).findStrategy();
    }
}
