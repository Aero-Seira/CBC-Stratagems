package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.item.StratagemDeviceItem;
import com.aeroseira.cbcstratagems.item.StratagemLicenseItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CBCStratagems.MOD_ID);

    public static final DeferredItem<StratagemDeviceItem> STRATAGEM_DEVICE =
            ITEMS.registerItem(
                    "stratagem_device",
                    StratagemDeviceItem::new,
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<StratagemLicenseItem> STRATAGEM_LICENSE =
            ITEMS.registerItem("stratagem_license", StratagemLicenseItem::new, new Item.Properties());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
