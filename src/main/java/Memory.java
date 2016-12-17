import model.*;

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
    private CastRangeService castRangeService;
    private Map<Long, WizardProxy> shadowWizards;
    private Message selfMessage;
    private Map<Long, Integer> allyWizardMessageIndex;
    private WizardRole assignedRole;
    private int expectedPushDuration;
    private Map<Integer, Integer> buildingCooldownByX;
    private Map<Long, Integer> lifeByLivingUnit;
    private Map<Long, Map<LocationType, Integer>> enemyDominantLocation;
    private boolean masterDidSwitch;
    private boolean laneGotSwitched;

    public Memory() {
        this.allyGuardianTowers = new ArrayList<>();
        this.destroyedEnemyGuardianTowers = new ArrayList<>();
        this.isTopBonusTaken = true;
        this.isBottomBonusTaken = true;
        this.pastBonusCooldowns = new HashMap<>();
        this.wizardPreviousPosition = new HashMap<>();
        this.projectileMeta = new HashMap<>();
        this.wentForBonusPrevTurn = false;
        this.shadowWizards = new HashMap<>();
        this.allyWizardMessageIndex = new HashMap<>();
        this.expectedPushDuration = 0;
        this.buildingCooldownByX = new HashMap<>();
        this.lifeByLivingUnit = new HashMap<>();
        this.masterDidSwitch = false;
        this.laneGotSwitched = false;
        this.enemyDominantLocation = new HashMap<>();
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

    public CastRangeService getCastRangeService() {
        return castRangeService;
    }

    public void setCastRangeService(CastRangeService castRangeService) {
        this.castRangeService = castRangeService;
    }

    public Map<Long, WizardProxy> getShadowWizards() {
        return shadowWizards;
    }

    public Message getSelfMessage() {
        return selfMessage;
    }

    public void setSelfMessage(Message selfMessage) {
        this.selfMessage = selfMessage;
    }

    public Map<Long, Integer> getAllyWizardMessageIndex() {
        return allyWizardMessageIndex;
    }

    public WizardRole getAssignedRole() {
        return assignedRole;
    }

    public void setAssignedRole(WizardRole assignedRole) {
        this.assignedRole = assignedRole;
    }

    public int getExpectedPushDuration() {
        return expectedPushDuration;
    }

    public void setExpectedPushDuration(int expectedPushDuration) {
        this.expectedPushDuration = expectedPushDuration;
    }

    public Map<Integer, Integer> getBuildingCooldownByX() {
        return buildingCooldownByX;
    }

    public Map<Long, Integer> getLifeByLivingUnit() {
        return lifeByLivingUnit;
    }

    public boolean isMasterDidSwitch() {
        return masterDidSwitch;
    }

    public void setMasterDidSwitch(boolean masterDidSwitch) {
        this.masterDidSwitch = masterDidSwitch;
    }

    public boolean isLaneGotSwitched() {
        return laneGotSwitched;
    }

    public void setLaneGotSwitched(boolean laneGotSwitched) {
        this.laneGotSwitched = laneGotSwitched;
    }

    public Map<Long, Map<LocationType, Integer>> getEnemyDominantLocation() {
        return enemyDominantLocation;
    }
}
