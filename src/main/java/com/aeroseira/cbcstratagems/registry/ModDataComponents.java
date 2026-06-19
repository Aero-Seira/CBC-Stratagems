package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.item.StratagemDeviceMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CBCStratagems.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StratagemDeviceMode>> DEVICE_MODE =
            DATA_COMPONENTS.registerComponentType(
                    "device_mode",
                    builder -> builder
                            .persistent(StratagemDeviceMode.CODEC)
                            .networkSynchronized(StratagemDeviceMode.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SELECTED_STRATAGEM =
            DATA_COMPONENTS.registerComponentType(
                    "selected_stratagem",
                    builder -> builder
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> LICENSE_STRATAGEM =
            DATA_COMPONENTS.registerComponentType(
                    "license_stratagem",
                    builder -> builder
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
            );

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
