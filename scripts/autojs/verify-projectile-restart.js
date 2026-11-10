(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),UUID=Java.loadClass("java.util.UUID"),checks=[];
    for(var before of RESTART_BEFORE.shots)if(level.getEntity(UUID.fromString(before.id))===null)return JSON.stringify({ready:false});
    for(var before of RESTART_BEFORE.shots){
        var shot=level.getEntity(UUID.fromString(before.id)),launch=api.projectiles().snapshot(shot).orElseThrow();
        if(String(launch.action())!==before.action||launch.shooter().revision()!==before.revision)throw new Error("Launch identity changed after restart");
        var out=Java.loadClass("net.minecraft.world.level.storage.TagValueOutput").createWithContext(Java.loadClass("net.minecraft.util.ProblemReporter").DISCARDING,level.registryAccess());shot.saveWithoutId(out);
        // Compare NBT structurally because map entry order is not a storage contract.
        var expected=Java.loadClass("net.minecraft.nbt.TagParser").parseCompoundFully(before.payload);
        if(!out.buildResult().get("combatnexus_launch").equals(expected))throw new Error("Frozen launch facts changed after restart");
        if(level.getEntity(launch.shooter().entity())!==null)throw new Error("Shooter must be absent for restart fixture");
        var samples=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level).decisionSamples();
        var value=samples.sample(launch.action(),cls("api.id.NexusId").parse("probe:launch"),cls("api.id.NexusId").parse("probe:decision"),Java.loadClass("java.util.Optional").empty(),new (Java.loadClass("java.util.function.Supplier"))({get:function(){throw new Error("Restored decision rerolled");}}));
        if(value!==0.25)throw new Error("Shared decision not restored");
        checks.push("launch identity preserved "+before.id,"complete frozen inputs preserved "+before.id,"missing shooter supported "+before.id,"shared sample not rerolled "+before.id);
        shot.discard();
    }
    return JSON.stringify({ready:true,passed:checks.length,checks:checks});
})()
