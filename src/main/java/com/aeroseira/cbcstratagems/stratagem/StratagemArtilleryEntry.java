package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record StratagemArtilleryEntry(
        StratagemProjectileSpec projectile,
        int count,
        int intervalTicks,
        int spawnHeight,
        double spawnScatter,
        double targetScatter,
        float power,
        float spread,
        int maxObstructionBlocks,
        double spawnBearingDegrees,
        double spawnDistance,
        StratagemTrajectoryMode trajectoryMode,
        double autoSearchRadius,
        double autoMinElevationDegrees,
        int autoBearingSteps,
        int autoRadiusSteps
) {
    public static final float DEFAULT_POWER = 8.0F;
    public static final int DEFAULT_MAX_OBSTRUCTION_BLOCKS = 16;
    public static final double DEFAULT_AUTO_SEARCH_RADIUS = 56.0D;
    public static final double DEFAULT_AUTO_MIN_ELEVATION_DEGREES = 30.0D;
    public static final int DEFAULT_AUTO_BEARING_STEPS = 16;
    public static final int DEFAULT_AUTO_RADIUS_STEPS = 2;

    public static final Codec<StratagemArtilleryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemProjectileSpec.CODEC.fieldOf("projectile").forGetter(StratagemArtilleryEntry::projectile),
            Codec.INT.fieldOf("count").forGetter(StratagemArtilleryEntry::count),
            Codec.INT.fieldOf("interval_ticks").forGetter(StratagemArtilleryEntry::intervalTicks),
            Codec.INT.fieldOf("spawn_height").forGetter(StratagemArtilleryEntry::spawnHeight),
            Codec.DOUBLE.fieldOf("spawn_scatter").forGetter(StratagemArtilleryEntry::spawnScatter),
            Codec.DOUBLE.fieldOf("target_scatter").forGetter(StratagemArtilleryEntry::targetScatter),
            Codec.FLOAT.optionalFieldOf("power", DEFAULT_POWER).forGetter(StratagemArtilleryEntry::power),
            Codec.FLOAT.fieldOf("spread").forGetter(StratagemArtilleryEntry::spread),
            Codec.INT.optionalFieldOf("max_obstruction_blocks", DEFAULT_MAX_OBSTRUCTION_BLOCKS).forGetter(StratagemArtilleryEntry::maxObstructionBlocks),
            Codec.DOUBLE.optionalFieldOf("spawn_bearing_degrees", 0.0D).forGetter(StratagemArtilleryEntry::spawnBearingDegrees),
            Codec.DOUBLE.optionalFieldOf("spawn_distance", 0.0D).forGetter(StratagemArtilleryEntry::spawnDistance),
            StratagemTrajectoryMode.CODEC.optionalFieldOf("trajectory_mode", StratagemTrajectoryMode.AUTO).forGetter(StratagemArtilleryEntry::trajectoryMode),
            Codec.DOUBLE.optionalFieldOf("auto_search_radius", DEFAULT_AUTO_SEARCH_RADIUS).forGetter(StratagemArtilleryEntry::autoSearchRadius),
            Codec.DOUBLE.optionalFieldOf("auto_min_elevation_degrees", DEFAULT_AUTO_MIN_ELEVATION_DEGREES).forGetter(StratagemArtilleryEntry::autoMinElevationDegrees),
            Codec.INT.optionalFieldOf("auto_bearing_steps", DEFAULT_AUTO_BEARING_STEPS).forGetter(StratagemArtilleryEntry::autoBearingSteps),
            Codec.INT.optionalFieldOf("auto_radius_steps", DEFAULT_AUTO_RADIUS_STEPS).forGetter(StratagemArtilleryEntry::autoRadiusSteps)
    ).apply(instance, StratagemArtilleryEntry::new));

    public Vec3 fixedSpawnOffset() {
        if (spawnDistance <= 0.0D) {
            return Vec3.ZERO;
        }

        double radians = Math.toRadians(spawnBearingDegrees);
        return new Vec3(Math.sin(radians) * spawnDistance, 0.0D, -Math.cos(radians) * spawnDistance);
    }
}
