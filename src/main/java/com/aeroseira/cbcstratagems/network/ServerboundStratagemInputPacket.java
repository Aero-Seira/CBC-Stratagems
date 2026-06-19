package com.aeroseira.cbcstratagems.network;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerboundStratagemInputPacket(StratagemCommand direction) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundStratagemInputPacket> TYPE =
            new CustomPacketPayload.Type<>(CBCStratagems.id("stratagem_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundStratagemInputPacket> STREAM_CODEC = StreamCodec.composite(
            StratagemCommand.STREAM_CODEC,
            ServerboundStratagemInputPacket::direction,
            ServerboundStratagemInputPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
