// Run with AutoJS eval_js on a disposable NeoForge 26.1.2 server (server side).
(function () {
    Java.loadClass("io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var root = "io.github.fatalitaya.combatnexus.combat.damage.";
    var Api = Java.loadClass(root + "api.DamageModuleApi");
    var Request = Java.loadClass(root + "api.damage.DamageRequest");
    var Source = Java.loadClass(root + "platform.source.NativeDamageSources");
    var Tracker = Java.loadClass(root + "core.settlement.DamageSettlementTracker");
    var Queue = Java.loadClass(root + "core.settlement.DamageSettlementCoordinator");
    var Content = Java.loadClass(root + "platform.DamageContent");
    var Builtins = Java.loadClass(root + "builtin.BuiltinDamageProcessors");
    var EntityType = Java.loadClass("net.minecraft.world.entity.EntityType");
    var Reason = Java.loadClass("net.minecraft.world.entity.MobSpawnType");
    var level = AutoJS.server().overworld();
    var checks = [];
    function check(value, label) {
        if (!value) throw new Error(label);
        checks.push(label);
    }
    function idle() {
        check(!Tracker.hasActiveHurt() && Queue.pendingCountForTests() === 0,
              "hurt scope and completion FIFO cleaned");
    }
    check(Content.block() !== null && Content.blockEntity() !== null &&
          Content.entity() !== null && Content.menu() !== null, "lazy content handles resolve");
    check(Builtins.builtInProcessors().size() === 21, "21 built-in processors");
    var Decisions=Java.loadClass(root+"core.pipeline.DamageDecisionCalculations"),Decision=Java.loadClass(root+"api.critical.CriticalDecision");
    check(Decisions.resolveCritical(Decision.DEFAULT,false,false,false,true).requiresSampling(),"eligible default critical explicitly requests sampling");
    [[Decision.SUPPRESS_CRITICAL,false,false,false,true,"SUPPRESSED"],
     [Decision.FORCE_CRITICAL,false,false,false,true,"FORCED"],
     [Decision.DEFAULT,false,true,false,true,"VANILLA_PROJECTILE"],
     [Decision.DEFAULT,true,false,false,true,"VANILLA_MELEE"],
     [Decision.DEFAULT,false,false,true,true,"INELIGIBLE"],
     [Decision.DEFAULT,false,false,false,false,"INELIGIBLE"]].forEach(function(row){
        var resolved=Decisions.resolveCritical(row[0],row[1],row[2],row[3],row[4]);
        check(!resolved.requiresSampling()&&String(resolved.outcome().orElseThrow())===row[5],"decided critical consumes no chance sample: "+row[5]);
    });
    check(Decisions.criticalChance(NaN)===0&&Decisions.criticalChance(-1)===0&&Decisions.criticalChance(2)===1,"critical probability nonfinite and range boundaries");
    check(!Decisions.critical(0,0)&&Decisions.critical(1,0.5)&&!Decisions.critical(1,NaN),"critical sample boundaries do not invent a successful nonfinite roll");
    check(!Decisions.dodgeRequiresSample(0)&&!Decisions.dodgeRequiresSample(1)&&Decisions.dodgeRequiresSample(0.5),"only fractional dodge probabilities consume samples");
    var Leech=Java.loadClass("io.github.fatalitaya.combatnexus.combat.healing.internal.LifestealCalculation"),Status=Java.loadClass(root+"api.damage.DamageSettlementStatus"),Kind=Java.loadClass(root+"api.damage.DamageRequestKind");
    check(Leech.eligible(Status.APPLIED,false,Kind.PRIMARY,false,true,true,true),"complete primary health evidence permits lifesteal");
    [[true,Kind.PRIMARY,false,true,true,true],[false,Kind.REFLECTED,false,true,true,true],
     [false,Kind.PRIMARY,true,true,true,true],[false,Kind.PRIMARY,false,false,true,true],
     [false,Kind.PRIMARY,false,true,false,true],[false,Kind.PRIMARY,false,true,true,false]].forEach(function(row){
        check(!Leech.eligible(Status.APPLIED,row[0],row[1],row[2],row[3],row[4],row[5]),"incomplete cancelled or derived evidence excludes lifesteal: "+row);
    });
    check(Leech.amount(8,10,3,0.5)===1.5&&Leech.amount(2,10,8,0.5)===1&&Leech.amount(0,10,8,0.5)===0,"lifesteal caps to confirmed health loss and pre-hit health, excluding absorption");
    var Registration=Java.loadClass("probe.EffectExtension");
    check(Registration.driverCapacityRejected&&Registration.additionalDrivers===60,"startup accepts exactly 64 driver registrations and rejects overflow");
    check(Registration.executorCapacityRejected&&Registration.additionalExecutors===250,"startup accepts exactly 256 ability executors and rejects overflow");
    var victim = EntityType.COW.create(level);
    victim.setPos(0, 100, 0);
    level.addFreshEntity(victim);
    try {
        var source = Source.from(victim.damageSources().generic());
        var request = Request.builder(level, victim, source, 4).build();
        var before = victim.getHealth();
        var result = Api.submitDamage(request);
        check(String(result.status()) === "APPLIED", "normal damage applied");
        check(result.settlement().isPresent() && victim.getHealth() < before,
              "synchronous completed settlement and health loss");
        idle();
        var replay = Api.submitDamage(request);
        check(String(replay.status()) === "REJECTED", "duplicate request rejected");
        idle();
        victim.invulnerableTime = 0;
        var zero = Api.submitDamage(Request.builder(level, victim, source, 0).build());
        check(String(zero.status()) !== "APPLIED", "zero damage boundary");
        idle();
        var invalid = false;
        try { Request.builder(level, victim, source, -1).build(); }
        catch (error) { invalid = true; }
        check(invalid, "negative damage rejected");
        idle();
        var UUID=Java.loadClass("java.util.UUID"),Probe=Java.loadClass("probe.DefenseExtension"),Attributes=Java.loadClass("io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes"),NativeAttributes=Java.loadClass("io.github.fatalitaya.combatnexus.platform.NativeAttributeAccess");
        var player=new (Java.loadClass("net.neoforged.neoforge.common.util.FakePlayer"))(level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"CNDecisionSamples"));
        player.setPos(2,512,0);level.addNewPlayer(player);
        var chance=player.getAttribute(NativeAttributes.resolve(Attributes.critChance()));
        try {
            Probe.criticalTarget=victim.getUUID();
            [[0,Decision.DEFAULT,0],[0.5,Decision.DEFAULT,1],[1,Decision.DEFAULT,1],
             [1,Decision.FORCE_CRITICAL,0],[1,Decision.SUPPRESS_CRITICAL,0]].forEach(function(row){
                chance.setBaseValue(row[0]);Probe.criticalDecision=row[1];Probe.criticalNext=NaN;
                victim.setHealth(victim.getMaxHealth());victim.invulnerableTime=0;
                var expected=Java.loadClass("net.minecraft.util.RandomSource").create(7412);
                for(var n=0;n<row[2];n++)expected.nextFloat();
                var damage=Api.submitDamage(Request.builder(level,victim,source,1).logicalAttacker(player).build());
                check(damage.settlement().isPresent()&&Probe.criticalNext===expected.nextFloat()&&Probe.criticalSampled===(row[2]===1),
                    "real critical pipeline consumes exactly "+row[2]+" samples: "+row[0]+" "+row[1]);
            });
            Probe.criticalTarget=null;chance.setBaseValue(0);
            player.getAttribute(NativeAttributes.resolve(Attributes.lifesteal())).setBaseValue(0.5);
            var Access=Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.CombatAccess").find(level).orElseThrow(),Absorption=Java.loadClass("io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand");
            var absorptionSource=new (Java.loadClass("io.github.fatalitaya.combatnexus.combat.api.CombatSource"))(Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId").parse("probe:leech_absorption"),Java.loadClass("java.util.Optional").empty(),Java.loadClass("java.util.Optional").empty());
            var leech=function(kind,health,absorption){
                victim.setHealth(health);victim.invulnerableTime=0;player.setHealth(5);
                Access.absorption().submit(new Absorption(UUID.randomUUID(),victim.getUUID(),absorptionSource,Absorption.Operation.REMOVE,0,0));
                if(absorption>0)check(Access.absorption().submit(new Absorption(UUID.randomUUID(),victim.getUUID(),absorptionSource,Absorption.Operation.GRANT,absorption,absorption)).applied()&&victim.getAbsorptionAmount()===absorption,"native absorption fixture grants exact requested balance");
                return Api.submitDamage(Request.builder(level,victim,source,4).logicalAttacker(player).kind(kind).build());
            };
            var absorbed=leech(Kind.PRIMARY,10,8);
            check(absorbed.settlement().get().evidence().healthLoss()===0&&player.getHealth()===5,"absorption-only hit cannot grant lifesteal: loss="+absorbed.settlement().get().evidence().healthLoss()+" healer="+player.getHealth());
            leech(Kind.CUSTOM,10,0);check(player.getHealth()===5,"custom hit cannot grant primary lifesteal");
            [Kind.REFLECTED,Kind.DOT].forEach(function(kind){
                var rejected=false;
                try{leech(kind,10,0);}catch(error){
                    if(String(error).indexOf("IllegalArgumentException")<0||String(error).indexOf("requires a parent damage lineage")<0)throw error;
                    rejected=true;
                }
                check(rejected&&player.getHealth()===5,"parentless derived damage is rejected without lifesteal: "+kind);
            });
            var confirmed=leech(Kind.PRIMARY,10,0);
            check(confirmed.settlement().get().evidence().healthLoss()===4&&player.getHealth()===7,"primary hit heals exactly half its confirmed health loss");
            var lethal=leech(Kind.PRIMARY,2,0);
            check(lethal.settlement().get().evidence().healthLoss()===2&&player.getHealth()===6,"lethal overkill cannot award lifesteal beyond remaining health");
        } finally {Probe.criticalTarget=null;Probe.criticalDecision=Decision.DEFAULT;player.discard();}
    } finally { victim.discard(); }
    return JSON.stringify({passed: checks.length, checks: checks});
})()
