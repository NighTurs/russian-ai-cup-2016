import model.SkillType;

import java.util.*;

public class LearnSkillsTacticBuilder implements TacticBuilder {

    private static final List<SkillType> skillsOrder = Arrays.asList(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
            SkillType.STAFF_DAMAGE_BONUS_AURA_1,
            SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
            SkillType.STAFF_DAMAGE_BONUS_AURA_2,
            SkillType.FIREBALL,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
            SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
            SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
            SkillType.HASTE,
            SkillType.RANGE_BONUS_PASSIVE_1,
            SkillType.RANGE_BONUS_AURA_1,
            SkillType.RANGE_BONUS_PASSIVE_2,
            SkillType.RANGE_BONUS_AURA_2,
            SkillType.ADVANCED_MAGIC_MISSILE);

    @Override
    public Optional<Tactic> build(TurnContainer turnContainer) {
        if (!turnContainer.getGame().isSkillsEnabled()) {
            return Optional.empty();
        }

        WizardProxy self = turnContainer.getSelf();

        Set<SkillType> learnedSkills = self.getSkills().length == 0 ?
                EnumSet.noneOf(SkillType.class) :
                EnumSet.copyOf(Arrays.asList(self.getSkills()));
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
