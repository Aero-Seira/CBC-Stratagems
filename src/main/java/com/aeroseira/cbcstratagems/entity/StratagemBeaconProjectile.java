package com.aeroseira.cbcstratagems.entity;

import com.aeroseira.cbcstratagems.registry.ModSoundEvents;
import com.aeroseira.cbcstratagems.stratagem.StratagemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import com.aeroseira.cbcstratagems.registry.ModEntityTypes;
import com.aeroseira.cbcstratagems.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class StratagemBeaconProjectile extends ThrowableItemProjectile {
    private static final String STRATAGEM_ID_KEY = "stratagem_id";

    private ResourceLocation stratagemId;

    public StratagemBeaconProjectile(EntityType<? extends StratagemBeaconProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public StratagemBeaconProjectile(Level level, LivingEntity shooter) {
        super(ModEntityTypes.STRATAGEM_BEACON.get(), shooter, level);
    }

    public StratagemBeaconProjectile(Level level, LivingEntity shooter, ResourceLocation stratagemId) {
        this(level, shooter);
        this.stratagemId = stratagemId;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.STRATAGEM_DEVICE.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (stratagemId != null) {
            compound.putString(STRATAGEM_ID_KEY, stratagemId.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        ResourceLocation.read(compound.getString(STRATAGEM_ID_KEY)).result().ifPresent(id -> stratagemId = id);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            spawnMarker(result);
            this.discard();
        }
    }

    private void spawnMarker(HitResult result) {
        if (!(this.level() instanceof ServerLevel serverLevel) || stratagemId == null || StratagemRegistry.get(stratagemId).isEmpty()) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) {
            return;
        }

        StratagemMarkerEntity marker = new StratagemMarkerEntity(ModEntityTypes.STRATAGEM_MARKER.get(), serverLevel);
        marker.configure(stratagemId, player.getUUID(), result.getLocation());
        serverLevel.addFreshEntity(marker);
        serverLevel.playSound(null, marker.getX(), marker.getY(), marker.getZ(), ModSoundEvents.BEACON_LAND.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
