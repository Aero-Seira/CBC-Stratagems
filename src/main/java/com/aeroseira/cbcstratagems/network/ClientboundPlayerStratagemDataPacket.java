package com.aeroseira.cbcstratagems.network;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.player.PlayerStratagemCooldown;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundPlayerStratagemDataPacket(
        List<ResourceLocation> unlockedStratagems,
        List<PlayerStratagemCooldown> cooldowns
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundPlayerStratagemDataPacket> TYPE =
            new CustomPacketPayload.Type<>(CBCStratagems.id("player_stratagem_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerStratagemDataPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundPlayerStratagemDataPacket::unlockedStratagems,
            PlayerStratagemCooldown.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ClientboundPlayerStratagemDataPacket::cooldowns,
            ClientboundPlayerStratagemDataPacket::new
    );

    public ClientboundPlayerStratagemDataPacket {
        unlockedStratagems = List.copyOf(unlockedStratagems);
        cooldowns = List.copyOf(cooldowns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
