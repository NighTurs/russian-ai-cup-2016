import model.LaneType;
import model.Message;
import model.Unit;

import java.util.*;
import java.util.stream.Collectors;

public class MasterWizardTacticBuilder implements TacticBuilder {

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
            Queue<WizardRole> roles = new ArrayDeque<>(Arrays.asList(WizardRole.FROST_BOLT,
                    WizardRole.HASTE,
                    WizardRole.RANGE,
                    WizardRole.SHIELD,
                    WizardRole.FIREBALL_TEAM));
            for (WizardProxy wizard : allyWizardsExceptMe) {
                WizardRole role = roles.poll();
                messages[memory.getAllyWizardMessageIndex().get(wizard.getId())] =
                        new Message(LaneType.MIDDLE, null, roleMessage(role));
            }
            moveBuilder.setMessages(messages);
            turnContainer.getMemory().setSelfMessage(new Message(LaneType.MIDDLE, null, roleMessage(roles.poll())));
            return Optional.of(new TacticImpl("MasterWizard", moveBuilder, Tactics.MASTER_WIZARD_TACTIC_BUILDER));
        }
        return Optional.empty();
    }

    private byte[] roleMessage(WizardRole role) {
        return new byte[]{ROLE_MESSAGE_CODE, (byte) role.getCode()};
    }
}
