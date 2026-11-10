package io.github.fatalitaya.combatnexus.combat.health.platform;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.ApplicationJournal;
import io.github.fatalitaya.combatnexus.combat.api.platform.LethalTarget;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class NativeLethalBinding extends NativeHealthBinding implements LethalTarget {
    private final DamageSource source;
    public NativeLethalBinding(LivingEntity entity, DamageSource source) { super(entity); this.source = java.util.Objects.requireNonNull(source); }
    public Set<NexusId> damageTags() { return source.typeHolder().tags().map(tag -> NativeIds.nexus(tag.location())).collect(Collectors.toUnmodifiableSet()); }
    public boolean protect() { return ((NativeHealthTarget)entity).combatnexus$nativeProtection(source); }
    public List<ApplicationJournal.Entry> protectionEvidence() { return ((NativeHealthTarget)entity).combatnexus$protectionEvidence(); }
    public boolean removed() { return entity.isRemoved(); }
    public boolean alive() { return entity.isAlive(); }
    public void deathSound() { ((NativeHealthTarget)entity).combatnexus$deathSound(); }
    public void rememberAttacker() {
        if (source.getEntity() instanceof LivingEntity attacker) {
            entity.setLastHurtByMob(attacker);
            if (attacker instanceof Player player) entity.setLastHurtByPlayer(player);
        }
    }
    public void recordDamage(float amount) { entity.getCombatTracker().recordDamage(source, amount); }
    public void die() { entity.die(source); }
    public boolean deathConfirmed() { return ((NativeHealthTarget)entity).combatnexus$deathConfirmed(); }
}
