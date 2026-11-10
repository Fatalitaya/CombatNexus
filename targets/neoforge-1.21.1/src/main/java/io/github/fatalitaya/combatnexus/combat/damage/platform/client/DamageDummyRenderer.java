package io.github.fatalitaya.combatnexus.combat.damage.platform.client;

import io.github.fatalitaya.combatnexus.CombatNexusCommon;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Entity-based renderer for the 1.21.1 model pipeline. */
public final class DamageDummyRenderer extends LivingEntityRenderer<DamageDummyEntity, DamageDummyModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(CombatNexusCommon.MOD_ID, "textures/entity/damage_dummy.png");
    public DamageDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new DamageDummyModel(context.bakeLayer(DamageDummyModel.LAYER_LOCATION)), 0.5F);
    }
    @Override protected boolean shouldShowName(DamageDummyEntity entity) { return false; }
    @Override public ResourceLocation getTextureLocation(DamageDummyEntity entity) { return TEXTURE; }
}
