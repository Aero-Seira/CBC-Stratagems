package com.aeroseira.cbcstratagems.entity;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.compat.cbc.CbcProjectileLaunchResult;
import com.aeroseira.cbcstratagems.compat.cbc.CbcProjectileLauncher;
import com.aeroseira.cbcstratagems.player.PlayerStratagemDataManager;
import com.aeroseira.cbcstratagems.registry.ModSoundEvents;
import com.aeroseira.cbcstratagems.stratagem.StratagemArtilleryEntry;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinition;
import com.aeroseira.cbcstratagems.stratagem.StratagemEnvironment;
import com.aeroseira.cbcstratagems.stratagem.StratagemRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StratagemMarkerEntity extends Entity {
    private static final int BEAM_HIDE_BEFORE_IMPACT_TICKS = 10;

    private static final EntityDataAccessor<String> DATA_STRATAGEM_ID = SynchedEntityData.defineId(StratagemMarkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_TICKS = SynchedEntityData.defineId(StratagemMarkerEntity.class, EntityDataSerializers.INT);

    private static final String STRATAGEM_ID_KEY = "stratagem_id";
    private static final String OWNER_UUID_KEY = "owner_uuid";
    private static final String REMAINING_TICKS_KEY = "remaining_ticks";
    private static final String COOLDOWN_STARTED_KEY = "cooldown_started";
    private static final String ENVIRONMENT_CHECKED_KEY = "environment_checked";
    private static final String STRIKE_STARTED_KEY = "strike_started";
    private static final String ARTILLERY_INDEX_KEY = "artillery_index";
    private static final String ARTILLERY_SHOT_INDEX_KEY = "artillery_shot_index";
    private static final String NEXT_SHOT_DELAY_KEY = "next_shot_delay";
    private static final String BEAM_DISCARD_GAME_TIME_KEY = "beam_discard_game_time";

    private ResourceLocation stratagemId;
    private UUID ownerUuid;
    private int remainingTicks;
    private boolean cooldownStarted;
    private boolean environmentChecked;
    private boolean strikeStarted;
    private int artilleryIndex;
    private int artilleryShotIndex;
    private int nextShotDelay;
    private long beamDiscardGameTime;

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

        if (tickStrike(serverLevel, definition.get())) {
            this.discard();
        }
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
        strikeStarted = compound.getBoolean(STRIKE_STARTED_KEY);
        artilleryIndex = compound.getInt(ARTILLERY_INDEX_KEY);
        artilleryShotIndex = compound.getInt(ARTILLERY_SHOT_INDEX_KEY);
        nextShotDelay = compound.getInt(NEXT_SHOT_DELAY_KEY);
        beamDiscardGameTime = compound.getLong(BEAM_DISCARD_GAME_TIME_KEY);
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
        compound.putBoolean(STRIKE_STARTED_KEY, strikeStarted);
        compound.putInt(ARTILLERY_INDEX_KEY, artilleryIndex);
        compound.putInt(ARTILLERY_SHOT_INDEX_KEY, artilleryShotIndex);
        compound.putInt(NEXT_SHOT_DELAY_KEY, nextShotDelay);
        compound.putLong(BEAM_DISCARD_GAME_TIME_KEY, beamDiscardGameTime);
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

        if (!StratagemEnvironment.allowsExternalStrike(level)) {
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

    private boolean tickStrike(ServerLevel level, StratagemDefinition definition) {
        if (!strikeStarted) {
            strikeStarted = true;
            notifyStrikeStart(level, definition);
        }
        if (nextShotDelay > 0) {
            nextShotDelay--;
            return false;
        }

        while (artilleryIndex < definition.artillery().size()) {
            StratagemArtilleryEntry entry = definition.artillery().get(artilleryIndex);
            if (artilleryShotIndex < entry.count()) {
                fireArtilleryShot(level, definition, entry);
                artilleryShotIndex++;
                nextShotDelay = entry.intervalTicks();
                return false;
            }

            artilleryIndex++;
            artilleryShotIndex = 0;
        }

        return beamDiscardGameTime <= 0L || level.getGameTime() >= beamDiscardGameTime;
    }

    private void notifyStrikeStart(ServerLevel level, StratagemDefinition definition) {
        ServerPlayer owner = owner(level);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("message.cbc_stratagems.strike.started", definition.name()), true);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSoundEvents.STRIKE_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void fireArtilleryShot(ServerLevel level, StratagemDefinition definition, StratagemArtilleryEntry entry) {
        Optional<Item> projectileItem = BuiltInRegistries.ITEM.getOptional(entry.projectile().id());
        if (projectileItem.isEmpty()) {
            failLaunch(level, definition, entry, CbcProjectileLaunchResult.CREATE_FAILED);
            return;
        }

        ItemStack projectileStack = new ItemStack(projectileItem.get(), entry.projectile().count());
        Vec3 targetPos = this.position().add(randomHorizontalOffset(level, entry.targetScatter()));
        StratagemEnvironment.FirePath firePath = StratagemEnvironment.resolveFirePath(level, targetPos, entry);
        Vec3 spawnPos = targetPos
                .add(firePath.horizontalOffset())
                .add(randomHorizontalOffset(level, entry.spawnScatter()))
                .add(0.0D, entry.spawnHeight(), 0.0D);
        spawnPos = new Vec3(spawnPos.x(), Mth.clamp(spawnPos.y(), level.getMinBuildHeight() + 1.0D, level.getMaxBuildHeight() - 1.0D), spawnPos.z());

        CbcProjectileLaunchResult result = CbcProjectileLauncher.launch(level, projectileStack, spawnPos, targetPos, entry.power(), entry.spread(), owner(level));
        if (result != CbcProjectileLaunchResult.SUCCESS) {
            failLaunch(level, definition, entry, result);
            return;
        }

        beamDiscardGameTime = Math.max(beamDiscardGameTime, estimateBeamDiscardGameTime(level, spawnPos, targetPos, entry.power()));
    }

    private Vec3 randomHorizontalOffset(ServerLevel level, double radius) {
        if (radius <= 0.0D) {
            return Vec3.ZERO;
        }

        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(level.random.nextDouble()) * radius;
        return new Vec3(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private long estimateBeamDiscardGameTime(ServerLevel level, Vec3 spawnPos, Vec3 targetPos, float power) {
        double speed = Math.max(0.1D, power);
        int travelTicks = Mth.ceil(spawnPos.distanceTo(targetPos) / speed);
        return level.getGameTime() + Math.max(0, travelTicks - BEAM_HIDE_BEFORE_IMPACT_TICKS);
    }

    private void failLaunch(
            ServerLevel level,
            StratagemDefinition definition,
            StratagemArtilleryEntry entry,
            CbcProjectileLaunchResult result
    ) {
        CBCStratagems.LOGGER.warn("Failed to launch stratagem projectile for {} using {}: {}", definition.id(), entry.projectile().id(), result);
        ServerPlayer owner = owner(level);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable("message.cbc_stratagems.strike.launch_failed", definition.name(), result.name()), true);
        }
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
