import model.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TurnContainer {

    private static final Set<String> ANTMSU_LIKE = new HashSet<>(Collections.singletonList("Antmsu"));
    private static final Set<String> SIDE_PUSH_MASTERS = new HashSet<>(Collections.singletonList("core2duo"));

    private final WizardProxy self;
    private final World world;
    private final Game game;
    private final WorldProxy worldProxy;
    private final MapUtils mapUtils;
    private final PathFinder pathFinder;
    private final LanePicker lanePicker;
    private final BonusControl bonusControl;
    private final ProjectileControl projectileControl;
    private final BuildingControl buildingControl;
    private final CastRangeService castRangeService;
    private final WizardControl wizardControl;
    private final TeamAdvantageService teamAdvantageService;
    private final Memory memory;

    public TurnContainer(Wizard self, World world, Game game, Memory memory) {
        this.self = WizardProxy.wizardProxy(self, world, game, memory);
        this.world = world;
        this.game = game;
        this.memory = memory;
        this.buildingControl = new BuildingControl(self, memory, world, game);
        this.wizardControl = new WizardControl(self, memory, world, game);
        this.worldProxy = new WorldProxy(world, self, wizardControl, buildingControl, game, memory);
        this.mapUtils = new MapUtils(worldProxy);
        this.pathFinder = new PathFinder(this.self, worldProxy, game, mapUtils);
        this.lanePicker = new LanePicker(worldProxy, this.self, game, mapUtils, this.memory);
        this.bonusControl = new BonusControl(this.self, worldProxy, game, memory);
        this.projectileControl = new ProjectileControl(worldProxy, game, memory);
        if (memory.getCastRangeService() == null) {
            memory.setCastRangeService(new CastRangeService(worldProxy, game));
        }
        this.castRangeService = memory.getCastRangeService();
        this.teamAdvantageService = new TeamAdvantageService(this.self, worldProxy, this.game);
    }

    public WizardProxy getSelf() {
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

    public CastRangeService getCastRangeService() {
        return castRangeService;
    }

    public TeamAdvantageService getTeamAdvantageService() {
        return teamAdvantageService;
    }

    public boolean isOffensiveMinion(Minion minion) {
        return minion.getFaction() == opposingFaction() || (minion.getFaction() == Faction.NEUTRAL &&
                (Math.abs(minion.getSpeedX()) + Math.abs(minion.getSpeedY()) != 0));
    }

    public boolean isOffensiveBuilding(Building building) {
        return building.getFaction() == opposingFaction();
    }

    public boolean isOffensiveWizard(WizardProxy wizard) {
        return wizard.getFaction() == opposingFaction();
    }

    public boolean isAllyMinion(Minion minion) {
        return minion.getFaction() == self.getFaction();
    }

    public boolean isAllyBuilding(Building building) {
        return building.getFaction() == self.getFaction();
    }

    public boolean isAllyWizard(WizardProxy wizard) {
        return wizard.getFaction() == self.getFaction() && wizard.getId() != self.getId();
    }

    public boolean isOffensiveUnit(Unit unit) {
        return (unit instanceof Building && isOffensiveBuilding((Building) unit)) ||
                (unit instanceof Minion && isOffensiveMinion(((Minion) unit))) ||
                (unit instanceof WizardProxy && isOffensiveWizard(((WizardProxy) unit)));
    }

    public boolean isAllyUnit(Unit unit) {
        return (unit instanceof Building && isAllyBuilding((Building) unit)) ||
                (unit instanceof Minion && isAllyMinion(((Minion) unit))) ||
                (unit instanceof WizardProxy && isAllyWizard((WizardProxy) unit));
    }

    public Faction opposingFaction() {
        return self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
    }

    public void postTurn() {
        for (WizardProxy wizard : worldProxy.getWizards()) {
            memory.getWizardPreviousPosition().put(wizard.getId(), new Point(wizard.getX(), wizard.getY()));
        }
        wizardControl.updateEnemyDominantLocations(self, memory, worldProxy, game, mapUtils);
    }

    public boolean againstSidePushMaster() {
        for (Player player : worldProxy.getPlayers()) {
            if (SIDE_PUSH_MASTERS.contains(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean againstAntmsuLike() {
        for (Player player : worldProxy.getPlayers()) {
            if (ANTMSU_LIKE.contains(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isMidEnemyTowerDestroyed() {
        for (Building building : memory.getDestroyedEnemyGuardianTowers()) {
            if (BuildingControl.isMidEnemyTower(this, building)) {
                return true;
            }
        }
        return false;
    }
}
