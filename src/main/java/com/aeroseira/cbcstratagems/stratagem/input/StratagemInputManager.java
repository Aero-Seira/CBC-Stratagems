package com.aeroseira.cbcstratagems.stratagem.input;

import com.aeroseira.cbcstratagems.item.StratagemDeviceMode;
import com.aeroseira.cbcstratagems.network.ClientboundStratagemInputStatePacket;
import com.aeroseira.cbcstratagems.player.PlayerStratagemDataManager;
import com.aeroseira.cbcstratagems.registry.ModDataComponents;
import com.aeroseira.cbcstratagems.registry.ModItems;
import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinition;
import com.aeroseira.cbcstratagems.stratagem.StratagemEnvironment;
import com.aeroseira.cbcstratagems.stratagem.StratagemRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StratagemInputManager {
    private static final Map<UUID, PlayerStratagemInputSession> SESSIONS = new ConcurrentHashMap<>();

    private StratagemInputManager() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StratagemInputManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(StratagemInputManager::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(StratagemInputManager::onPlayerChangedDimension);
    }

    public static boolean start(ServerPlayer player) {
        ItemStack deviceStack = player.getMainHandItem();
        if (!deviceStack.is(ModItems.STRATAGEM_DEVICE.get())) {
            return false;
        }

        boolean blocked = StratagemEnvironment.blocksCallerInput(player.serverLevel(), player.blockPosition());
        PlayerStratagemInputSession session = new PlayerStratagemInputSession(blocked);
        SESSIONS.put(player.getUUID(), session);
        syncInputState(player, session);
        return true;
    }

    public static void cancel(ServerPlayer player) {
        PlayerStratagemInputSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            sync(player, StratagemInputStatus.INACTIVE, session.input(), Optional.empty(), Component.empty());
        }
    }

    public static void handleInput(ServerPlayer player, StratagemCommand command) {
        PlayerStratagemInputSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        ItemStack deviceStack = player.getMainHandItem();
        if (!deviceStack.is(ModItems.STRATAGEM_DEVICE.get())) {
            cancel(player);
            return;
        }
        if (session.inputBlocked()) {
            syncInputState(player, session);
            return;
        }

        session.add(command);
        List<StratagemCommand> input = session.input();
        List<StratagemDefinition> candidates = findCandidates(input);
        if (candidates.isEmpty()) {
            resetInput(player, session, input, Component.translatable("message.cbc_stratagems.input.no_match"), StratagemInputFeedback.ERROR);
            return;
        }

        Optional<StratagemDefinition> complete = candidates.stream()
                .filter(definition -> definition.command().size() == input.size())
                .findFirst();
        if (complete.isPresent()) {
            complete(player, deviceStack, session, input, complete.get());
        } else {
            sync(player, StratagemInputStatus.ACTIVE, input, Optional.empty(), Component.empty());
        }
    }

    private static void complete(
            ServerPlayer player,
            ItemStack deviceStack,
            PlayerStratagemInputSession session,
            List<StratagemCommand> input,
            StratagemDefinition definition
    ) {
        ResourceLocation stratagemId = definition.id();
        if (!PlayerStratagemDataManager.isUnlocked(player, stratagemId)) {
            fail(player, input, Component.translatable("message.cbc_stratagems.input.locked", definition.name()));
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        long cooldownEnd = PlayerStratagemDataManager.getCooldownEnd(player, stratagemId);
        if (cooldownEnd > gameTime) {
            resetInput(
                    player,
                    session,
                    input,
                    Component.translatable("message.cbc_stratagems.input.cooldown", (cooldownEnd - gameTime + 19L) / 20L),
                    StratagemInputFeedback.COOLDOWN
            );
            return;
        }

        SESSIONS.remove(player.getUUID());
        deviceStack.set(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.BEACON);
        deviceStack.set(ModDataComponents.SELECTED_STRATAGEM, stratagemId);
        player.stopUsingItem();
        sync(player, StratagemInputStatus.COMPLETE, input, Optional.of(stratagemId), Component.translatable("message.cbc_stratagems.input.complete", definition.name()));
    }

    private static void fail(ServerPlayer player, List<StratagemCommand> input, Component message) {
        SESSIONS.remove(player.getUUID());
        player.stopUsingItem();
        sync(player, StratagemInputStatus.FAILED, input, Optional.empty(), message);
        player.displayClientMessage(message, true);
    }

    private static void resetInput(
            ServerPlayer player,
            PlayerStratagemInputSession session,
            List<StratagemCommand> displayInput,
            Component message,
            StratagemInputFeedback feedback
    ) {
        session.clear();
        sync(player, StratagemInputStatus.ACTIVE, displayInput, Optional.empty(), message, feedback);
        player.displayClientMessage(message, true);
    }

    private static void failWithoutSession(ServerPlayer player, Component message) {
        sync(player, StratagemInputStatus.FAILED, List.of(), Optional.empty(), message);
        player.displayClientMessage(message, true);
    }

    private static void syncInputState(ServerPlayer player, PlayerStratagemInputSession session) {
        if (session.inputBlocked()) {
            sync(
                    player,
                    StratagemInputStatus.ACTIVE,
                    List.of(),
                    Optional.empty(),
                    Component.translatable("message.cbc_stratagems.input.no_sky"),
                    StratagemInputFeedback.BLOCKED
            );
            return;
        }

        sync(player, StratagemInputStatus.ACTIVE, session.input(), Optional.empty(), Component.empty());
    }

    private static List<StratagemDefinition> findCandidates(List<StratagemCommand> input) {
        List<StratagemDefinition> candidates = new ArrayList<>();
        for (StratagemDefinition definition : StratagemRegistry.definitions()) {
            if (isPrefix(input, definition.command())) {
                candidates.add(definition);
            }
        }
        return candidates;
    }

    private static boolean isPrefix(List<StratagemCommand> input, List<StratagemCommand> command) {
        if (input.size() > command.size()) {
            return false;
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i) != command.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static void sync(
            ServerPlayer player,
            StratagemInputStatus status,
            List<StratagemCommand> input,
            Optional<ResourceLocation> selectedStratagem,
            Component message
    ) {
        sync(player, status, input, selectedStratagem, message, StratagemInputFeedback.NONE);
    }

    private static void sync(
            ServerPlayer player,
            StratagemInputStatus status,
            List<StratagemCommand> input,
            Optional<ResourceLocation> selectedStratagem,
            Component message,
            StratagemInputFeedback feedback
    ) {
        PacketDistributor.sendToPlayer(player, new ClientboundStratagemInputStatePacket(status, input, selectedStratagem, message, feedback));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (Map.Entry<UUID, PlayerStratagemInputSession> entry : List.copyOf(SESSIONS.entrySet())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                SESSIONS.remove(entry.getKey());
            } else if (!player.getMainHandItem().is(ModItems.STRATAGEM_DEVICE.get())) {
                cancel(player);
            } else {
                updateEnvironmentBlock(player, entry.getValue());
            }
        }
    }

    private static void updateEnvironmentBlock(ServerPlayer player, PlayerStratagemInputSession session) {
        boolean blocked = StratagemEnvironment.blocksCallerInput(player.serverLevel(), player.blockPosition());
        if (session.inputBlocked() == blocked) {
            return;
        }

        session.setInputBlocked(blocked);
        session.clear();
        syncInputState(player, session);
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player);
        }
    }
}
