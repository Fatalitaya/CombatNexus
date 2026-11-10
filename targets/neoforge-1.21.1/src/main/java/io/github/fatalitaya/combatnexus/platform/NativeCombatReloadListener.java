package io.github.fatalitaya.combatnexus.platform;
import io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitionReloadListener;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
public final class NativeCombatReloadListener extends SimplePreparableReloadListener<CombatDefinitionReloadListener.Prepared> {
    private final CombatDefinitionReloadListener compiler;
    public NativeCombatReloadListener(net.minecraft.core.HolderLookup.Provider context){compiler=new CombatDefinitionReloadListener(io.github.fatalitaya.combatnexus.combat.platform.ItemBindingDefaults.take(context));}
    protected CombatDefinitionReloadListener.Prepared prepare(ResourceManager resources, ProfilerFiller profiler) { return compiler.prepare(new NativeDefinitionResources(resources)); }
    protected void apply(CombatDefinitionReloadListener.Prepared prepared, ResourceManager resources, ProfilerFiller profiler) { compiler.apply(prepared); }
    public DefinitionGeneration candidate() { return compiler.candidate(); }
}
