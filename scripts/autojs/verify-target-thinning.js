// Pure values and service-state regression, executed in the real AutoJS/Rhino environment.
(function () {
    function cls(n) { return Java.loadClass("io.github.fatalitaya.combatnexus." + n); }
    var checks=[];
    function check(v,n) { if(!v)throw new Error(n); checks.push(n); }
    function rejects(f,type,message) {
        try { f(); return false; } catch(e) {
            var cause=e.javaException || e;
            if(!cause || !(cause instanceof Java.loadClass("java.lang."+type)) || String(cause)!=="java.lang."+type+": "+message) throw e;
            return true;
        }
    }
    var Id=cls("api.id.NexusId"), List=Java.loadClass("java.util.List"), Set=Java.loadClass("java.util.Set");
    var Optional=Java.loadClass("java.util.Optional"), OL=Java.loadClass("java.util.OptionalLong"), UUID=Java.loadClass("java.util.UUID");
    var Parser=Java.loadClass("com.google.gson.JsonParser"), Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
    var Codecs=cls("api.codec.CodecBudgets"), Limits=cls("combat.damage.api.rule.DamageRuleLimits"), Codec=Java.loadClass("com.mojang.serialization.Codec");
    [Codecs,Limits].forEach(function(c) {
        var strings=c.boundedString(2,"probe");
        check(strings.parse(Ops,Parser.parseString('"😀😀"')).result().isPresent(),"two supplementary code points accepted: "+c);
        check(strings.parse(Ops,Parser.parseString('"😀😀x"')).error().isPresent(),"Unicode overflow refused: "+c);
        var lists=c.boundedList(Codec.INT,2,"probe");
        check(lists.parse(Ops,Parser.parseString('[1,2]')).result().isPresent(),"list at limit accepted: "+c);
        check(lists.parse(Ops,Parser.parseString('[1,2,3]')).error().isPresent(),"list overflow refused: "+c);
        var guarded=c.guardRawStructure(Codec.PASSTHROUGH,"probe",3,5);
        check(guarded.parse(Ops,Parser.parseString('[[0]]')).result().isPresent(),"depth boundary accepted: "+c);
        check(String(guarded.parse(Ops,Parser.parseString('[[[0]]]')).error().orElseThrow().message()).indexOf('raw_depth=4 maximum=3')>=0,"depth overflow preflight: "+c);
        check(guarded.parse(Ops,Parser.parseString('[0,1,2,3]')).result().isPresent(),"node boundary accepted: "+c);
        check(String(guarded.parse(Ops,Parser.parseString('[0,1,2,3,4]')).error().orElseThrow().message()).indexOf('raw_nodes=6 maximum=5')>=0,"node overflow preflight: "+c);
        check(String(c.guardRawStructure(Codec.INT,"probe",1,5).parse(Ops,Parser.parseString('[0]')).error().orElseThrow().message()).indexOf('rejected before decode')>=0,"raw rejection precedes wrong leaf codec: "+c);
        check(c.guardRawStructure(Codec.INT,"probe",0,5).parse(Ops,Parser.parseString('1')).error().isPresent(),"nonpositive structural budget refused: "+c);
    });
    var Context=cls("combat.damage.api.rule.RuleExecutionContext"), Role=cls("combat.damage.api.rule.DamageRuleRole"), Location=cls("combat.damage.api.rule.RuleSourceLocation");
    var Slots=cls("combat.damage.api.rule.DamageRuleSourceFilter"), Pos=cls("api.equipment.EquipmentPosition");
    var hand=Context.itemEquipment(Location.ATTACKER_MAINHAND,Role.OFFENSIVE,null,null,Pos.MAINHAND);
    check(hand.sourceItemId().equals(Context.EMPTY_ITEM_ID),"null item keeps empty identity default");
    check(hand["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.ITEM)&&hand["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.WEAPON)&&!hand["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.ARMOR),"hand damage contribution source matching");
    var bad=Context.itemEquipment(Location.PROJECTILE,Role.OFFENSIVE,null,null,Pos.MAINHAND);
    check(!bad.hasConsistentBuiltInSource()&&!bad["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.ITEM),"contradictory builtin source cannot match");
    check(rejects(function(){Context.itemEquipment(Location.EXTERNAL,Role.OFFENSIVE,null,null,Pos.MAINHAND);},"IllegalArgumentException","External item execution contexts require complete external source identity"),"partial external identity rejected");
    var Category=cls("combat.damage.api.rule.source.EquippedItemRuleSourceCategory");
    var ext=Context.externalItemSource(Role.OFFENSIVE,null,null,Id.parse("probe:provider"),Id.parse("probe:item"),Id.parse("probe:slot"),Category.PROJECTILE);
    check(ext["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.ITEM)&&ext["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.WEAPON)&&ext["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.PROJECTILE)&&!ext["matches(io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleSourceFilter)"](Slots.ARMOR),"external projectile category matching");
    var Kind=cls("combat.damage.api.damage.DamageRequestKind"), Value=cls("combat.damage.api.display.phrase.RequestKindValue"), Phrases=cls("combat.damage.api.display.phrase.DamageModuleRulePhrases");
    check(new Value(Kind.values()[0]).kind()===Kind.values()[0]&&Phrases.REQUEST_KIND.required(),"request-kind phrase value and required slot preserved");
    check(rejects(function(){new Value(null);},"NullPointerException","kind"),"null request kind refused");
    var Formatter=cls("presentation.internal.AttributeRowFormatter"), row=Formatter.value(Id.parse("probe:attr"),1,2,"probe.description","",Optional.empty());
    check(!row.has("reserved")&&String(row.get("unit").getAsString())===""&&row.get("value").getAsDouble()===2,"attribute row optional metadata defaults");
    Formatter.effective(row,2,3,Id.parse("probe:reason"));
    check(row.get("raw").getAsDouble()===2&&row.get("value").getAsDouble()===3,"effective display changes formatting only");

    ["AttackerHealthAboveCondition","AttackerHealthBelowCondition","TargetHealthAboveCondition","TargetHealthBelowCondition"].forEach(function(name){
        var Condition=cls("combat.damage.builtin.rule.condition."+name);
        [0,.5,1].forEach(function(value){
            check(new Condition(value).threshold()===value,name+" Java accepts "+value);
            check(Condition.CODEC.codec().parse(Ops,Parser.parseString('{"threshold":'+value+'}')).result().isPresent(),name+" codec accepts "+value);
        });
        [-.1,1.1,NaN,Infinity,-Infinity].forEach(function(value){
            check(rejects(function(){new Condition(value);},"IllegalArgumentException","Health ratio must be finite and in [0,1]"),name+" Java rejects "+value);
            var json=new (Java.loadClass("com.google.gson.JsonObject"))();json.addProperty("threshold",Java.loadClass("java.lang.Float").valueOf(value));
            check(Condition.CODEC.codec().parse(Ops,json).error().isPresent(),name+" codec rejects "+value);
        });
    });
    var Op=cls("combat.damage.api.rule.DamageRuleOperation"),NumericLimit=cls("combat.damage.api.rule.DamageRuleOperation$NumericLimit");
    var RuleDef=cls("combat.damage.api.rule.DamageRuleDefinition"),Phase=cls("combat.damage.api.enums.DamagePhase"),BudgetRole=cls("combat.damage.api.rule.DamageRuleRole"),Stacking=cls("combat.damage.api.rule.DamageRuleStacking");
    function customRule(value,category){
        var operation=new (Java.loadClass("probe.BudgetOperation"))(value,category);
        return new RuleDef(Id.parse("probe:custom"),BudgetRole.OFFENSIVE,Phase.BASE_MODIFICATION,0,List.of(),List.of(operation),Stacking.STACK,Optional.empty());
    }
    check(!Limits.findRuleProblem(customRule(101,NumericLimit.DAMAGE)).isPresent(),"custom ordinary operation retains damage budget");
    check(!Limits.findRuleProblem(customRule(100,NumericLimit.MULTIPLIER)).isPresent(),"custom multiplier boundary accepted");
    check(String(Limits.findRuleProblem(customRule(101,NumericLimit.MULTIPLIER)).orElse("")).indexOf("operation_value_out_of_range")===0,"custom multiplier declaration controls budget");
    check(String(Limits.findRuleProblem(customRule(NaN,NumericLimit.DAMAGE)).orElse("")).indexOf("non_finite_operation_value")===0,"custom operation finite check retained");
    var target=UUID.randomUUID(), calls=[],alive=true,natural=true,hook=null,readHook=null;
    function source(n) { return new (cls("combat.api.CombatSource"))(Id.parse("probe:"+n),Optional.empty(),Optional.empty()); }
    var Conditions=cls("combat.regeneration.spi.RegenerationConditions"), State=cls("combat.regeneration.spi.RegenerationConditions$State");
    var Scheduler=cls("combat.regeneration.internal.DefaultRegenerationScheduler"), Plan=cls("combat.regeneration.api.RegenerationPlan"), Def=cls("combat.regeneration.api.RegenerationDefinition");
    var businessTick=0;
    function advance(t, callback) { businessTick=t; scheduler.tick(t, callback); }
    var scheduler=new Scheduler(new Conditions({read:function(){if(readHook)readHook();return new State(alive,natural);}}),function(){return businessTick;});
    var healing=new (cls("combat.healing.api.HealingService"))({submit:function(r){calls.push(String(r.source().id()));if(hook)hook(r);return new (cls("combat.healing.api.HealingSettlement"))(r,r.amount(),0,0,cls("combat.healing.api.HealingSettlement").Outcome.CANCELLED,cls("combat.api.ApplicationJournal").EMPTY);}});
    function plan(n,rate) { return new Plan(target,source(n),new Def(rate,0,true,false,1200000),OL.empty()); }
    function ticks(a,b) { for(var t=a;t<=b;t++)advance(t,function(r){healing.submit(r);}); }
    function saved(n,now) { return scheduler.save(target,Set.of(Id.parse("probe:"+n)),now).get(0); }
    try {
        scheduler.replace(plan("a",2));ticks(0,6);
        check(saved("a",6).ticks()===7&&Math.abs(saved("a",6).accrued()-.7)<1e-8,"seven eligible ticks retain fractional progress");
        var progress=scheduler.save(target,Set.of(Id.parse("probe:a")),6);scheduler.reset();businessTick=100;scheduler.restore(target,progress,Set.of(Id.parse("probe:a")));advance(100,function(r){healing.submit(r);});
        check(calls.length===0&&saved("a",100).ticks()===7,"restored recovery preserves next opportunity delay");ticks(101,113);
        check(calls.length===1&&saved("a",113).ticks()===0,"saved fraction resumes at twentieth eligible tick");
        advance(113,function(r){healing.submit(r);});check(calls.length===1,"duplicate recovery tick ignored");
        check(rejects(function(){advance(112,function(r){healing.submit(r);});},"IllegalArgumentException","Clock moved backwards; reset the world session first"),"recovery clock reversal refused");
        advance(10000,function(r){healing.submit(r);});check(saved("a",10000).ticks()===1,"clock jump adds one eligible tick");
        alive=false;advance(10001,function(r){healing.submit(r);});check(saved("a",10001).ticks()===0,"death clears accrued progress");
        alive=true;advance(10002,function(r){healing.submit(r);});natural=false;advance(10003,function(r){healing.submit(r);});check(saved("a",10003).ticks()===0,"disabled rule clears accrued progress");natural=true;
        scheduler.reset();calls=[];scheduler.replace(plan("a",2));scheduler.replace(plan("b",2));ticks(0,18);
        hook=function(){scheduler.replace(plan("b",4));};advance(19,function(r){healing.submit(r);});hook=null;
        check(calls.join(",")==="probe:a"&&saved("b",19).ticks()===0,"callback replaces later source before its result is computed");
        ticks(20,39);check(calls.join(",")==="probe:a,probe:a,probe:b","replacement retains stable order and full new installment");
        scheduler.reset();calls=[];scheduler.replace(plan("a",2));scheduler.replace(plan("b",2));ticks(0,18);
        hook=function(){throw new (Java.loadClass("java.lang.IllegalStateException"))("intentional healing failure");};
        advance(19,function(r){healing.submit(r);});hook=null;
        check(saved("a",19).ticks()===0&&saved("b",19).ticks()===0,"failed callback does not block later installment");
        advance(20,function(r){healing.submit(r);});check(calls.join(",")==="probe:a,probe:b","next tick does not retry consumed installments");
        scheduler.reset();calls=[];scheduler.replace(plan("a",2));scheduler.replace(plan("b",2));ticks(0,18);
        var reads=0;readHook=function(){if(++reads===1)throw new (Java.loadClass("java.lang.IllegalStateException"))("intentional condition failure");};
        advance(19,function(r){healing.submit(r);});readHook=null;
        check(saved("a",19).ticks()===19&&Math.abs(saved("a",19).accrued()-1.9)<1e-8&&calls.join(",")==="probe:b","condition failure preserves fraction and later source executes");
        advance(20,function(r){healing.submit(r);});check(calls.join(",")==="probe:b,probe:a","condition failure contributes no tick and resumes once");
        advance(20,function(r){healing.submit(r);});advance(21,function(r){healing.submit(r);});check(calls.length===2,"failed-read recovery is same-tick idempotent without retry burst");
        scheduler.reset();for(var i=0;i<4096;i++)scheduler.replace(plan("capacity_"+i,1));
        check(scheduler.snapshot(target).size()===4096,"recovery capacity boundary accepted");
        check(rejects(function(){scheduler.replace(plan("overflow",1));},"IllegalStateException","Recovery plan capacity exceeded"),"recovery capacity overflow refused");
        scheduler.replace(plan("capacity_0",2));check(scheduler.snapshot(target).size()===4096,"replacement at capacity accepted");
    } finally { scheduler.close(); }
    var Layout=cls("combat.damage.core.PreMultiplierLayout"), Pre=cls("combat.damage.core.PreMultiplierSet");
    var ids=new (Java.loadClass("java.util.ArrayList"))();ids.add(Id.parse("probe:first"));ids.add(Id.parse("probe:second"));
    var layout=new Layout(ids);ids.clear();
    check(layout.size()===2&&layout.indexOf(Id.parse("probe:second"))===1&&layout.indexOf(Id.parse("probe:missing"))===-1,"bucket layout freezes order and indices");
    var pre=new Pre(layout);pre.add(0,.5);pre.add(0,.5);pre.add(1,2);
    check(pre.apply(2)===12,"same bucket adds before ordered bucket multiplication");
    check(rejects(function(){pre.add(2,0);},"IndexOutOfBoundsException","Invalid pre-multiplier bucket id: 2"),"invalid index rejected even for zero contribution");
    var Registry=cls("combat.damage.core.registry.PreMultiplierBucketRegistry");
    check(Registry.isFrozen()&&Registry.layout().size()===Registry.bucketCount(),"platform publishes frozen layout");
    check(rejects(function(){Registry.getPreMultiplierBucketId(Id.parse("probe:missing"));},"IllegalArgumentException","Unknown pre-multiplier bucket: probe:missing"),"platform refuses unknown bucket");
    var Damage=cls("combat.damage.core.DamageComponent"),Channel=cls("combat.damage.api.enums.DamageChannel"),Bucket=cls("combat.damage.api.enums.DamageApplicationBucket");
    var damage=new Damage(new Channel(Id.parse("probe:channel"),0),layout);
    damage.addBase(Bucket.DN_RULE_BASE,10);damage.addBase(Bucket.DN_TRUE_DAMAGE,4);damage.addPreMultiplier(0,1);damage.addApplicationPreMultiplier(Bucket.DN_TRUE_DAMAGE,1,.5);damage.addPostMultiplier(.5);damage.addMitigation(.5);damage.addTemporaryResistance(3);
    damage.calculateFinalOffensive(null,new (Java.loadClass("it.unimi.dsi.fastutil.floats.FloatArrayList"))());damage.calculateFinalDefensive(new (Java.loadClass("it.unimi.dsi.fastutil.floats.FloatArrayList"))());
    check(damage.getFinalizedOffensiveAmount()===36&&damage.getPostMitigationAmount()===21,"mixed regular and true damage retain multiplier and mitigation ownership");
    var checkpoint=damage.checkpoint();damage.addBase(100);damage.addPreMultiplier(1,9);damage.addTemporaryResistance(8);damage.restore(checkpoint);
    damage.calculateFinalOffensive(null,new (Java.loadClass("it.unimi.dsi.fastutil.floats.FloatArrayList"))());damage.calculateFinalDefensive(new (Java.loadClass("it.unimi.dsi.fastutil.floats.FloatArrayList"))());
    check(damage.getBaseAmount()===14&&damage.getPostMitigationAmount()===21&&damage.getTemporaryResistanceRating()===3,"checkpoint restores all damage state and multiplier layout");
    var Selection=cls("effect.internal.EffectLayerSelection"),Candidate=cls("effect.internal.EffectLayerSelection$Candidate");
    var selected=Selection.select(List.of(new Candidate(2,20),new Candidate(2,30),new Candidate(2,30),new Candidate(1,25),new Candidate(0,40)),10);
    check(String(selected)==="[1, 4]","same level selects later expiry then first tie; covered hidden layer discarded");
    check(String(Selection.select(List.of(new Candidate(2,20),new Candidate(0,40)),20))==="[1]","expired strongest layer exposes unexpired hidden layer");
    check(Selection.rebase(OL.empty(),5,10).isEmpty()&&Selection.rebase(OL.of(20),5,100).getAsLong()===115,"permanent and timed rebase arithmetic preserved");
    var Numerical=cls("combat.healing.internal.HealingNumericalState"),Rule=cls("combat.healing.api.HealingRuleContribution"),numeric=new Numerical(1);
    numeric.reduce(List.of(new Rule(Id.parse("probe:a"),-5,2),new Rule(Id.parse("probe:b"),6,3)));
    check(numeric.amount()===12&&numeric.contributions().get(0).after()===-4&&numeric.contributions().get(2).after()===2&&numeric.contributions().get(3).after()===4,"healing sums signed additions then clamps then multiplies in order");
    var numericCheckpoint=numeric.checkpoint();numeric.reduce(List.of(new Rule(Id.parse("probe:negative"),-100,3)));
    check(numeric.amount()===0,"negative healing total clamps before multiplier");numeric.restore(numericCheckpoint);
    check(numeric.amount()===12&&numeric.contributions().size()===5,"healing checkpoint restores numerical explanation");
    var Store=cls("combat.state.internal.EntityScheduleStore"),Task=cls("combat.api.scheduling.ScheduledTask"),Ignite=cls("combat.api.scheduling.ScheduledIntent$Ignite");
    var store=new Store(),sourceValue=new (cls("combat.api.CombatSource"))(Id.parse("probe:schedule"),Optional.empty(),Optional.empty());
    function task(persistent){return new Task(UUID.randomUUID(),sourceValue,Optional.empty(),100,2,3,new Ignite(4),Set.of(),persistent);}
    var original=task(true);store.add(original);
    check(rejects(function(){store.add(original);},"IllegalArgumentException","Duplicate scheduled identity/capacity"),"duplicate schedule identity refused");
    var encoded=String(store.encode(false));
    check(rejects(function(){store.restore('['+encoded.slice(1,-1)+','+encoded.slice(1,-1)+']');},"IllegalArgumentException","Duplicate scheduled identity/capacity"),"duplicate restored identities rejected");
    check(store.snapshot().size()===1&&store.snapshot().get(0).equals(original),"failed restore preserves existing schedules");
    store.add(task(false));var restored=new Store();restored.restore(String(store.encode(true)));
    check(restored.snapshot().size()===1&&restored.snapshot().get(0).equals(original),"persistent filtering and codec preserve all task values");
    check(store.remove(original.id())&&!store.remove(original.id()),"schedule removal idempotent");
    for(var n=1;n<64;n++)store.add(task(false));
    check(rejects(function(){store.add(task(false));},"IllegalArgumentException","Duplicate scheduled identity/capacity"),"schedule capacity refused");
    store.restore(encoded);check(store.snapshot().size()===1&&store.snapshot().get(0).equals(original),"successful restore replaces entire store");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
