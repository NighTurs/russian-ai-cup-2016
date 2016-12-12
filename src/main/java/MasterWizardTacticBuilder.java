import model.LaneType;
import model.Message;
import model.Unit;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MasterWizardTacticBuilder implements TacticBuilder {

    private static final byte[] EMPTY_RAW_MESSAGE = new byte[0];
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
            Message message = new Message(LaneType.MIDDLE, null, EMPTY_RAW_MESSAGE);
            for (WizardProxy wizard : allyWizardsExceptMe) {
                messages[memory.getAllyWizardMessageIndex().get(wizard.getId())] = message;
            }
            moveBuilder.setMessages(messages);
            turnContainer.getMemory().setSelfMessage(message);
            return Optional.of(new TacticImpl("MasterWizard", moveBuilder, Tactics.MASTER_WIZARD_TACTIC_BUILDER));
        }
        return Optional.empty();
    }
}
