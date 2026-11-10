(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),Map=Java.loadClass("java.util.Map"),Optional=Java.loadClass("java.util.Optional"),Probe=Java.loadClass("probe.AbilityExtension"),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
 var actor=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);actor.setPos(0,512,0);actor.setNoAi(true);actor.setNoGravity(true);actor.addTag("ability_periodic_release");level.addFreshEntity(actor);cls("combat.platform.CombatPersistence").tick(level);actor.setHealth(1);
 var source=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),Probe.id("periodic_provider"),Optional.of(actor.getUUID()),"periodic"),plan=new (cls("combat.api.source.SourcePlan"))(Probe.id("periodic"),Map.of(),Map.of(),Map.of(),Map.of(Probe.id("periodic"),Probe.id("ability_periodic_unlimited")),true);
 var result=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),actor.getUUID(),source,plan));if(!result.applied())throw new Error("periodic source rejected");
 Probe.periodicSession=api.drivers().bind(Java.loadClass("probe.DriverExtension").abilities,actor.getUUID(),Java.loadClass("java.util.Set").of(cls("combat.driver.api.DriverScope").PERIODIC),Optional.empty());
 return JSON.stringify({actor:String(actor.getUUID()),tick:Number(level.getGameTime())});
})();
