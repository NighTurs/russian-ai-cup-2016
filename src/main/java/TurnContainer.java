import model.*;

public class TurnContainer {

    private final Wizard self;
    private final World world;
    private final Game game;
    private final WorldProxy worldProxy;
    private final UnitLocationType unitLocationType;
    private final PathFinder pathFinder;
    private final LanePicker lanePicker;

    public TurnContainer(Wizard self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.worldProxy = new WorldProxy(world);
        this.unitLocationType = new UnitLocationType(worldProxy, game);
        this.pathFinder = new PathFinder(self, worldProxy, game, unitLocationType);
        this.lanePicker = new LanePicker(worldProxy, self, unitLocationType);
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

    public LanePicker getLanePicker() {
        return lanePicker;
    }

    public boolean isOffensiveMinion(Minion minion) {
        return minion.getFaction() == opposingFaction() || (minion.getFaction() == Faction.NEUTRAL &&
                (Math.abs(minion.getSpeedX()) + Math.abs(minion.getSpeedY()) != 0));
    }

    public boolean isOffensiveBuilding(Building building) {
        return building.getFaction() == opposingFaction();
    }

    public boolean isOffensiveUnit(Unit unit) {
        return (unit instanceof Building && isOffensiveBuilding((Building) unit)) ||
                (unit instanceof Minion && isOffensiveMinion(((Minion) unit)));
    }

    public Faction opposingFaction() {
        return self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
    }
}
