package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import java.util.*;
import java.util.function.IntFunction;
@org.jetbrains.annotations.ApiStatus.Internal
public final class EffectCapabilityDescriptions {
 private static final Map<NexusId,IntFunction<List<DisplayText>>> VALUES=new LinkedHashMap<>();
 private static boolean frozen;
 private EffectCapabilityDescriptions() {}
 public static synchronized void register(NexusId id,IntFunction<List<DisplayText>> description) {
  if(frozen)throw new IllegalStateException("Effect descriptions frozen");
  if(VALUES.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(description))!=null)throw new IllegalArgumentException("Duplicate effect description "+id);
 }
 public static void freeze(){frozen=true;}
 public static List<DisplayText> describeRequired(NexusId id,int amplifier){return List.copyOf(Objects.requireNonNull(VALUES.get(id),"Unknown effect description: "+id).apply(amplifier));}
}
