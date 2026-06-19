package com.aeroseira.cbcstratagems;

import com.aeroseira.cbcstratagems.player.PlayerStratagemDataManager;
import com.aeroseira.cbcstratagems.registry.ModAttachments;
import com.aeroseira.cbcstratagems.registry.ModCreativeTabs;
import com.aeroseira.cbcstratagems.registry.ModDataComponents;
import com.aeroseira.cbcstratagems.registry.ModEntityTypes;
import com.aeroseira.cbcstratagems.registry.ModItems;
import com.aeroseira.cbcstratagems.registry.ModPackets;
import com.aeroseira.cbcstratagems.registry.ModSoundEvents;
import com.aeroseira.cbcstratagems.stratagem.StratagemReloadListener;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CBCStratagems.MOD_ID)
public final class CBCStratagems {
    public static final String MOD_ID = "cbc_stratagems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CBCStratagems(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModPackets.register(modEventBus);
        StratagemReloadListener.register();
        PlayerStratagemDataManager.register();
        StratagemInputManager.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            CBCStratagemsClient.register(modEventBus);
        }

        modEventBus.addListener(this::commonSetup);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CBC Stratagems common setup");
    }
}
