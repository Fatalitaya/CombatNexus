package io.github.fatalitaya.combatnexus.effect.api;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
public interface EffectCommands {
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<List<EffectCleanse.Entry>> cleanse(EffectCleanse request);
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<List<EffectCleanse.Entry>> cleanse(EffectCleanse request,CombatParent parent);
    EffectResult fact(UUID request, EffectFact fact);
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<Integer> clear(UUID request, UUID target);
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<Boolean> remove(UUID request, UUID target, io.github.fatalitaya.combatnexus.api.id.NexusId effect,
            io.github.fatalitaya.combatnexus.api.id.NexusId source, EffectService.Removal reason);
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command);
    io.github.fatalitaya.combatnexus.combat.api.CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command, CombatParent parent);
    List<EffectView> snapshot(UUID target);
    EffectResult apply(EffectApplication application);
    EffectResult apply(EffectApplication application, CombatParent parent);
    EffectResult bind(UUID request, EffectBinding binding);
    EffectResult bind(UUID request, EffectBinding binding, CombatParent parent);
    EffectResult dispel(UUID request, EffectView effect);
    EffectResult dispel(UUID request, EffectView effect, CombatParent parent);
}
