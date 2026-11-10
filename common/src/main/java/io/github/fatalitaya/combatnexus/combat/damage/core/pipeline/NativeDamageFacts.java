package io.github.fatalitaya.combatnexus.combat.damage.core.pipeline;

import io.github.fatalitaya.combatnexus.combat.damage.bridge.vanilla.VanillaDamageSourceProfile;

/** Captured bridge facts; no native write or application authority. */
public interface NativeDamageFacts {
    boolean isMeleeDamage();
    boolean isProjectileDamage();
    VanillaDamageSourceProfile vanillaSourceProfile();
}
