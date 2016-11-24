import model.Building;
import model.StatusType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memory {
    private LocationType lane;
    private List<Building> allyGuardianTowers;
    private List<Building> destroyedEnemyGuardianTowers;
    private boolean isTopBonusTaken;
    private boolean isBottomBonusTaken;
    private Map<Long, Map<StatusType, Integer>> pastBonusCooldowns;
    private Map<Long, Point> wizardPreviousPosition;
    private Map<Long, ProjectileControl.ProjectileMeta> projectileMeta;
    private boolean wentForBonusPrevTurn;


    public Memory() {
        this.allyGuardianTowers = new ArrayList<>();
        this.destroyedEnemyGuardianTowers = new ArrayList<>();
        this.isTopBonusTaken = true;
        this.isBottomBonusTaken = true;
        this.pastBonusCooldowns = new HashMap<>();
        this.wizardPreviousPosition = new HashMap<>();
        this.projectileMeta = new HashMap<>();
        this.wentForBonusPrevTurn = false;
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

    public boolean isTopBonusTaken() {
        return isTopBonusTaken;
    }

    public void setTopBonusTaken(boolean topBonusTaken) {
        isTopBonusTaken = topBonusTaken;
    }

    public boolean isBottomBonusTaken() {
        return isBottomBonusTaken;
    }

    public void setBottomBonusTaken(boolean bottomBonusTaken) {
        isBottomBonusTaken = bottomBonusTaken;
    }

    public Map<Long, Map<StatusType, Integer>> getPastBonusCooldowns() {
        return pastBonusCooldowns;
    }

    public Map<Long, Point> getWizardPreviousPosition() {
        return wizardPreviousPosition;
    }

    public Map<Long, ProjectileControl.ProjectileMeta> getProjectileMeta() {
        return projectileMeta;
    }

    public boolean isWentForBonusPrevTurn() {
        return wentForBonusPrevTurn;
    }

    public void setWentForBonusPrevTurn(boolean wentForBonusPrevTurn) {
        this.wentForBonusPrevTurn = wentForBonusPrevTurn;
    }
}
