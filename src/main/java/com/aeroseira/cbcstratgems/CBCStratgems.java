package com.aeroseira.cbcstratgems;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(CBCStratgems.MOD_ID)
public final class CBCStratgems {
    public static final String MOD_ID = "cbc_stratgems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CBCStratgems(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CBC Stratagems common setup");
    }
}
