import model.SkillType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum WizardRole {
    FIREBALL_SOLO(1,
            Arrays.asList(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                    SkillType.FIREBALL,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                    SkillType.HASTE,
                    SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.RANGE_BONUS_AURA_2,
                    SkillType.ADVANCED_MAGIC_MISSILE)),
    FIREBALL_TEAM(2,
            Arrays.asList(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                    SkillType.FIREBALL,
                    SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                    SkillType.RANGE_BONUS_AURA_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)),
    RANGE(3,
            Arrays.asList(SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.RANGE_BONUS_AURA_2,
                    SkillType.ADVANCED_MAGIC_MISSILE,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                    SkillType.FIREBALL,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2)),
    HASTE(4,
            Arrays.asList(SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2,
                    SkillType.HASTE,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                    SkillType.FIREBALL,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.RANGE_BONUS_AURA_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)),
    SHIELD(5,
            Arrays.asList(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2,
                    SkillType.SHIELD,
                    SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.STAFF_DAMAGE_BONUS_AURA_2,
                    SkillType.FIREBALL,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.RANGE_BONUS_AURA_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2)),
    FROST_BOLT(6,
            Arrays.asList(SkillType.RANGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                    SkillType.MAGICAL_DAMAGE_BONUS_AURA_2,
                    SkillType.FROST_BOLT,
                    SkillType.RANGE_BONUS_AURA_1,
                    SkillType.RANGE_BONUS_PASSIVE_2,
                    SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1,
                    SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1,
                    SkillType.MOVEMENT_BONUS_FACTOR_AURA_2));
    private final int code;
    private final List<SkillType> skillsOrder;
    private static final Map<Integer, WizardRole> codes = new HashMap<>();

    static {
        for (WizardRole role : WizardRole.values()) {
            codes.put(role.getCode(), role);
        }
    }

    WizardRole(int code, List<SkillType> skillsOrder) {
        this.code = code;
        this.skillsOrder = skillsOrder;
    }

    public int getCode() {
        return code;
    }

    public List<SkillType> getSkillsOrder() {
        return skillsOrder;
    }

    public static WizardRole fromCode(int code) {
        return codes.get(code);
    }
}
