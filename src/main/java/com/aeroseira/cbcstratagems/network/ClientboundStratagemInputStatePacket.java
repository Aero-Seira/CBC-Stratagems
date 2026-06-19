package com.aeroseira.cbcstratagems.network;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputStatus;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundStratagemInputStatePacket(
        StratagemInputStatus status,
        List<StratagemCommand> input,
        Optional<ResourceLocation> selectedStratagem,
        Component message
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundStratagemInputStatePacket> TYPE =
            new CustomPacketPayload.Type<>(CBCStratagems.id("stratagem_input_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStratagemInputStatePacket> STREAM_CODEC = StreamCodec.composite(
            StratagemInputStatus.STREAM_CODEC,
            ClientboundStratagemInputStatePacket::status,
            StratagemCommand.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundStratagemInputStatePacket::input,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            ClientboundStratagemInputStatePacket::selectedStratagem,
            ComponentSerialization.STREAM_CODEC,
            ClientboundStratagemInputStatePacket::message,
            ClientboundStratagemInputStatePacket::new
    );

    public ClientboundStratagemInputStatePacket {
        input = List.copyOf(input);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
