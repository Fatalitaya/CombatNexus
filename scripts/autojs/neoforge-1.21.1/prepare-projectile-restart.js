(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Spawn=Java.loadClass("net.minecraft.world.entity.MobSpawnType"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
 AutoJS.server().overworld().setChunkForced(0,0,true);
    var shooter=Type.COW.create(level);shooter.setPos(0,256,0);shooter.setNoAi(true);shooter.setNoGravity(true);level.addFreshEntity(shooter);
    shooter.setItemSlot(Slot.MAINHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").BOW));
    var result=[];
    for(var type of [Type.ARROW,Type.TRIDENT,Type.FIREWORK_ROCKET]) {
        var shot=type.create(level);shot.setPos(0,256,0);shot.setNoGravity(true);shot.setOwner(shooter);
        if(type===Type.FIREWORK_ROCKET)continue; // Fireworks expire naturally; their save/load codec is covered separately.
        level.addFreshEntity(shot);shot.addTag("combatnexus_launch_restart");
        var launch=api.projectiles().snapshot(shot).orElseThrow(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Id=cls("api.id.NexusId"),Optional=Java.loadClass("java.util.Optional");
        world.decisionSamples().sample(launch.action(),Id.parse("probe:launch"),Id.parse("probe:decision"),Optional.empty(),new (Java.loadClass("java.util.function.Supplier"))({get:function(){return Java.loadClass("java.lang.Float").valueOf(0.25);}}));
        var out=new (Java.loadClass("net.minecraft.nbt.CompoundTag"))();shot.saveWithoutId(out);
        result.push({id:String(shot.getUUID()),action:String(launch.action()),revision:launch.shooter().revision(),payload:String(out.get("combatnexus_launch"))});
    }
    shooter.discard();
    return JSON.stringify({prepared:true,shots:result});
})()
