package com.aeroseira.cbcstratagems.stratagem;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class StratagemRegistry {
    private static volatile Map<ResourceLocation, StratagemDefinition> definitions = Map.of();

    private StratagemRegistry() {
    }

    public static void replaceDefinitions(Map<ResourceLocation, StratagemDefinition> newDefinitions) {
        List<StratagemDefinition> sortedDefinitions = newDefinitions.values()
                .stream()
                .sorted(Comparator.comparing(StratagemDefinition::id, ResourceLocation::compareNamespaced))
                .toList();

        Map<ResourceLocation, StratagemDefinition> next = new LinkedHashMap<>();
        for (StratagemDefinition definition : sortedDefinitions) {
            next.put(definition.id(), definition);
        }
        definitions = Collections.unmodifiableMap(next);
    }

    public static Optional<StratagemDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return definitions.containsKey(id);
    }

    public static Collection<StratagemDefinition> definitions() {
        return definitions.values();
    }

    public static List<StratagemDefinitionSummary> summaries() {
        return definitions.values().stream().map(StratagemDefinition::summary).toList();
    }
}
