import model.SkillType;

import java.util.ArrayList;
import java.util.List;

/**
 * Типы волшебников
 */
enum WizardType {

    UNIVERSAL_RANGE(new ArrayList<SkillType>() {{
        add(SkillType.RANGE_BONUS_PASSIVE_1);
        add(SkillType.RANGE_BONUS_AURA_1);
        add(SkillType.RANGE_BONUS_PASSIVE_2);
        add(SkillType.RANGE_BONUS_AURA_2);
        add(SkillType.ADVANCED_MAGIC_MISSILE);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
        add(SkillType.FROST_BOLT);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
        add(SkillType.SHIELD);
    }}),

    UNIVERSAL_FROST(new ArrayList<SkillType>() {{
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
        add(SkillType.FROST_BOLT);
        add(SkillType.RANGE_BONUS_PASSIVE_1);
        add(SkillType.RANGE_BONUS_AURA_1);
        add(SkillType.RANGE_BONUS_PASSIVE_2);
        add(SkillType.RANGE_BONUS_AURA_2);
        add(SkillType.ADVANCED_MAGIC_MISSILE);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
        add(SkillType.SHIELD);
    }}),

    SAVER_FROST(new ArrayList<SkillType>() {{
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
        add(SkillType.FROST_BOLT);
        add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1);
        add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1);
        add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2);
        add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2);
        add(SkillType.HASTE);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
        add(SkillType.SHIELD);
    }}),

    DAMAGER_FIREBALL(new ArrayList<SkillType>() {{
        add(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.STAFF_DAMAGE_BONUS_AURA_1);
        add(SkillType.STAFF_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.STAFF_DAMAGE_BONUS_AURA_2);
        add(SkillType.FIREBALL);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
        add(SkillType.FROST_BOLT);
        add(SkillType.RANGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
        add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1);
        add(SkillType.RANGE_BONUS_AURA_1);
        add(SkillType.RANGE_BONUS_PASSIVE_2);
    }}),

    SAVER_SHIELD(new ArrayList<SkillType>() {{
        add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1);
        add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_1);
        add(SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2);
        add(SkillType.MOVEMENT_BONUS_FACTOR_AURA_2);
        add(SkillType.HASTE);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_ABSORPTION_AURA_2);
        add(SkillType.SHIELD);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_1);
        add(SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2);
        add(SkillType.MAGICAL_DAMAGE_BONUS_AURA_2);
        add(SkillType.FROST_BOLT);
    }});

    List<SkillType> skills;

    WizardType(List<SkillType> skills) {
        this.skills = skills;
    }


}
