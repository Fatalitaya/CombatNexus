package io.github.fatalitaya.combatnexus.presentation.platform.client;
import io.github.fatalitaya.combatnexus.platform.NativeIds;
import io.github.fatalitaya.combatnexus.presentation.platform.network.CombatControlPayload;
import io.github.fatalitaya.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class CombatControlClient {
    private static CombatControlPayload state;
    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static net.minecraft.client.player.LocalPlayer player;
    private static void reconcile() {
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player) { state = null; level = mc.level; player = mc.player; }
    }
    public static void accept(CombatControlPayload value) {
        reconcile(); var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.getId() != value.entityId()
                || !NativeIds.nexus(mc.level.dimension().location()).equals(value.dimension())) return;
        if (state != null && (!state.session().equals(value.session()) || value.tick() < state.tick())) return;
        state = value;
    }
    public static long incarnation() { reconcile(); return state == null ? 0 : state.incarnation(); }
    public static java.util.UUID session() { reconcile(); return state == null ? null : state.session(); }
    public static boolean allowed(RestrictedAction action) { reconcile(); return state == null || (state.mask() & (1 << action.ordinal())) == 0; }
    public static void register() {
        io.github.fatalitaya.combatnexus.combat.api.action.CombatActionGate.clientView((entity, action) -> entity != Minecraft.getInstance().player || allowed(action));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { state = null; level = null; player = null; }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var input = event.getInput();
        boolean walk = allowed(RestrictedAction.WALK);
        if (!walk) { input.up = false; input.down = false; input.left = false; input.right = false; input.forwardImpulse = 0; input.leftImpulse = 0; }
        if (!allowed(RestrictedAction.JUMP)) input.jumping = false;
    }
    @SubscribeEvent public static void action(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult
                && !allowed(RestrictedAction.ATTACK)) {
            event.setCanceled(true); event.setSwingHand(false);
        }
    }
}
