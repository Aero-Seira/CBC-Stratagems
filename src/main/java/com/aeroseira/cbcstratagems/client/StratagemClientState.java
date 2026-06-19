package com.aeroseira.cbcstratagems.client;

import com.aeroseira.cbcstratagems.stratagem.StratagemDefinitionSummary;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class StratagemClientState {
    private static volatile Map<ResourceLocation, StratagemDefinitionSummary> definitions = Map.of();

    private StratagemClientState() {
    }

    public static void replaceDefinitions(List<StratagemDefinitionSummary> newDefinitions) {
        Map<ResourceLocation, StratagemDefinitionSummary> next = new LinkedHashMap<>();
        for (StratagemDefinitionSummary definition : newDefinitions) {
            next.put(definition.id(), definition);
        }
        definitions = Collections.unmodifiableMap(next);
    }

    public static Collection<StratagemDefinitionSummary> definitions() {
        return definitions.values();
    }

    public static Optional<StratagemDefinitionSummary> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }
}
