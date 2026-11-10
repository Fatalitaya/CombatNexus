package io.github.fatalitaya.combatnexus.combat.damage.platform.client.damage;

import io.github.fatalitaya.combatnexus.combat.damage.client.damage.DamageNumberFormatter;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;

public final class DamageNumberRenderer {

    private DamageNumberRenderer() {
    }

    public static void render(
            Camera camera, PoseStack poseStack
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.font == null) {
            return;
        }

        if (ClientDamageNumberManager.active().isEmpty()) {
            return;
        }

        float partialTick = minecraft.getTimer()
                .getGameTimeDeltaPartialTick(false);

        poseStack.pushPose();
        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(256);
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(byteBufferBuilder);
        try {
            for (FloatingDamageNumber number
                    : ClientDamageNumberManager.active()) {
                poseStack.pushPose();
                poseStack.translate(
                        number.renderX() - camera.getPosition().x,
                        number.renderY(partialTick) - camera.getPosition().y,
                        number.renderZ() - camera.getPosition().z
                );
                poseStack.mulPose(camera.rotation());
                float worldScale = number.worldScale(partialTick);
                poseStack.scale(
                        worldScale,
                        -worldScale,
                        worldScale
                );

                String text = DamageNumberFormatter.format(number.damage());
                float x = -minecraft.font.width(text) / 2.0F;
                int alpha = Math.round(
                        number.alpha(partialTick) * 255.0F
                );
                int color = (alpha << 24)
                        | (number.critical() ? 0xFFD34E : 0xFFFFFF);

                minecraft.font.drawInBatch(
                        text,
                        x,
                        0.0F,
                        color,
                        true,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        LightTexture.FULL_BRIGHT
                );
                poseStack.popPose();
            }
            bufferSource.endBatch();
        } finally {
            byteBufferBuilder.close();
        }
        poseStack.popPose();
    }
}
