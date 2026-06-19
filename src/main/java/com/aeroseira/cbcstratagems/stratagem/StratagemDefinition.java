package com.aeroseira.cbcstratagems.stratagem;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public record StratagemDefinition(
        ResourceLocation id,
        Component name,
        ResourceLocation icon,
        List<StratagemCommand> command,
        long cooldownTicks,
        int countdownTicks,
        List<StratagemArtilleryEntry> artillery
) {
    public static DataResult<StratagemDefinition> decode(ResourceLocation id, JsonElement json) {
        return JsonDefinition.CODEC.parse(JsonOps.INSTANCE, json).map(definition -> definition.bind(id));
    }

    public StratagemDefinition {
        command = List.copyOf(command);
        artillery = List.copyOf(artillery);
    }

    public StratagemDefinitionSummary summary() {
        return new StratagemDefinitionSummary(id, name, icon, command, cooldownTicks, countdownTicks);
    }

    private record JsonDefinition(
            Component name,
            ResourceLocation icon,
            List<StratagemCommand> command,
            long cooldownTicks,
            int countdownTicks,
            List<StratagemArtilleryEntry> artillery
    ) {
        private static final com.mojang.serialization.Codec<JsonDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(JsonDefinition::name),
                ResourceLocation.CODEC.fieldOf("icon").forGetter(JsonDefinition::icon),
                StratagemCommand.CODEC.listOf().fieldOf("command").forGetter(JsonDefinition::command),
                com.mojang.serialization.Codec.LONG.optionalFieldOf("cooldown_ticks", 0L).forGetter(JsonDefinition::cooldownTicks),
                com.mojang.serialization.Codec.INT.optionalFieldOf("countdown_ticks", 0).forGetter(JsonDefinition::countdownTicks),
                StratagemArtilleryEntry.CODEC.listOf().fieldOf("artillery").forGetter(JsonDefinition::artillery)
        ).apply(instance, JsonDefinition::new));

        private StratagemDefinition bind(ResourceLocation id) {
            return new StratagemDefinition(id, name, icon, command, cooldownTicks, countdownTicks, artillery);
        }
    }
}
