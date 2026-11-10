package io.github.fatalitaya.combatnexus.combat.damage.platform.lifecycle;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.template.DamageTemplateRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Platform lifecycle forwarding; storage and atomic reset live in common. */
@EventBusSubscriber(modid = "combatnexus")
public final class DamageStoreLifecycle {
    private DamageStoreLifecycle() {}
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DamageChannelRegistry.resetServerState();
        DamageTemplateRegistry.resetServerState();
        DatapackDamageRuleStore.resetServerState();
    }
}
