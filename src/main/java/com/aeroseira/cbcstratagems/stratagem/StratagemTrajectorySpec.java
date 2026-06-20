package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record StratagemTrajectorySpec(
        StratagemTrajectoryMode mode,
        int maxObstructionBlocks,
        double spawnBearingDegrees,
        double spawnDistance,
        double searchRadius,
        double minElevationDegrees,
        int bearingSteps,
        int radiusSteps
) {
    public static final int DEFAULT_MAX_OBSTRUCTION_BLOCKS = 16;
    public static final double DEFAULT_SEARCH_RADIUS = 56.0D;
    public static final double DEFAULT_MIN_ELEVATION_DEGREES = 30.0D;
    public static final int DEFAULT_BEARING_STEPS = 16;
    public static final int DEFAULT_RADIUS_STEPS = 2;

    public static final StratagemTrajectorySpec DEFAULT = new StratagemTrajectorySpec(
            StratagemTrajectoryMode.AUTO,
            DEFAULT_MAX_OBSTRUCTION_BLOCKS,
            0.0D,
            0.0D,
            DEFAULT_SEARCH_RADIUS,
            DEFAULT_MIN_ELEVATION_DEGREES,
            DEFAULT_BEARING_STEPS,
            DEFAULT_RADIUS_STEPS
    );

    public static final Codec<StratagemTrajectorySpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemTrajectoryMode.CODEC.optionalFieldOf("mode", DEFAULT.mode()).forGetter(StratagemTrajectorySpec::mode),
            Codec.INT.optionalFieldOf("max_obstruction_blocks", DEFAULT.maxObstructionBlocks()).forGetter(StratagemTrajectorySpec::maxObstructionBlocks),
            Codec.DOUBLE.optionalFieldOf("spawn_bearing_degrees", DEFAULT.spawnBearingDegrees()).forGetter(StratagemTrajectorySpec::spawnBearingDegrees),
            Codec.DOUBLE.optionalFieldOf("spawn_distance", DEFAULT.spawnDistance()).forGetter(StratagemTrajectorySpec::spawnDistance),
            Codec.DOUBLE.optionalFieldOf("search_radius", DEFAULT.searchRadius()).forGetter(StratagemTrajectorySpec::searchRadius),
            Codec.DOUBLE.optionalFieldOf("min_elevation_degrees", DEFAULT.minElevationDegrees()).forGetter(StratagemTrajectorySpec::minElevationDegrees),
            Codec.INT.optionalFieldOf("bearing_steps", DEFAULT.bearingSteps()).forGetter(StratagemTrajectorySpec::bearingSteps),
            Codec.INT.optionalFieldOf("radius_steps", DEFAULT.radiusSteps()).forGetter(StratagemTrajectorySpec::radiusSteps)
    ).apply(instance, StratagemTrajectorySpec::new));

    public static StratagemTrajectorySpec legacy(StratagemArtilleryEntry entry) {
        return new StratagemTrajectorySpec(
                entry.trajectoryMode(),
                entry.maxObstructionBlocks(),
                entry.spawnBearingDegrees(),
                entry.spawnDistance(),
                entry.autoSearchRadius(),
                entry.autoMinElevationDegrees(),
                entry.autoBearingSteps(),
                entry.autoRadiusSteps()
        );
    }

    public StratagemTrajectorySpec normalized() {
        return new StratagemTrajectorySpec(
                mode,
                maxObstructionBlocks,
                spawnBearingDegrees,
                spawnDistance,
                searchRadius,
                minElevationDegrees,
                bearingSteps,
                radiusSteps
        );
    }

    public Vec3 fixedSpawnOffset() {
        if (spawnDistance <= 0.0D) {
            return Vec3.ZERO;
        }

        double radians = Math.toRadians(spawnBearingDegrees);
        return new Vec3(Math.sin(radians) * spawnDistance, 0.0D, -Math.cos(radians) * spawnDistance);
    }
}
