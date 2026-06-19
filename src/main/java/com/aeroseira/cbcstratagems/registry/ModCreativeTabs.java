package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CBCStratagems.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CBC_STRATAGEMS = TABS.register(
            "cbc_stratagems",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cbc_stratagems"))
                    .icon(() -> new ItemStack(ModItems.STRATAGEM_DEVICE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.STRATAGEM_DEVICE.get());
                        output.accept(ModItems.STRATAGEM_LICENSE.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
