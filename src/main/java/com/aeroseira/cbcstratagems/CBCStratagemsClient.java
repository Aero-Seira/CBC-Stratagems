package com.aeroseira.cbcstratagems;

import com.aeroseira.cbcstratagems.client.StratagemInputClient;
import com.aeroseira.cbcstratagems.client.StratagemMarkerRenderer;
import com.aeroseira.cbcstratagems.registry.ModEntityTypes;
import com.aeroseira.cbcstratagems.registry.ModKeyMappings;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class CBCStratagemsClient {
    private CBCStratagemsClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModKeyMappings::register);
        modEventBus.addListener(CBCStratagemsClient::registerEntityRenderers);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onClientTickPre);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onInteractionKeyMapping);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onMouseButton);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onMovementInput);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(StratagemInputClient::onLoggingOut);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.STRATAGEM_BEACON.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.STRATAGEM_MARKER.get(), StratagemMarkerRenderer::new);
    }
}
