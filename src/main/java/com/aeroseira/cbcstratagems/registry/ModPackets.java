package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.client.StratagemClientState;
import com.aeroseira.cbcstratagems.network.ClientboundPlayerStratagemDataPacket;
import com.aeroseira.cbcstratagems.network.ClientboundStratagemInputStatePacket;
import com.aeroseira.cbcstratagems.network.ClientboundStratagemDefinitionsPacket;
import com.aeroseira.cbcstratagems.network.ServerboundStratagemInputControlPacket;
import com.aeroseira.cbcstratagems.network.ServerboundStratagemInputPacket;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPackets {
    public static final String PROTOCOL_VERSION = "1";

    private ModPackets() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPackets::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CBCStratagems.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(
                ClientboundStratagemDefinitionsPacket.TYPE,
                ClientboundStratagemDefinitionsPacket.STREAM_CODEC,
                (payload, context) -> StratagemClientState.replaceDefinitions(payload.definitions())
        );
        registrar.playToClient(
                ClientboundPlayerStratagemDataPacket.TYPE,
                ClientboundPlayerStratagemDataPacket.STREAM_CODEC,
                (payload, context) -> StratagemClientState.replacePlayerData(payload.unlockedStratagems(), payload.cooldowns())
        );
        registrar.playToClient(
                ClientboundStratagemInputStatePacket.TYPE,
                ClientboundStratagemInputStatePacket.STREAM_CODEC,
                (payload, context) -> StratagemClientState.replaceInputState(payload)
        );
        registrar.playToServer(
                ServerboundStratagemInputControlPacket.TYPE,
                ServerboundStratagemInputControlPacket.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        if (payload.active()) {
                            StratagemInputManager.start(player);
                        } else {
                            StratagemInputManager.cancel(player);
                        }
                    }
                }
        );
        registrar.playToServer(
                ServerboundStratagemInputPacket.TYPE,
                ServerboundStratagemInputPacket.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StratagemInputManager.handleInput(player, payload.direction());
                    }
                }
        );
    }
}
