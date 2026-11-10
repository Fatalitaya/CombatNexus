package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.provider;

import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.*;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.fatalitaya.combatnexus.combat.damage.core.rule.StackEntryCollector;
import java.util.List;

/** Uses the selected damage facts frozen at launch, including their installation identity. */
public final class ProjectileDamageRuleProvider implements DamageRuleProvider {
    @Override public void collect(NativeDamageRuleContext context,DamagePhase phase,List<RuntimeDamageRule> out){
        var ctx=DamageInternalContexts.facts(context,"projectile rule provider");
        var profile=ctx.vanillaSourceProfile();
        if(profile==null||!profile.projectile()||!profile.directProjectile())return;
        var launch=ctx.projectileLaunch();
        if(launch.isEmpty()||launch.get().weapon().isEmpty())return;
        var weapon=launch.get().weapon();var owner=ctx.equipmentOwner();
        var execution=RuleExecutionContext.projectileSource(DamageRuleRole.OFFENSIVE,owner==null?null:owner.getId(),weapon.itemId(),profile.directAttackerId());
        StackEntryCollector.collectStackEntries(ctx,phase,out,weapon,execution,"captured_projectile_damage");
    }
}
