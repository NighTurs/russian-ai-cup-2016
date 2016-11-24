import model.*;

public class TurnContainer {

    private final Wizard self;
    private final World world;
    private final Game game;
    private final WorldProxy worldProxy;
    private final MapUtils mapUtils;
    private final PathFinder pathFinder;
    private final LanePicker lanePicker;
    private final BonusControl bonusControl;
    private final ProjectileControl projectileControl;
    private final Memory memory;

    public TurnContainer(Wizard self, World world, Game game, Memory memory) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.memory = memory;
        this.worldProxy = new WorldProxy(world, memory);
        this.mapUtils = new MapUtils(worldProxy);
        this.pathFinder = new PathFinder(self, worldProxy, game, mapUtils);
        this.lanePicker = new LanePicker(worldProxy, self, mapUtils, this.memory);
        this.bonusControl = new BonusControl(self, worldProxy, game, memory);
        this.projectileControl = new ProjectileControl(worldProxy, game, memory);
    }

    public Wizard getSelf() {
        return self;
    }

    public Game getGame() {
        return game;
    }

    public WorldProxy getWorldProxy() {
        return worldProxy;
    }

    public MapUtils getMapUtils() {
        return mapUtils;
    }

    public PathFinder getPathFinder() {
        return pathFinder;
    }

    public LanePicker getLanePicker() {
        return lanePicker;
    }

    public BonusControl getBonusControl() {
        return bonusControl;
    }

    public ProjectileControl getProjectileControl() {
        return projectileControl;
    }

    public Memory getMemory() {
        return memory;
    }

    public boolean isOffensiveMinion(Minion minion) {
        return minion.getFaction() == opposingFaction() || (minion.getFaction() == Faction.NEUTRAL &&
                (Math.abs(minion.getSpeedX()) + Math.abs(minion.getSpeedY()) != 0));
    }

    public boolean isOffensiveBuilding(Building building) {
        return building.getFaction() == opposingFaction();
    }

    public boolean isOffensiveWizard(Wizard wizard) {
        return wizard.getFaction() == opposingFaction();
    }

    public boolean isAllyMinion(Minion minion) {
        return minion.getFaction() == self.getFaction();
    }

    public boolean isAllyBuilding(Building building) {
        return building.getFaction() == self.getFaction();
    }

    public boolean isAllyWizard(Wizard wizard) {
        return wizard.getFaction() == self.getFaction() && wizard.getId() != self.getId();
    }

    public boolean isOffensiveUnit(Unit unit) {
        return (unit instanceof Building && isOffensiveBuilding((Building) unit)) ||
                (unit instanceof Minion && isOffensiveMinion(((Minion) unit))) ||
                (unit instanceof Wizard && isOffensiveWizard(((Wizard) unit)));
    }

    public boolean isAllyUnit(Unit unit) {
        return (unit instanceof Building && isAllyBuilding((Building) unit)) ||
                (unit instanceof Minion && isAllyMinion(((Minion) unit))) ||
                (unit instanceof Wizard && isAllyWizard((Wizard) unit));
    }

    public Faction opposingFaction() {
        return self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
    }
}
