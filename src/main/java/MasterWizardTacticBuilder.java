import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class MasterWizardTacticBuilder implements TacticBuilder {

    private static final Set<String> SCARED_OF = new HashSet<>(Collections.singletonList("tyamgin"));
    private static final List<LaneRole> ALL_MID = Arrays.asList(new LaneRole(LaneType.MIDDLE, WizardRole.FROST_BOLT),
            new LaneRole(LaneType.MIDDLE, WizardRole.HASTE_QUICK),
            new LaneRole(LaneType.MIDDLE, WizardRole.RANGE),
            new LaneRole(LaneType.MIDDLE, WizardRole.SHIELD_QUICK),
            new LaneRole(LaneType.MIDDLE, WizardRole.FIREBALL_TEAM));
    public static final byte ROLE_MESSAGE_CODE = 'r';
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

            boolean scared = false;
            for (Player player : world.getPlayers()) {
                if (SCARED_OF.contains(player.getName())) {
                    scared = true;
                }
            }

            Queue<LaneRole> roles;
            if (scared) {
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
            return Optional.of(new TacticImpl("MasterWizard", moveBuilder, Tactics.MASTER_WIZARD_TACTIC_BUILDER));
        }
        return Optional.empty();
    }

    private byte[] roleMessage(WizardRole role) {
        return new byte[]{ROLE_MESSAGE_CODE, (byte) role.getCode()};
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
