package com.aeroseira.cbcstratagems;

import com.aeroseira.cbcstratagems.registry.ModKeyMappings;
import net.neoforged.bus.api.IEventBus;

public final class CBCStratagemsClient {
    private CBCStratagemsClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModKeyMappings::register);
    }
}
