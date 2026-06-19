package com.aeroseira.cbcstratagems.network;

import com.aeroseira.cbcstratagems.CBCStratagems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundStratagemInputControlPacket(boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundStratagemInputControlPacket> TYPE =
            new CustomPacketPayload.Type<>(CBCStratagems.id("stratagem_input_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundStratagemInputControlPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ServerboundStratagemInputControlPacket::active,
            ServerboundStratagemInputControlPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
