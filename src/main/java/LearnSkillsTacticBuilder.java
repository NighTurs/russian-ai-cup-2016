import model.Message;
import model.SkillType;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LearnSkillsTacticBuilder implements TacticBuilder {

    private static final List<SkillType> DEFAULT_SKILL_ORDER = WizardRole.FIREBALL_SOLO.getSkillsOrder();

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        if (!turnContainer.getGame().isSkillsEnabled()) {
            return Optional.empty();
        }

        if (turnContainer.getGame().isRawMessagesEnabled()) {
            for (Message message : turnContainer.getSelf().getMessages()) {
                byte[] rawMessage = message.getRawMessage();
                if (rawMessage[0] == MasterWizardTacticBuilder.ROLE_MESSAGE_CODE) {
                    turnContainer.getMemory().setAssignedRole(WizardRole.fromCode(rawMessage[1]));
                }
            }
        }

        List<SkillType> skillsOrder = turnContainer.getMemory().getAssignedRole() == null ?
                DEFAULT_SKILL_ORDER :
                turnContainer.getMemory().getAssignedRole().getSkillsOrder();

        WizardProxy self = turnContainer.getSelf();

        Set<SkillType> learnedSkills = self.getSkills().isEmpty() ?
                EnumSet.noneOf(SkillType.class) :
                EnumSet.copyOf(self.getSkills());
        if (learnedSkills.size() == self.getLevel()) {
            return Optional.empty();
        }

        for (SkillType skillType : skillsOrder) {
            if (!learnedSkills.contains(skillType)) {
                MoveBuilder moveBuilder = new MoveBuilder();
                moveBuilder.setSkillToLearn(skillType);
                return Optional.of(new TacticImpl("LearnSkills", moveBuilder, Tactics.LEARN_SKILLS_TACTIC_BUILDER));
            }
        }

        return Optional.empty();
    }
}
