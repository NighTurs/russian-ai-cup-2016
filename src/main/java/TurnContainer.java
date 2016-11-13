import model.Game;
import model.Wizard;
import model.World;

public class TurnContainer {

    private final Wizard self;
    private final World world;
    private final Game game;
    private final WorldProxy worldProxy;
    private final UnitLocationType unitLocationType;
    private final PathFinder pathFinder;

    public TurnContainer(Wizard self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.worldProxy = new WorldProxy(world);
        this.unitLocationType = new UnitLocationType(worldProxy, game);
        this.pathFinder = new PathFinder(self, worldProxy, game, unitLocationType);
    }

    public Wizard getSelf() {
        return self;
    }

    public World getWorld() {
        return world;
    }

    public Game getGame() {
        return game;
    }

    public WorldProxy getWorldProxy() {
        return worldProxy;
    }

    public UnitLocationType getUnitLocationType() {
        return unitLocationType;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }
}
