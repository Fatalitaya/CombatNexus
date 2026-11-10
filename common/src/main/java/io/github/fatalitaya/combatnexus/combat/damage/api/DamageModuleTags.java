package io.github.fatalitaya.combatnexus.combat.damage.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;

public class DamageModuleTags {

    public static final NexusId SPEARS = createItem("spears");

    private static NexusId createItem(String name) {
        return DamageModuleIds.id(name);
    }

    private static NexusId createEntityType(String name) {
        return DamageModuleIds.id(name);
    }

    private static NexusId createDamageType(String name) {
        return DamageModuleIds.id(name);
    }

    public static final class EntityTypes {
        public static final NexusId BOSSES =
                createEntityType("bosses");

        private EntityTypes() {
        }
    }

    public static class DamageTypes {
        public static final NexusId BYPASSES_DAMAGENEXUS =
                createDamageType("bypasses_combatnexus");

        public static final NexusId IS_PHYSICAL = createDamageType("is_physical");
        public static final NexusId IS_PROJECTILE = createDamageType("is_projectile");
        public static final NexusId IS_RANGED = createDamageType("is_ranged");
        public static final NexusId IS_SPELL = createDamageType("is_spell");
        public static final NexusId IS_EXPLOSION = createDamageType("is_explosion");
        public static final NexusId IS_ENVIRONMENTAL = createDamageType("is_environmental");
        public static final NexusId IS_MAGIC = createDamageType("is_magic");
        public static final NexusId IS_COLD = createDamageType("is_cold");
        public static final NexusId IS_FIRE = createDamageType("is_fire");
        public static final NexusId IS_LIGHTNING = createDamageType("is_lightning");
        public static final NexusId IS_POISON = createDamageType("is_poison");
        public static final NexusId IS_WITHER = createDamageType("is_wither");
        public static final NexusId IS_KINETIC = createDamageType("is_kinetic");
        public static final NexusId IS_MELEE = createDamageType("is_melee");
        public static final NexusId IS_MACE_SMASH =
                createDamageType("is_mace_smash");

        public static final NexusId IS_SPEAR_STAB =
                createDamageType("is_spear_stab");

        public static final NexusId IS_SPEAR_CHARGE =
                createDamageType("is_spear_charge");

        public static final NexusId IS_SPEAR_ATTACK =
                createDamageType("is_spear_attack");

        private DamageTypes() {
        }
    }
}
