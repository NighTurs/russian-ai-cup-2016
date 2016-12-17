import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class MasterWizardTacticBuilder implements TacticBuilder {

    private static final double RELOCATE_HP_THRESHOLD = 0.5;
    private static final Set<String> SCARED_OF = new HashSet<>(Collections.singletonList("tyamgin"));
    private static final List<LaneRole> ALL_MID = Arrays.asList(new LaneRole(LaneType.MIDDLE, WizardRole.FROST_BOLT),
            new LaneRole(LaneType.MIDDLE, WizardRole.HASTE_QUICK),
            new LaneRole(LaneType.MIDDLE, WizardRole.RANGE),
            new LaneRole(LaneType.MIDDLE, WizardRole.SHIELD_QUICK),
            new LaneRole(LaneType.MIDDLE, WizardRole.FIREBALL_TEAM));
    public static final byte ROLE_MESSAGE_CODE = 'r';
    public static final byte LANE_SWITCH_CODE = 's';
    private static final int NUMBER_OF_ALLY_WIZARDS = 4;

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        turnContainer.getMemory().setSelfMessage(null);
        if (!turnContainer.getGame().isRawMessagesEnabled() || !turnContainer.getSelf().isMaster()) {
            return Optional.empty();
        }
        WorldProxy world = turnContainer.getWorldProxy();
        Memory memory = turnContainer.getMemory();
        WizardProxy self = turnContainer.getSelf();
        List<WizardProxy> allyWizardsExceptMe = world.getWizards()
                .stream()
                .filter(x -> x.getId() != self.getId())
                .filter(x -> x.getFaction() == self.getFaction())
                .collect(Collectors.toList());

        if (world.getTickIndex() == 0) {
            allyWizardsExceptMe.stream()
                    .sorted(Comparator.comparingLong(Unit::getId))
                    .forEach(x -> memory.getAllyWizardMessageIndex()
                            .put(x.getId(), memory.getAllyWizardMessageIndex().size()));
        }

        if (world.getTickIndex() == 0) {
            MoveBuilder moveBuilder = new MoveBuilder();
            Message[] messages = new Message[NUMBER_OF_ALLY_WIZARDS];

            Queue<LaneRole> roles;
            if (inFearMode(turnContainer)) {
                roles = new ArrayDeque<>(Arrays.asList(new LaneRole(LaneType.MIDDLE, WizardRole.RANGE),
                        new LaneRole(LaneType.MIDDLE, WizardRole.HASTE_DELAYED),
                        new LaneRole(LaneType.MIDDLE, WizardRole.SHIELD_DELAYED),
                        new LaneRole(LaneType.BOTTOM, WizardRole.FIREBALL_SOLO),
                        new LaneRole(LaneType.TOP, WizardRole.FIREBALL_SOLO)));
            } else {
                roles = new ArrayDeque<>(ALL_MID);
            }
            for (WizardProxy wizard : allyWizardsExceptMe) {
                LaneRole laneRole = roles.poll();
                messages[memory.getAllyWizardMessageIndex().get(wizard.getId())] =
                        new Message(laneRole.laneType, null, roleMessage(laneRole.role));
            }
            moveBuilder.setMessages(messages);
            LaneRole laneRole = roles.poll();
            turnContainer.getMemory().setSelfMessage(new Message(laneRole.laneType, null, roleMessage(laneRole.role)));
            return buildTactic(moveBuilder);
        }
        return switchLane(turnContainer);
    }

    private Optional<Tactic> switchLane(TurnContainer turnContainer) {
        Memory memory = turnContainer.getMemory();
        boolean alreadySwitched = memory.isMasterDidSwitch();
        boolean fearSetup = inFearMode(turnContainer);
        boolean midTowerDestroyed = isMidEnemyTowerDestroyed(turnContainer);
        if (alreadySwitched || fearSetup || !midTowerDestroyed) {
            return Optional.empty();
        }
        Optional<LocationType> locationHopeOpt = enemySideLaneHope(turnContainer);
        Optional<Long> lowOrDeadWizardOpt = lowOrDeadWizard(turnContainer);
        if (!locationHopeOpt.isPresent() || !lowOrDeadWizardOpt.isPresent()) {
            return Optional.empty();
        }
        long lowOrDeadWizard = lowOrDeadWizardOpt.get();
        LocationType locationHope = locationHopeOpt.get();
        Message message = new Message(locationHope == LocationType.BOTTOM_LANE ? LaneType.BOTTOM : LaneType.TOP,
                null,
                new byte[]{LANE_SWITCH_CODE});
        turnContainer.getMemory().setMasterDidSwitch(true);
        if (lowOrDeadWizard == turnContainer.getSelf().getId()) {
            turnContainer.getMemory().setSelfMessage(message);
        } else {
            Message[] messages = new Message[NUMBER_OF_ALLY_WIZARDS];
            messages[memory.getAllyWizardMessageIndex().get(lowOrDeadWizard)] = message;
            MoveBuilder moveBuilder = new MoveBuilder();
            moveBuilder.setMessages(messages);
            turnContainer.getMemory().setMasterDidSwitch(true);
            return buildTactic(moveBuilder);
        }
        return Optional.empty();
    }

    private Optional<Long> lowOrDeadWizard(TurnContainer turnContainer) {
        WorldProxy world = turnContainer.getWorldProxy();
        Memory memory = turnContainer.getMemory();
        MapUtils mapUtils = turnContainer.getMapUtils();
        Set<Long> currentWizardId = new HashSet<>();
        Integer minHp = Integer.MAX_VALUE;
        boolean foundInsideBase = false;
        WizardProxy lowestWizard = null;
        for (WizardProxy wizard : world.getWizards()) {
            if (wizard.getFaction() == turnContainer.getSelf().getFaction()) {
                currentWizardId.add(wizard.getId());
                if (mapUtils.allyBaseInfluenceDist(wizard.getX(), wizard.getY()) <=
                        mapUtils.getWaypointBaseInfluence()) {
                    foundInsideBase = true;
                    minHp = wizard.getLife();
                    lowestWizard = wizard;
                } else if (!foundInsideBase && minHp > wizard.getLife()) {
                    minHp = wizard.getLife();
                    lowestWizard = wizard;
                }
            }
        }
        for (Long wizardId : memory.getAllyWizardMessageIndex().keySet()) {
            if (!currentWizardId.contains(wizardId)) {
                return Optional.of(wizardId);
            }
        }
        if (lowestWizard == null) {
            throw new RuntimeException("Should have at least master in the game");
        }
        if (foundInsideBase || ((double) lowestWizard.getLife() / lowestWizard.getMaxLife() <= RELOCATE_HP_THRESHOLD &&
                mapUtils.getLocationType(lowestWizard.getId()) != LocationType.RIVER)) {
            return Optional.of(lowestWizard.getId());
        }
        return Optional.empty();
    }

    private Optional<LocationType> enemySideLaneHope(TurnContainer turnContainer) {
        int enemySideLanes = 0;
        LocationType locationHope = null;
        Memory memory = turnContainer.getMemory();
        for (Long wizardId : memory.getEnemyDominantLocation().keySet()) {
            Optional<LocationType> locationOpt = WizardControl.getDominantLocation(memory, wizardId);
            if (!locationOpt.isPresent()) {
                continue;
            }
            LocationType locationType = locationOpt.get();
            if (locationType == LocationType.TOP_LANE || locationType == LocationType.BOTTOM_LANE) {
                enemySideLanes++;
                locationHope = locationType;
            }
        }
        if (enemySideLanes == 1) {
            return Optional.of(locationHope);
        }
        return Optional.empty();
    }

    private boolean isMidEnemyTowerDestroyed(TurnContainer turnContainer) {
        for (Building building : turnContainer.getMemory().getDestroyedEnemyGuardianTowers()) {
            if (BuildingControl.isMidEnemyTower(turnContainer, building)) {
                return true;
            }
        }
        return false;
    }

    private boolean inFearMode(TurnContainer turnContainer) {
        for (Player player : turnContainer.getWorldProxy().getPlayers()) {
            if (SCARED_OF.contains(player.getName())) {
                return true;
            }
        }
        return false;
    }

    private byte[] roleMessage(WizardRole role) {
        return new byte[]{ROLE_MESSAGE_CODE, (byte) role.getCode()};
    }

    private static Optional<Tactic> buildTactic(MoveBuilder moveBuilder) {
        return Optional.of(new TacticImpl("MasterWizard", moveBuilder, Tactics.MASTER_WIZARD_TACTIC_BUILDER));
    }

    private static class LaneRole {

        LaneType laneType;
        WizardRole role;

        public LaneRole(LaneType laneType, WizardRole role) {
            this.laneType = laneType;
            this.role = role;
        }
    }
}
