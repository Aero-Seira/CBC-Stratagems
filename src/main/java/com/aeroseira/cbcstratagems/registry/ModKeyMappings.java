package com.aeroseira.cbcstratagems.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.cbc_stratagems";

    public static final KeyMapping OPEN_STRATAGEM_PANEL = new KeyMapping(
            "key.cbc_stratagems.open_stratagem_panel",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STRATAGEM_PANEL);
    }
}
