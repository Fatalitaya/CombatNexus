package probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.platform.api.CombatRegisterEvent;
import io.github.fatalitaya.combatnexus.effect.api.EffectDisplay;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

/** AutoJS startup extension. Not a production source set or published dependency. */
@Mod("combatnexus_effect_probe")
public final class EffectExtension {
    public static boolean rejectedUnfrozenBucket;
    public static java.util.UUID failingShieldTarget;
    public static int shieldCallbacks;
    private static final java.util.List<io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment> MOMENTS = new java.util.ArrayList<>();
    public static java.util.List<io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment> moments() { return java.util.List.copyOf(MOMENTS); }
    public static void clearMoments() { MOMENTS.clear(); }
    private static final java.util.Map<io.github.fatalitaya.combatnexus.effect.api.EffectCapability.Trigger, Integer> TRIGGERS = new java.util.EnumMap<>(io.github.fatalitaya.combatnexus.effect.api.EffectCapability.Trigger.class);
    public static void resetTriggers() { TRIGGERS.clear(); }
    public static int triggers(String name) { return TRIGGERS.getOrDefault(io.github.fatalitaya.combatnexus.effect.api.EffectCapability.Trigger.valueOf(name), 0); }
    public static net.minecraft.server.network.ServerGamePacketListenerImpl localConnection(net.minecraft.server.level.ServerPlayer player) {
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        return new net.minecraft.server.network.ServerGamePacketListenerImpl(player.level().getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) { return false; }
        };
    }
    public static boolean rejectedDescriptorMismatch;
    private static io.github.fatalitaya.combatnexus.combat.api.platform.WorldLifecycleAccess lifecycle;
    public static boolean rejectedComputeLifecycle;
    public static boolean rejectsWrongThreadLifecycle(net.minecraft.server.level.ServerLevel level) {
        var rejected = new java.util.concurrent.atomic.AtomicBoolean();
        var worker = new Thread(() -> { try { lifecycle.loaded(level); } catch (IllegalStateException expected) { rejected.set(true); } });
        worker.start();
        try { worker.join(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
        return rejected.get();
    }
    private static io.github.fatalitaya.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentAccess;
    public static boolean rejectsExpiredEquipmentSink(net.minecraft.world.entity.LivingEntity target) {
        var captured = new java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt>>();
        equipmentAccess.apply(target, target.damageSources().generic(), captured::set);
        try {
            captured.get().accept(new io.github.fatalitaya.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt(
                io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.BODY, NexusId.parse("minecraft:wolf_armor"), 0, 0, false));
            return false;
        } catch (IllegalStateException expected) { return true; }
    }
    private static final NexusId ID = NexusId.parse("combatnexus_effect_probe:condition");
    public EffectExtension(IEventBus bus) {
        var displays = DeferredRegister.create(Registries.MOB_EFFECT, "combatnexus_effect_probe");
        displays.register("condition", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x66AA88) {}); displays.register(bus);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> {
            if ("true".equals(System.getenv("COMBATNEXUS_VERIFICATION_FROZEN"))) event.getServer().tickRateManager().setFrozen(true);
        });
        BindingExtension.items(bus);
        AbilityExtension.items(bus);
        NeoForge.EVENT_BUS.addListener(EffectExtension::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, CombatRegisterEvent.class,
            EffectExtension::registrationLimits);
        NeoForge.EVENT_BUS.addListener(EffectExtension::registerDamage);
    }
    private static io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext retainedDamageContext;
    public static boolean rejectsExpiredContribution() {
        try { retainedDamageContext.recordContribution(null, null); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    private static final java.util.Map<java.util.UUID, io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput> INPUTS = new java.util.LinkedHashMap<>();
    public static io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput(java.util.UUID target) { return INPUTS.get(target); }
    private static void registerDamage(io.github.fatalitaya.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent event) {
        event.registerSettlementListener(NexusId.parse("combatnexus_effect_probe:item_block_authority"), 0,
            callback -> { if (callback.snapshot().itemBlocked()) ItemBlockExtension.retained = callback; });
        DefenseExtension.register(event);
        ProcessorFailureProbe.register(event);
        PlanExample.registerDamage(event);
        try {
            io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets.runtimeIndex(
                    io.github.fatalitaya.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets.GENERIC_DAMAGE);
            throw new AssertionError("Bucket index exposed before startup freeze");
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains("not frozen yet")) throw expected;
            rejectedUnfrozenBucket = true;
        }
        event.registerPhaseProcessor(NexusId.parse("combatnexus_effect_probe:native_input"), new io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor() {
            public io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase phase() {
                return io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase.BASE_MODIFICATION;
            }
            public int getPriority() { return Integer.MAX_VALUE; }
            public void apply(io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext context) {
                if (INPUTS.size() > 512) INPUTS.clear();
                try { lifecycle.beginTick((net.minecraft.server.level.ServerLevel)context.victim().level()); rejectedComputeLifecycle = false; }
                catch (IllegalStateException expected) { rejectedComputeLifecycle = true; }
                retainedDamageContext = context;
                INPUTS.put(context.victim().getUUID(), context.nativeInput());
            }
        });
    }
    public static boolean driverCapacityRejected, executorCapacityRejected;
    public static int additionalDrivers, additionalExecutors;
    private static void registrationLimits(CombatRegisterEvent event) {
        var api = event.registrar();
        for (int i = 0; i <= 64; i++) {
            try {
                var owner = io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition.ApplicationOwner.COMBATNEXUS;
                api.externalDriver(new io.github.fatalitaya.combatnexus.combat.driver.api.DriverDefinition(
                    new NexusId("combatnexus_effect_probe", "capacity_driver_" + i),
                    java.util.Set.of(io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope.ACTIVE), owner, owner, owner));
                additionalDrivers++;
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains("capacity")) throw expected;
                driverCapacityRejected = true;
                break;
            }
        }
        for (int i = 0; i <= 256; i++) {
            try {
                api.abilityExecutor(new NexusId("combatnexus_effect_probe", "capacity_executor_" + i),
                    new io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutor(
                        new io.github.fatalitaya.combatnexus.combat.ability.api.AbilityExecutorSpec(
                            java.util.Set.of(io.github.fatalitaya.combatnexus.combat.ability.api.AbilityTarget.Kind.SELF),
                            java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING), 1),
                        input -> true, execution -> new io.github.fatalitaya.combatnexus.combat.ability.api.AbilityStepResult.Complete()));
                additionalExecutors++;
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains("capacity")) throw expected;
                executorCapacityRejected = true;
                break;
            }
        }
    }
    private static void register(CombatRegisterEvent event) {
        var api = event.registrar();
        api.sourceDescriptions(NexusId.parse("probe:panel_capture_counter"), input -> {
            if (input.entity().equals(PresentationCost.observedTarget)) PresentationCost.panelCaptures++;
            return java.util.List.of();
        });
        ItemBlockExtension.register(api);
        AbilityExtension.register(api);
        SdkExample.register(api);
        PlanExample.register(api);
        SemanticExample.register(api);
        DriverExtension.register(api);
        api.observeShield(NexusId.parse("combatnexus_effect_probe:shield_failure"), 10, result -> {
            if (result.target().equals(failingShieldTarget)) throw new IllegalStateException("Intentional shield observer failure");
        });
        api.observeShield(NexusId.parse("combatnexus_effect_probe:shield_after_failure"), 0, result -> {
            if (result.target().equals(failingShieldTarget)) shieldCallbacks++;
        });
        equipmentAccess = api.equipmentDamage();
        lifecycle = api.worldLifecycle();
        api.observeMoment(NexusId.parse("combatnexus_effect_probe:moments"), io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.class, moment -> {
            if (MOMENTS.size() == 4096) MOMENTS.removeFirst();
            MOMENTS.add(moment);
        });
        api.effectCondition(ID, facts -> facts.getOrDefault("active", 0.0) > 0);
        api.effectCapability(ID, context -> { TRIGGERS.merge(context.trigger(), 1, Integer::sum); return true; });
        api.effectDisplay(ID, new EffectDisplay(ID, false, false, true, null, false, false, true));
        api.effectStacking(NexusId.parse("combatnexus_effect_probe:keep_first"), (old, incoming, now) -> old);
        api.behavior(new BehaviorType<>(NexusId.parse("combatnexus_effect_probe:pulse"), Pulse.class,
            RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Pulse::amount),
                Codec.INT.fieldOf("interval").forGetter(Pulse::intervalTicks)).apply(i, Pulse::new))));
        BindingExtension.definitions(api);
        api.behavior(new BehaviorType<>(NexusId.parse("combatnexus_effect_probe:mystery"), Mystery.class, com.mojang.serialization.MapCodec.unit(new Mystery())));
        try { api.effectDescriptor(NexusId.parse("combatnexus_effect_probe:pulse"), Mystery.class, (value, level) -> null); }
        catch (IllegalArgumentException expected) { rejectedDescriptorMismatch = true; }
        api.effectDescriptor(NexusId.parse("combatnexus_effect_probe:pulse"), Pulse.class, (pulse, level) ->
            new io.github.fatalitaya.combatnexus.presentation.api.TooltipBlock(java.util.Optional.<io.github.fatalitaya.combatnexus.presentation.api.DisplayText>empty(),
                java.util.List.of(io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts.translatable("tooltip.combatnexus.effect.periodic",
                    io.github.fatalitaya.combatnexus.presentation.api.TooltipNumbers.number(pulse.intervalTicks() / 20.0),
                    io.github.fatalitaya.combatnexus.presentation.api.TooltipNumbers.number(pulse.amount()))), java.util.List.of()));
    }
    public record Mystery() implements BehaviorComponent {}
    public record Pulse(double amount, int intervalTicks) implements PeriodicBehavior {
        public PeriodicPhase phase() { return PeriodicPhase.PRESERVE; }
        public void execute(PeriodicExecution execution) { execution.heal(amount); }
    }
}
