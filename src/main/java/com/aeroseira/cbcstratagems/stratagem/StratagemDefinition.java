package com.aeroseira.cbcstratagems.stratagem;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
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
        StratagemFirePlan firePlan
) {
    public static DataResult<StratagemDefinition> decode(ResourceLocation id, JsonElement json) {
        return JsonDefinition.CODEC.parse(JsonOps.INSTANCE, json).flatMap(definition -> definition.bind(id));
    }

    public StratagemDefinition {
        command = List.copyOf(command);
        firePlan = firePlan.normalized();
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
            Optional<StratagemFirePlan> firePlan,
            Optional<List<StratagemArtilleryEntry>> artillery
    ) {
        private static final com.mojang.serialization.Codec<JsonDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(JsonDefinition::name),
                ResourceLocation.CODEC.fieldOf("icon").forGetter(JsonDefinition::icon),
                StratagemCommand.CODEC.listOf().fieldOf("command").forGetter(JsonDefinition::command),
                com.mojang.serialization.Codec.LONG.optionalFieldOf("cooldown_ticks", 0L).forGetter(JsonDefinition::cooldownTicks),
                com.mojang.serialization.Codec.INT.optionalFieldOf("countdown_ticks", 0).forGetter(JsonDefinition::countdownTicks),
                StratagemFirePlan.CODEC.optionalFieldOf("fire_plan").forGetter(JsonDefinition::firePlan),
                StratagemArtilleryEntry.CODEC.listOf().optionalFieldOf("artillery").forGetter(JsonDefinition::artillery)
        ).apply(instance, JsonDefinition::new));

        private DataResult<StratagemDefinition> bind(ResourceLocation id) {
            if (firePlan.isPresent() && artillery.isPresent()) {
                return DataResult.error(() -> "stratagem must not define both fire_plan and legacy artillery");
            }
            if (firePlan.isEmpty() && artillery.isEmpty()) {
                return DataResult.error(() -> "stratagem must define fire_plan or legacy artillery");
            }

            StratagemFirePlan resolvedFirePlan = firePlan
                    .orElseGet(() -> StratagemFirePlan.legacy(artillery.orElseThrow()));
            return DataResult.success(new StratagemDefinition(id, name, icon, command, cooldownTicks, countdownTicks, resolvedFirePlan));
        }
    }
}
