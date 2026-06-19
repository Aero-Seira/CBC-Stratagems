package com.aeroseira.cbcstratagems.network;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinitionSummary;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundStratagemDefinitionsPacket(List<StratagemDefinitionSummary> definitions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundStratagemDefinitionsPacket> TYPE =
            new CustomPacketPayload.Type<>(CBCStratagems.id("stratagem_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStratagemDefinitionsPacket> STREAM_CODEC =
            StratagemDefinitionSummary.STREAM_CODEC
                    .apply(ByteBufCodecs.list())
                    .map(ClientboundStratagemDefinitionsPacket::new, ClientboundStratagemDefinitionsPacket::definitions);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
