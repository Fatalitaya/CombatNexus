package io.github.fatalitaya.combatnexus.combat.control.platform;

import io.github.fatalitaya.combatnexus.combat.spi.ParticipantInstance;
import io.github.fatalitaya.combatnexus.combat.platform.ParticipantIncarnations;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class ParticipantIdentityMixin implements ParticipantInstance {
    @Unique private final long combatnexus$incarnation = ParticipantIncarnations.next();
    public long combatnexus$incarnation() { return combatnexus$incarnation; }
}
