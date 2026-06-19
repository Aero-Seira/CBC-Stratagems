package com.aeroseira.cbcstratagems.entity;

import com.aeroseira.cbcstratagems.registry.ModEntityTypes;
import com.aeroseira.cbcstratagems.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class StratagemBeaconProjectile extends ThrowableItemProjectile {
    public StratagemBeaconProjectile(EntityType<? extends StratagemBeaconProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public StratagemBeaconProjectile(Level level, LivingEntity shooter) {
        super(ModEntityTypes.STRATAGEM_BEACON.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.STRATAGEM_DEVICE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
