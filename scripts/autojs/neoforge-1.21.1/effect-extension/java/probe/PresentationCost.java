package probe;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;
import com.google.gson.JsonObject;

/** Measures only the supplied projection calls, after warming both paths equally. */
public final class PresentationCost {
    private static volatile Object sink;
    public static java.util.UUID observedTarget;
    public static int panelCaptures;
    public static String withEquipment(String supplement, String equipment) {
        var value=com.google.gson.JsonParser.parseString(supplement).getAsJsonObject();
        value.add("equipment",com.google.gson.JsonParser.parseString(equipment));
        return value.toString();
    }
    public static int subscriptions(io.github.fatalitaya.combatnexus.presentation.platform.CombatPresentationNetwork network) {
        try {
            var field=network.getClass().getDeclaredField("mirrors");field.setAccessible(true);
            return ((java.util.Map<?,?>)field.get(network)).size();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    public static JsonObject equipment(Object world, java.util.UUID target, boolean cached) {
        try {
            var field=world.getClass().getDeclaredField("equipment");field.setAccessible(true);
            var controller=field.get(world);
            var refresh=controller.getClass().getDeclaredMethod("refresh",java.util.UUID.class,java.util.Map.class);refresh.setAccessible(true);
            var completed=controller.getClass().getDeclaredField("completed");completed.setAccessible(true);
            var resolved=controller.getClass().getDeclaredField("resolved");resolved.setAccessible(true);
            var observations=controller.getClass().getDeclaredField("observations");observations.setAccessible(true);
            var nativeObservations=observations.get(controller);
            var clear=nativeObservations.getClass().getMethod("clear",java.util.UUID.class);
            return sample(() -> {
                try {
                    if (!cached) {
                        ((java.util.Map<?,?>)completed.get(controller)).remove(target);
                        ((java.util.Map<?,?>)resolved.get(controller)).remove(target);
                        clear.invoke(nativeObservations,target);
                    }
                    return refresh.invoke(controller,target,java.util.Map.of());
                } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            },200,1000);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    public static JsonObject checkpoint(io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext context) {
        try {
            var field=context.getClass().getDeclaredField("delegate");field.setAccessible(true);
            var internal=field.get(context);
            var checkpoint=internal.getClass().getMethod("checkpointMutableState");
            return sample(() -> {
                try { return checkpoint.invoke(internal); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            }, 200, 1000);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    public static JsonObject sample(Supplier<?> work, int warmup, int samples) {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();
        for(int i=0;i<warmup;i++)sink=work.get();
        long bytes=bean.getThreadAllocatedBytes(thread),started=System.nanoTime();
        for(int i=0;i<samples;i++)sink=work.get();
        long nanos=System.nanoTime()-started;
        var result=new JsonObject();result.addProperty("warmup",warmup);result.addProperty("samples",samples);
        result.addProperty("allocatedBytes",bean.getThreadAllocatedBytes(thread)-bytes);result.addProperty("nanos",nanos);
        return result;
    }
    public static JsonObject measure(Supplier<?> previous, Supplier<?> current) {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();
        int warmup=10000,samples=20000;
        for(int i=0;i<warmup;i++){sink=previous.get();sink=current.get();}
        long before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<samples;i++)sink=previous.get();
        long oldBytes=bean.getThreadAllocatedBytes(thread)-before;
        before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<samples;i++)sink=current.get();
        long newBytes=bean.getThreadAllocatedBytes(thread)-before;
        var result=new JsonObject();result.addProperty("warmup",warmup);result.addProperty("samples",samples);
        result.addProperty("oldAllocatedBytes",oldBytes);result.addProperty("newAllocatedBytes",newBytes);
        return result;
    }
}
