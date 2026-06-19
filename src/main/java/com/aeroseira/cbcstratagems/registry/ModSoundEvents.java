package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CBCStratagems.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> UI_OPEN = register("ui_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_CLOSE = register("ui_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> INPUT = register("input");
    public static final DeferredHolder<SoundEvent, SoundEvent> INPUT_FAILED = register("input_failed");
    public static final DeferredHolder<SoundEvent, SoundEvent> INPUT_COMPLETE = register("input_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEACON_THROW = register("beacon_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEACON_LAND = register("beacon_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRIKE_DENIED = register("strike_denied");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRIKE_START = register("strike_start");

    private ModSoundEvents() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CBCStratagems.id(name)));
    }
}
