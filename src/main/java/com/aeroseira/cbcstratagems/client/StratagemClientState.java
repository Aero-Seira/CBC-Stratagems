package com.aeroseira.cbcstratagems.client;

import com.aeroseira.cbcstratagems.network.ClientboundStratagemInputStatePacket;
import com.aeroseira.cbcstratagems.player.PlayerStratagemCooldown;
import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinitionSummary;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class StratagemClientState {
    private static final int INPUT_FEEDBACK_TICKS = 40;

    private static volatile Map<ResourceLocation, StratagemDefinitionSummary> definitions = Map.of();
    private static volatile Set<ResourceLocation> unlockedStratagems = Set.of();
    private static volatile Map<ResourceLocation, Long> cooldowns = Map.of();
    private static volatile StratagemInputStatus inputStatus = StratagemInputStatus.INACTIVE;
    private static volatile List<StratagemCommand> currentInput = List.of();
    private static volatile Optional<ResourceLocation> selectedStratagem = Optional.empty();
    private static volatile Component inputMessage = Component.empty();
    private static int inputFeedbackTicks;

    private StratagemClientState() {
    }

    public static void replaceDefinitions(List<StratagemDefinitionSummary> newDefinitions) {
        Map<ResourceLocation, StratagemDefinitionSummary> next = new LinkedHashMap<>();
        for (StratagemDefinitionSummary definition : newDefinitions) {
            next.put(definition.id(), definition);
        }
        definitions = Collections.unmodifiableMap(next);
    }

    public static void replacePlayerData(List<ResourceLocation> unlocked, List<PlayerStratagemCooldown> newCooldowns) {
        Set<ResourceLocation> nextUnlocked = new LinkedHashSet<>();
        for (ResourceLocation stratagemId : unlocked) {
            nextUnlocked.add(stratagemId);
        }

        Map<ResourceLocation, Long> nextCooldowns = new LinkedHashMap<>();
        for (PlayerStratagemCooldown cooldown : newCooldowns) {
            nextCooldowns.put(cooldown.stratagemId(), cooldown.endTime());
        }

        unlockedStratagems = Collections.unmodifiableSet(nextUnlocked);
        cooldowns = Collections.unmodifiableMap(nextCooldowns);
    }

    public static void replaceInputState(ClientboundStratagemInputStatePacket packet) {
        inputStatus = packet.status();
        currentInput = List.copyOf(packet.input());
        selectedStratagem = packet.selectedStratagem();
        inputMessage = packet.message();
        inputFeedbackTicks = packet.status() == StratagemInputStatus.FAILED || packet.status() == StratagemInputStatus.COMPLETE
                ? INPUT_FEEDBACK_TICKS
                : 0;
    }

    public static void beginLocalInput() {
        inputStatus = StratagemInputStatus.ACTIVE;
        currentInput = List.of();
        selectedStratagem = Optional.empty();
        inputMessage = Component.empty();
        inputFeedbackTicks = 0;
    }

    public static void clearInputState() {
        inputStatus = StratagemInputStatus.INACTIVE;
        currentInput = List.of();
        selectedStratagem = Optional.empty();
        inputMessage = Component.empty();
        inputFeedbackTicks = 0;
    }

    public static void tickInputFeedback() {
        if (inputFeedbackTicks > 0) {
            inputFeedbackTicks--;
        }
    }

    public static Collection<StratagemDefinitionSummary> definitions() {
        return definitions.values();
    }

    public static Optional<StratagemDefinitionSummary> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static boolean isUnlocked(ResourceLocation id) {
        return definitions.containsKey(id) && unlockedStratagems.contains(id);
    }

    public static long cooldownEnd(ResourceLocation id) {
        return definitions.containsKey(id) ? cooldowns.getOrDefault(id, 0L) : 0L;
    }

    public static boolean isInputActive() {
        return inputStatus == StratagemInputStatus.ACTIVE;
    }

    public static boolean shouldRenderInputOverlay() {
        return isInputActive() || inputFeedbackTicks > 0;
    }

    public static StratagemInputStatus inputStatus() {
        return inputStatus;
    }

    public static List<StratagemCommand> currentInput() {
        return currentInput;
    }

    public static Optional<ResourceLocation> selectedStratagem() {
        return selectedStratagem;
    }

    public static Component inputMessage() {
        return inputMessage;
    }
}
