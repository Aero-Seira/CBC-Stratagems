package com.aeroseira.cbcstratagems.entity;

import com.aeroseira.cbcstratagems.player.PlayerStratagemDataManager;
import com.aeroseira.cbcstratagems.registry.ModSoundEvents;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinition;
import com.aeroseira.cbcstratagems.stratagem.StratagemEnvironment;
import com.aeroseira.cbcstratagems.stratagem.StratagemRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StratagemMarkerEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_STRATAGEM_ID = SynchedEntityData.defineId(StratagemMarkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_TICKS = SynchedEntityData.defineId(StratagemMarkerEntity.class, EntityDataSerializers.INT);

    private static final String STRATAGEM_ID_KEY = "stratagem_id";
    private static final String OWNER_UUID_KEY = "owner_uuid";
    private static final String REMAINING_TICKS_KEY = "remaining_ticks";
    private static final String COOLDOWN_STARTED_KEY = "cooldown_started";
    private static final String ENVIRONMENT_CHECKED_KEY = "environment_checked";

    private ResourceLocation stratagemId;
    private UUID ownerUuid;
    private int remainingTicks;
    private boolean cooldownStarted;
    private boolean environmentChecked;

    public StratagemMarkerEntity(EntityType<? extends StratagemMarkerEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void configure(ResourceLocation stratagemId, UUID ownerUuid, Vec3 position) {
        this.stratagemId = stratagemId;
        this.ownerUuid = ownerUuid;
        this.setPos(position.x(), position.y(), position.z());
        this.remainingTicks = StratagemRegistry.get(stratagemId).map(StratagemDefinition::countdownTicks).orElse(0);
        syncData();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STRATAGEM_ID, "");
        builder.define(DATA_REMAINING_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Optional<StratagemDefinition> definition = stratagemId == null ? Optional.empty() : StratagemRegistry.get(stratagemId);
        if (definition.isEmpty()) {
            this.discard();
            return;
        }

        if (!validateEnvironment(serverLevel, definition.get())) {
            return;
        }
        startCooldownIfNeeded(serverLevel, definition.get());
        if (remainingTicks > 0) {
            remainingTicks--;
            this.getEntityData().set(DATA_REMAINING_TICKS, remainingTicks);
            return;
        }

        notifyStrikeReady(serverLevel, definition.get());
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        ResourceLocation.read(compound.getString(STRATAGEM_ID_KEY)).result().ifPresent(id -> stratagemId = id);
        if (compound.hasUUID(OWNER_UUID_KEY)) {
            ownerUuid = compound.getUUID(OWNER_UUID_KEY);
        }
        remainingTicks = compound.getInt(REMAINING_TICKS_KEY);
        cooldownStarted = compound.getBoolean(COOLDOWN_STARTED_KEY);
        environmentChecked = compound.getBoolean(ENVIRONMENT_CHECKED_KEY);
        syncData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (stratagemId != null) {
            compound.putString(STRATAGEM_ID_KEY, stratagemId.toString());
        }
        if (ownerUuid != null) {
            compound.putUUID(OWNER_UUID_KEY, ownerUuid);
        }
        compound.putInt(REMAINING_TICKS_KEY, remainingTicks);
        compound.putBoolean(COOLDOWN_STARTED_KEY, cooldownStarted);
        compound.putBoolean(ENVIRONMENT_CHECKED_KEY, environmentChecked);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(1.0D, 128.0D, 1.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    private void startCooldownIfNeeded(ServerLevel level, StratagemDefinition definition) {
        if (cooldownStarted) {
            return;
        }
        cooldownStarted = true;

        ServerPlayer owner = owner(level);
        if (owner != null && definition.cooldownTicks() > 0) {
            PlayerStratagemDataManager.startCooldown(owner, definition.id(), level.getGameTime() + definition.cooldownTicks());
        }
    }

    private boolean validateEnvironment(ServerLevel level, StratagemDefinition definition) {
        if (environmentChecked) {
            return true;
        }
        environmentChecked = true;

        if (!StratagemEnvironment.hasOpenSky(level, this.blockPosition())) {
            denyStrike(level, Component.translatable("message.cbc_stratagems.input.no_sky"));
            return false;
        }

        StratagemEnvironment.ObstructionResult result = StratagemEnvironment.validateObstructions(level, this.position(), definition);
        if (!result.clear()) {
            denyStrike(level, Component.translatable(
                    "message.cbc_stratagems.strike.obstructed",
                    result.obstructionBlocks(),
                    result.maxObstructionBlocks()
            ));
            return false;
        }

        return true;
    }

    private void denyStrike(ServerLevel level, Component message) {
        ServerPlayer owner = owner(level);
        if (owner != null) {
            owner.displayClientMessage(message, true);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSoundEvents.STRIKE_DENIED.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        this.discard();
    }

    private void notifyStrikeReady(ServerLevel level, StratagemDefinition definition) {
        ServerPlayer owner = owner(level);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("message.cbc_stratagems.strike.not_implemented", definition.name()), true);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSoundEvents.STRIKE_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private ServerPlayer owner(ServerLevel level) {
        return ownerUuid == null ? null : level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public ResourceLocation stratagemId() {
        String encoded = this.getEntityData().get(DATA_STRATAGEM_ID);
        return encoded.isEmpty() ? null : ResourceLocation.parse(encoded);
    }

    public int remainingTicks() {
        return this.getEntityData().get(DATA_REMAINING_TICKS);
    }

    private void syncData() {
        this.getEntityData().set(DATA_STRATAGEM_ID, stratagemId == null ? "" : stratagemId.toString());
        this.getEntityData().set(DATA_REMAINING_TICKS, remainingTicks);
    }
}
