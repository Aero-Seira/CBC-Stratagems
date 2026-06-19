package com.aeroseira.cbcstratagems.player;

import com.aeroseira.cbcstratagems.network.ClientboundPlayerStratagemDataPacket;
import com.aeroseira.cbcstratagems.registry.ModAttachments;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerStratagemDataManager {
    private PlayerStratagemDataManager() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerStratagemDataManager::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(PlayerStratagemDataManager::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(PlayerStratagemDataManager::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(PlayerStratagemDataManager::onDatapackSync);
    }

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation stratagemId) {
        return data(player).isUnlocked(stratagemId);
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation stratagemId) {
        boolean changed = data(player).unlock(stratagemId);
        if (changed) {
            sync(player);
        }
        return changed;
    }

    public static long getCooldownEnd(ServerPlayer player, ResourceLocation stratagemId) {
        return data(player).getCooldownEnd(stratagemId);
    }

    public static void startCooldown(ServerPlayer player, ResourceLocation stratagemId, long endTime) {
        data(player).startCooldown(stratagemId, endTime);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PlayerStratagemData playerData = data(player);
        List<ResourceLocation> unlocked = playerData.unlockedStratagems()
                .stream()
                .sorted(ResourceLocation::compareNamespaced)
                .toList();
        List<PlayerStratagemCooldown> cooldowns = playerData.cooldowns()
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey(), ResourceLocation::compareNamespaced))
                .map(entry -> new PlayerStratagemCooldown(entry.getKey(), entry.getValue()))
                .toList();

        PacketDistributor.sendToPlayer(player, new ClientboundPlayerStratagemDataPacket(unlocked, cooldowns));
    }

    private static PlayerStratagemData data(ServerPlayer player) {
        return player.getData(ModAttachments.PLAYER_STRATAGEM_DATA);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(PlayerStratagemDataManager::sync);
    }
}
