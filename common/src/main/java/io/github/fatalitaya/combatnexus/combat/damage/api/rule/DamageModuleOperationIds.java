package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

/** Stable serialized identifiers for built-in DamageModule rule operations. */
public final class DamageModuleOperationIds {
    public static final NexusId ADD_BASE_DAMAGE = id("add_base_damage");
    public static final NexusId ADD_CHANNEL_PRE_MULTIPLIER = id("add_channel_pre_multiplier");
    public static final NexusId ADD_CHANNEL_POST_MULTIPLIER = id("add_channel_post_multiplier");
    public static final NexusId ADD_GLOBAL_PRE_MULTIPLIER = id("add_global_pre_multiplier");
    public static final NexusId ADD_GLOBAL_POST_MULTIPLIER = id("add_global_post_multiplier");
    public static final NexusId OVERRIDE_FINAL_DAMAGE = id("override_final_damage");
    public static final NexusId CANCEL_DAMAGE = id("cancel_damage");
    public static final NexusId ADD_TEMPORARY_RESISTANCE = id("add_temporary_resistance");
    public static final NexusId CONVERT_DAMAGE = id("convert_damage");
    public static final NexusId GAIN_EXTRA_DAMAGE = id("gain_extra_damage");
    public static final NexusId ADD_CHANNEL_MITIGATION = id("add_channel_mitigation");
    public static final NexusId ADD_GLOBAL_MITIGATION = id("add_global_mitigation");
    public static final NexusId MULTIPLY_ARMOR_EFFECTIVENESS = id("multiply_armor_effectiveness");
    public static final NexusId ADD_TRUE_DAMAGE = id("add_true_damage");

    private DamageModuleOperationIds() {}
    private static NexusId id(String path) { return DamageModuleIds.id(path); }
}
