package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleTags;
import io.github.fatalitaya.combatnexus.combat.damage.api.VanillaDamageTags;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageTypeFacts;
import org.jspecify.annotations.Nullable;

public final class DamageSourcePolicy {
    public static final NexusId VANILLA_THORNS_DAMAGE_TYPE = NexusId.withDefaultNamespace("thorns");
    private DamageSourcePolicy() {}
    /** Direct holders and hard-vanilla bypass sources remain outside managed execution. */
    public static boolean shouldManage(DamageTypeFacts source) {
        return source != null && source.id().isPresent()
                && !source.tags().contains(DamageModuleTags.DamageTypes.BYPASSES_DAMAGENEXUS)
                && !source.tags().contains(VanillaDamageTags.BYPASSES_INVULNERABILITY);
    }
    public static boolean isVanillaThorns(DamageTypeFacts source) {
        return source != null && source.id().map(DamageSourcePolicy::isVanillaThornsId).orElse(false);
    }
    public static boolean isVanillaThornsId(@Nullable NexusId id) { return VANILLA_THORNS_DAMAGE_TYPE.equals(id); }
}
