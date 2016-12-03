import model.Game;
import model.Move;
import model.Wizard;
import model.World;

public final class MyStrategy implements Strategy {
    private Memory memory = new Memory();
    TurnContainer turnContainer;

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        this.turnContainer = new TurnContainer(self, world, game, memory);

        new TurnStrategy(turnContainer, move).findStrategy();
    }
}
