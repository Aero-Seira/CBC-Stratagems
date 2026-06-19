package com.aeroseira.cbcstratagems.player;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record PlayerStratagemCooldown(ResourceLocation stratagemId, long endTime) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerStratagemCooldown> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            PlayerStratagemCooldown::stratagemId,
            ByteBufCodecs.VAR_LONG,
            PlayerStratagemCooldown::endTime,
            PlayerStratagemCooldown::new
    );
}
