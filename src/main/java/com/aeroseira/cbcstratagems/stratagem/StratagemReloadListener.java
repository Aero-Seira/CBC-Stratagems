package com.aeroseira.cbcstratagems.stratagem;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.network.ClientboundStratagemDefinitionsPacket;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StratagemReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = CBCStratagems.MOD_ID + "/stratagems";

    public StratagemReloadListener() {
        super(GSON, DIRECTORY);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StratagemReloadListener::addReloadListener);
        NeoForge.EVENT_BUS.addListener(StratagemReloadListener::onDatapackSync);
    }

    private static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new StratagemReloadListener());
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        ClientboundStratagemDefinitionsPacket payload = new ClientboundStratagemDefinitionsPacket(StratagemRegistry.summaries());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, StratagemDefinition> loaded = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
            ResourceLocation id = entry.getKey();
            StratagemDefinition.decode(id, entry.getValue())
                    .resultOrPartial(message -> CBCStratagems.LOGGER.error("Failed to parse stratagem {}: {}", id, message))
                    .ifPresent(definition -> {
                        List<String> definitionErrors = StratagemValidator.validateDefinition(definition);
                        if (definitionErrors.isEmpty()) {
                            loaded.put(id, definition);
                        } else {
                            for (String error : definitionErrors) {
                                CBCStratagems.LOGGER.error("Invalid stratagem {}: {}", id, error);
                            }
                        }
                    });
        }

        List<String> globalErrors = StratagemValidator.validateNoPrefixConflicts(loaded.values());
        if (!globalErrors.isEmpty()) {
            for (String error : globalErrors) {
                CBCStratagems.LOGGER.error("Invalid stratagem command set: {}", error);
            }
            StratagemRegistry.replaceDefinitions(Map.of());
            CBCStratagems.LOGGER.error("Loaded 0 stratagem definitions because command validation failed");
            return;
        }

        StratagemRegistry.replaceDefinitions(loaded);
        CBCStratagems.LOGGER.info("Loaded {} stratagem definitions", loaded.size());
    }
}
