package io.github.fatalitaya.combatnexus.presentation.platform.client;

import io.github.fatalitaya.combatnexus.platform.NativeIds;
import com.google.gson.*;
import io.github.fatalitaya.combatnexus.presentation.platform.network.*;
import io.github.fatalitaya.combatnexus.presentation.client.ClientTooltipDefinitions;
import io.github.fatalitaya.combatnexus.presentation.platform.client.ItemDescriptionProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.*;

@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class CombatPresentationClient {
    private static final KeyMapping PANEL = new KeyMapping("key.combatnexus.panel", 67, "key.categories.inventory");
    private static ClientLevel world;
    private static net.minecraft.client.player.LocalPlayer player;
    private static UUID session;
    private static long generation;
    private CombatPresentationClient() {}
    static long generation() { return generation; }
    public static void keys(RegisterKeyMappingsEvent event) { event.register(PANEL); }
    public static void handle(CombatControlPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) { CombatControlClient.accept(payload); }
    public static void handle(CombatGenerationPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) { adoptGeneration(payload.generation()); }
    public static void handle(CombatPresentationPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) { accept(payload); }
    public static void handle(TooltipDefinitionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) { ClientTooltipDefinitions.accept(payload.generation(), ClientTooltipDefinitions.Kind.valueOf(payload.kind().name()), payload.id(), payload.json()); }
    public static void handle(EffectTooltipPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
            reconcile();
            if (context.player().getUUID().equals(payload.target()) && NativeIds.nexus(context.player().level().dimension().location()).equals(payload.dimension())
                    && payload.generation() == generation && (session == null || session.equals(payload.session()))) {
                session = payload.session();
                io.github.fatalitaya.combatnexus.presentation.client.ClientEffectTooltips.accept(payload.effect(), payload.json());
            }
        }
    private static void reconcile() {
        var mc = Minecraft.getInstance();
        if (world != mc.level || player != mc.player) {
            clearWorld(); world = mc.level; player = mc.player;
        }
    }
    private static void clearWorld() {
        io.github.fatalitaya.combatnexus.presentation.client.ClientEffectTooltips.clear();
        world = null; player = null; session = null;
        ClientEntityCombatMirrors.clear();
        if (Minecraft.getInstance().screen instanceof CombatPanelScreen screen) screen.invalidate();
    }
    private static void clear() { clearWorld(); generation = 0; ClientTooltipDefinitions.clear(); }
    private static void adoptGeneration(long next) {
        if (next <= generation) return;
        generation = next;
        ClientTooltipDefinitions.begin(next);
        io.github.fatalitaya.combatnexus.presentation.client.ClientEffectTooltips.clear();
        ClientEntityCombatMirrors.clear();
        if (Minecraft.getInstance().screen instanceof CombatPanelScreen screen) screen.invalidate();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        reconcile();
        var mc = Minecraft.getInstance();
        while (PANEL.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.screen == null) {
                var target = mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living ? living : mc.player;
                open(target);

            }
        }
        if (mc.screen instanceof CombatPanelScreen screen) {
            var target = screen.target();
            if (target.level() != mc.level || target.isRemoved()) { mc.setScreen(null); return; }
            ClientEntityCombatMirrors.subscribe(target);
            var refusal = ClientEntityCombatMirrors.refusal(target);
            if (refusal.isPresent()) screen.reject(refusal.get());
            else screen.accept(ClientEntityCombatMirrors.rows(target));
        } else ClientEntityCombatMirrors.clear();
    }
    public static void open(net.minecraft.world.entity.LivingEntity target) {
        var mc = Minecraft.getInstance();
        if (target.level() != mc.level || target.isRemoved()) throw new IllegalArgumentException("Panel target must be a current client entity");
        mc.setScreen(new CombatPanelScreen(target));
    }
    @SubscribeEvent public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof net.minecraft.world.entity.LivingEntity entity) ClientEntityCombatMirrors.remove(entity);
    }
    public static Optional<JsonObject> item(ItemStack stack) {
        var mc = Minecraft.getInstance();
        if (stack == null || stack.isEmpty() || mc.player == null || mc.level == null) return Optional.empty();
        try { return Optional.of(ItemDescriptionProjection.capture(mc.player, stack)); }
        catch (Exception failure) {
            return Optional.empty();
        }
    }
    private static void accept(CombatPresentationPayload payload) {
        reconcile();
        if (payload.generation() != generation) return;
        ClientEntityCombatMirrors.accept(payload);
    }
}
