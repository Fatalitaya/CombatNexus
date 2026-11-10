package probe;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;
import com.google.gson.JsonObject;

/** Measures only the supplied projection calls, after warming both paths equally. */
public final class PresentationCost {
    private static volatile Object sink;
    public static String withEquipment(String supplement, String equipment) {
        var value=com.google.gson.JsonParser.parseString(supplement).getAsJsonObject();
        value.add("equipment",com.google.gson.JsonParser.parseString(equipment));
        return value.toString();
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
