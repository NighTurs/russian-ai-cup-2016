import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public final class WorldProxy {

    private final World world;
    private final List<Player> players;
    private final List<WizardProxy> wizards;
    private final List<Minion> minions;
    private final List<Projectile> projectiles;
    private final List<Bonus> bonuses;
    private final List<Tree> trees;
    private final List<Unit> allUnits;
    private final List<Unit> allUnitsWoTrees;
    private final List<Unit> allUnitsNearby;
    private final Building allyBase;
    private List<Building> buildings;

    public WorldProxy(World world, Wizard self, BuildingControl buildingControl, Game game) {
        this.world = world;
        this.players = unmodifiableList(world.getPlayers());
        this.wizards = unmodifiableList(world.getWizards()).stream()
                .map(x -> new WizardProxy(x, world, game))
                .collect(Collectors.toList());
        this.minions = unmodifiableList(world.getMinions());
        this.projectiles = unmodifiableList(world.getProjectiles());
        this.bonuses = unmodifiableList(world.getBonuses());
        this.buildings = buildingControl.buildingsIncludingEnemy();
        this.allyBase = allyBase(world.getBuildings(), world.getMyPlayer());

        this.trees = unmodifiableList(world.getTrees());

        List<Unit> units = new ArrayList<>();
        units.addAll(getWizards());
        units.addAll(getMinions());
        units.addAll(getProjectiles());
        units.addAll(getBonuses());
        units.addAll(getBuildings());
        this.allUnitsWoTrees = Collections.unmodifiableList(units);
        units.addAll(getTrees());
        this.allUnits = Collections.unmodifiableList(units);

        this.allUnitsNearby = Collections.unmodifiableList(units.stream()
                .filter(x -> self.getDistanceTo(x) <= self.getVisionRange())
                .collect(Collectors.toList()));
    }

    private static <T> List<T> unmodifiableList(T[] array) {
        return Collections.unmodifiableList(Arrays.asList(array));
    }

    /**
     * @return Возвращает номер текущего тика.
     */
    public int getTickIndex() {
        return world.getTickIndex();
    }

    /**
     * @return Возвращает базовую длительность игры в тиках. Реальная длительность может отличаться от этого значения в
     * меньшую сторону. Эквивалентно {@code game.tickCount}.
     */
    public int getTickCount() {
        return world.getTickCount();
    }

    /**
     * @return Возвращает ширину мира.
     */
    public double getWidth() {
        return world.getWidth();
    }

    /**
     * @return Возвращает высоту мира.
     */
    public double getHeight() {
        return world.getHeight();
    }

    /**
     * @return Возвращает список игроков (в случайном порядке).
     * После каждого тика объекты, задающие игроков, пересоздаются.
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * @return Возвращает список видимых волшебников (в случайном порядке).
     * После каждого тика объекты, задающие волшебников, пересоздаются.
     */
    public List<WizardProxy> getWizards() {
        return wizards;
    }

    /**
     * @return Возвращает список видимых последователей (в случайном порядке).
     * После каждого тика объекты, задающие последователей, пересоздаются.
     */
    public List<Minion> getMinions() {
        return minions;
    }

    /**
     * @return Возвращает список видимых магических снарядов (в случайном порядке).
     * После каждого тика объекты, задающие снаряды, пересоздаются.
     */
    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    /**
     * @return Возвращает список видимых бонусов (в случайном порядке).
     * После каждого тика объекты, задающие бонусы, пересоздаются.
     */
    public List<Bonus> getBonuses() {
        return bonuses;
    }

    /**
     * @return Возвращает список видимых строений (в случайном порядке).
     * После каждого тика объекты, задающие строения, пересоздаются.
     */
    public List<Building> getBuildings() {
        return buildings;
    }

    /**
     * @return Возвращает список видимых деревьев (в случайном порядке).
     * После каждого тика объекты, задающие деревья, пересоздаются.
     */
    public List<Tree> getTrees() {
        return trees;
    }

    /**
     * @return Возвращает вашего игрока.
     */
    public Player getMyPlayer() {
        return world.getMyPlayer();
    }

    public List<Unit> allUnits() {
        return this.allUnits;
    }

    public List<Unit> allUnitsWoTrees() {
        return this.allUnitsWoTrees;
    }

    public List<Unit> getAllUnitsNearby() {
        return this.allUnitsNearby;
    }

    public Building allyBase() {
        return this.allyBase;
    }

    public static Building allyBase(Building[] buildings, Player myPlayer) {
        for (Building building : buildings) {
            if (building.getType() == BuildingType.FACTION_BASE && building.getFaction() == myPlayer.getFaction()) {
                return building;
            }
        }
        throw new RuntimeException("Ally base not found");
    }
}
