(function () {
    function cls(name){return Java.loadClass("io.github.fatalitaya.combatnexus."+name);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Id=cls("api.id.NexusId");
    var Command=cls("combat.absorption.api.AbsorptionCommand"),Operation=cls("combat.absorption.api.AbsorptionCommand$Operation");
    var Binding=cls("combat.health.platform.NativeAbsorptionTarget"),State=cls("combat.absorption.internal.NativeAbsorptionState");
    var checks=[];function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function rejects(action){try{action();return false;}catch(e){return true;}}
    var Type=Java.loadClass("net.minecraft.world.entity.EntityType"),Reason=Java.loadClass("net.minecraft.world.entity.MobSpawnType");
    var cow=Type.COW.create(level),other=Type.COW.create(level);
    cow.setPos(0,512,0);cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);
    var owner=UUID.randomUUID(),source=new (cls("combat.api.CombatSource"))(Id.parse("probe:absorption"),Optional.of(owner),Optional.empty());
    var binding=new Binding(cow),state=binding.state();
    var command=function(amount,capacity){return new Command(UUID.randomUUID(),cow.getUUID(),source,Operation.GRANT,amount,capacity);};
    try {
        check(binding.equals(new Binding(cow)),"repeated bindings retain exact entity identity");
        other.setUUID(cow.getUUID());
        check(!binding.equals(new Binding(other))&&rejects(function(){state.maximum(new Binding(other),0);}),"same UUID cannot borrow another entity absorption state");
        var granted=api.absorption().submit(command(8,10));
        check(granted.applied()&&cow.getAbsorptionAmount()===8&&granted.journal().applicationCompleted(),"world-bound grant records native write");
        api.absorption().submit(command(8,10));
        check(cow.getAbsorptionAmount()===10&&api.absorption().snapshot(cow.getUUID()).get(0).granted()===10,"source capacity preserved");
        cow.setAbsorptionAmount(6);
        var observed=api.absorption().snapshot(cow.getUUID()).get(0);
        check(observed.balance()===6&&observed.consumed()===4,"native setter reconciles owned consumption exactly once");
        var encoded=state.save(binding),copy=new State(new Binding(other));copy.restore(new Binding(other),encoded);
        check(String(copy.save(new Binding(other)))===String(encoded),"saved source identity and ordering round trip unchanged");
        copy.restore(new Binding(other),"broken");
        check(String(copy.save(new Binding(other)))==="broken","invalid persisted payload retained for recovery");
        copy.restore(new Binding(other),encoded);
        check(String(copy.save(new Binding(other)))===String(encoded),"valid restore recovers after invalid state");
        var Target=cls("combat.api.platform.AbsorptionTarget"),Failure=Java.loadClass("java.lang.IllegalStateException"),actual=0,localState=null;
        var fake=new Target({requireThread:function(){},state:function(){return localState;},amount:function(value){if(value!==undefined){actual=value;throw new Failure("after native write");}return actual;}});
        localState=new State(fake);
        var result=cls("combat.absorption.internal.NativeAbsorptionApplication").apply(fake,command(3,3));
        check(!result.applied()&&!result.journal().applicationCompleted()&&actual===3,"write then throw produces incomplete receipt without replay");
        check(State.ledger(fake).balance()===3,"failed application retains observed native balance");
        localState.restore(fake,"broken");
        check(rejects(function(){State.ledger(fake);}),"invalid restored ledger refuses business access");
        api.absorption().submit(new Command(UUID.randomUUID(),cow.getUUID(),source,Operation.REMOVE,0,0));
        check(cow.getAbsorptionAmount()===0,"source removal releases only remaining owned absorption");
    } finally {cow.discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
