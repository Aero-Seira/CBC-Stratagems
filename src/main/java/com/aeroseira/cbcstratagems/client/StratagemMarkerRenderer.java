package com.aeroseira.cbcstratagems.client;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.entity.StratagemMarkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class StratagemMarkerRenderer extends EntityRenderer<StratagemMarkerEntity> {
    private static final ResourceLocation TEXTURE = CBCStratagems.id("textures/entity/stratagem_marker.png");
    private static final int BEAM_COLOR = 0xFFFF3030;

    public StratagemMarkerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StratagemMarkerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BeaconRenderer.renderBeaconBeam(
                poseStack,
                bufferSource,
                BeaconRenderer.BEAM_LOCATION,
                partialTick,
                1.0F,
                entity.level().getGameTime(),
                0,
                256,
                BEAM_COLOR,
                0.16F,
                0.22F
        );
        renderMarkerText(entity, poseStack, bufferSource, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StratagemMarkerEntity entity) {
        return TEXTURE;
    }

    private void renderMarkerText(StratagemMarkerEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ResourceLocation stratagemId = entity.stratagemId();
        Component name = stratagemId == null
                ? Component.translatable("entity.cbc_stratagems.stratagem_marker")
                : StratagemClientState.get(stratagemId).map(summary -> summary.name()).orElse(Component.literal(stratagemId.toString()));
        int seconds = Math.max(0, (entity.remainingTicks() + 19) / 20);
        Component text = Component.translatable("overlay.cbc_stratagems.marker", name, seconds);

        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();
        Font font = this.getFont();
        float x = -font.width(text) / 2.0F;
        int background = (int)(Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0.0F, 0xFFFFFFFF, false, matrix, bufferSource, Font.DisplayMode.NORMAL, background, packedLight);
        poseStack.popPose();
    }
}
