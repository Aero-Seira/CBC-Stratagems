package com.aeroseira.cbcstratagems.client;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.entity.StratagemMarkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

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
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StratagemMarkerEntity entity) {
        return TEXTURE;
    }
}
