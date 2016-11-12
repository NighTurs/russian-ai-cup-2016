import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public final class WorldProxy {

    private final World world;

    public WorldProxy(World world) {
        this.world = world;
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
        return Arrays.asList(world.getPlayers());
    }

    /**
     * @return Возвращает список видимых волшебников (в случайном порядке).
     * После каждого тика объекты, задающие волшебников, пересоздаются.
     */
    public List<Wizard> getWizards() {
        return Arrays.asList(world.getWizards());
    }

    /**
     * @return Возвращает список видимых последователей (в случайном порядке).
     * После каждого тика объекты, задающие последователей, пересоздаются.
     */
    public List<Minion> getMinions() {
        return Arrays.asList(world.getMinions());
    }

    /**
     * @return Возвращает список видимых магических снарядов (в случайном порядке).
     * После каждого тика объекты, задающие снаряды, пересоздаются.
     */
    public List<Projectile> getProjectiles() {
        return Arrays.asList(world.getProjectiles());
    }

    /**
     * @return Возвращает список видимых бонусов (в случайном порядке).
     * После каждого тика объекты, задающие бонусы, пересоздаются.
     */
    public List<Bonus> getBonuses() {
        return Arrays.asList(world.getBonuses());
    }

    /**
     * @return Возвращает список видимых строений (в случайном порядке).
     * После каждого тика объекты, задающие строения, пересоздаются.
     */
    public List<Building> getBuildings() {
        return Arrays.asList(world.getBuildings());
    }

    /**
     * @return Возвращает список видимых деревьев (в случайном порядке).
     * После каждого тика объекты, задающие деревья, пересоздаются.
     */
    public List<Tree> getTrees() {
        return Arrays.asList(world.getTrees());
    }

    /**
     * @return Возвращает вашего игрока.
     */
    public Player getMyPlayer() {
        return world.getMyPlayer();
    }

    public List<Unit> allUnits() {
        List<Unit> units = new ArrayList<>();
        units.addAll(getWizards());
        units.addAll(getMinions());
        units.addAll(getProjectiles());
        units.addAll(getBonuses());
        units.addAll(getBuildings());
        units.addAll(getTrees());
        return units;
    }

    public Building allyBase() {
        Player myPlayer = getMyPlayer();
        for (Building building : getBuildings()) {
            if (building.getType() == BuildingType.FACTION_BASE && building.getFaction() == myPlayer.getFaction()) {
                return building;
            }
        }
        throw new RuntimeException("Ally base not found");
    }
}
