package com.aeroseira.cbcstratagems.registry;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.entity.StratagemBeaconProjectile;
import com.aeroseira.cbcstratagems.entity.StratagemMarkerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CBCStratagems.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<StratagemBeaconProjectile>> STRATAGEM_BEACON =
            ENTITY_TYPES.register("stratagem_beacon", key -> EntityType.Builder.<StratagemBeaconProjectile>of(StratagemBeaconProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(key.toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<StratagemMarkerEntity>> STRATAGEM_MARKER =
            ENTITY_TYPES.register("stratagem_marker", key -> EntityType.Builder.<StratagemMarkerEntity>of(StratagemMarkerEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(key.toString()));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
