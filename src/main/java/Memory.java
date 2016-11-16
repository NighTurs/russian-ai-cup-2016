import model.Building;

import java.util.ArrayList;
import java.util.List;

public class Memory {
    private LocationType lane;
    private List<Building> allyGuardianTowers;
    private List<Building> destroyedEnemyGuardianTowers;

    public Memory() {
        this.allyGuardianTowers = new ArrayList<>();
        this.destroyedEnemyGuardianTowers = new ArrayList<>();
    }

    public LocationType getLane() {
        return lane;
    }

    public void setLane(LocationType lane) {
        this.lane = lane;
    }

    public List<Building> getAllyGuardianTowers() {
        return allyGuardianTowers;
    }

    public List<Building> getDestroyedEnemyGuardianTowers() {
        return destroyedEnemyGuardianTowers;
    }
}
