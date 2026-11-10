package io.github.fatalitaya.combatnexus.compat.apotheosis;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.*;
import net.minecraft.world.entity.projectile.Projectile;

/** Native projectile multiplier joins the frozen offensive total before mitigation. */
public record ProjectileDamage(int multiplierBucket) implements DamagePhaseProcessor {
    public static final NexusId ID = NexusId.parse("apothic_attributes:projectile_damage");
    @Override public DamagePhase phase() { return DamagePhase.GLOBAL_ADJUSTMENT; }
    @Override public void apply(NativeDamageRuleContext context) {
        if (!(context.directEntity() instanceof Projectile)) return;
        var values = context.projectileLaunch().map(launch -> launch.shooter().attributeSnapshot())
                .or(() -> context.logicalAttacker() == null ? java.util.Optional.empty()
                        : context.attributeSnapshot(context.logicalAttacker().getUUID()));
        if (values.isEmpty()) return;
        float delta = values.orElseThrow().valueOrZero(ID) - 1;
        for (var bucket : DamageApplicationBucket.values()) {
            var result = context.tryAddApplicationPreMultiplier(bucket, multiplierBucket, delta, ID.toString());
            context.recordContribution(result, new VanillaContributionFact(VanillaContributionFact.Kind.MULTIPLIER,
                    ID, DamageContributionSourceKind.JAVA_API, phase(), null, bucket, ID, delta,
                    ID.toString(), DamageContributionStatus.APPLIED));
        }
    }
}
