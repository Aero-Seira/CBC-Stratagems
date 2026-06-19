package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.client.StratagemClientState;
import com.aeroseira.cbcstratagems.network.ClientboundStratagemDefinitionsPacket;
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
    }
}
