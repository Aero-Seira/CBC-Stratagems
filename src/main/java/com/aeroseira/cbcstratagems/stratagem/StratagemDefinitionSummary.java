package com.aeroseira.cbcstratagems.stratagem;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record StratagemDefinitionSummary(
        ResourceLocation id,
        Component name,
        ResourceLocation icon,
        List<StratagemCommand> command,
        long cooldownTicks,
        int countdownTicks
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StratagemDefinitionSummary> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            StratagemDefinitionSummary::id,
            ComponentSerialization.STREAM_CODEC,
            StratagemDefinitionSummary::name,
            ResourceLocation.STREAM_CODEC,
            StratagemDefinitionSummary::icon,
            StratagemCommand.STREAM_CODEC.apply(ByteBufCodecs.list()),
            StratagemDefinitionSummary::command,
            ByteBufCodecs.VAR_LONG,
            StratagemDefinitionSummary::cooldownTicks,
            ByteBufCodecs.VAR_INT,
            StratagemDefinitionSummary::countdownTicks,
            StratagemDefinitionSummary::new
    );

    public StratagemDefinitionSummary {
        command = List.copyOf(command);
    }
}
