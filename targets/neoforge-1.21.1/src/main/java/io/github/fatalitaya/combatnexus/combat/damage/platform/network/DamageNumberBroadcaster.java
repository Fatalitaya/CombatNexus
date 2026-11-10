package io.github.fatalitaya.combatnexus.combat.damage.platform.network;

import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageSettledEvent;
import io.github.fatalitaya.combatnexus.combat.damage.platform.network.payload.DamageNumberPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class DamageNumberBroadcaster {
    private static final float MIN_PRESENTED_DAMAGE = 0.001F;

    private DamageNumberBroadcaster() {
    }

    @SubscribeEvent
    public static void onDamageSettled(DamageSettledEvent event) {
        if (event == null) {
            return;
        }
        broadcast(event.snapshot(), event.context());
    }

    private static void broadcast(DamageSettlementSnapshot snapshot, io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext live) {
        if (snapshot.status() != DamageSettlementStatus.APPLIED) {
            return;
        }

        float damage = snapshot.appliedDamage();
        if (!Float.isFinite(damage) || damage <= MIN_PRESENTED_DAMAGE) {
            return;
        }

        if (!(live.logicalAttacker() instanceof ServerPlayer player)) {
            return;
        }

        if (player.isRemoved() || player.hasDisconnected()) {
            return;
        }
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer || !player.connection.hasChannel(DamageNumberPayload.TYPE)) {
            return;
        }

        LivingEntity target = live.target();
        if (target == null || target.level() != live.level()) {
            return;
        }

        double y = target.getY() + target.getBbHeight() * 0.65D;
        DamageNumberPayload payload = new DamageNumberPayload(
                snapshot.damageId(),
                target.getX(),
                y,
                target.getZ(),
                damage,
                snapshot.critical()
        );

        try {
            PacketDistributor.sendToPlayer(player, payload);
        } catch (RuntimeException exception) {
        }
    }
}
