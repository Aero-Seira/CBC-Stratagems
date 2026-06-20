package com.aeroseira.cbcstratagems.stratagem;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class StratagemEnvironment {
    private static final double SAMPLES_PER_BLOCK = 4.0D;

    private StratagemEnvironment() {
    }

    public static boolean hasOpenSky(ServerLevel level, BlockPos pos) {
        return !level.dimensionType().hasCeiling() && level.canSeeSky(pos);
    }

    public static boolean blocksCallerInput(ServerLevel level, BlockPos pos) {
        return level.dimensionType().hasCeiling() || isBelowSurface(level, pos);
    }

    public static boolean allowsExternalStrike(ServerLevel level) {
        return !level.dimensionType().hasCeiling();
    }

    public static boolean isBelowSurface(ServerLevel level, BlockPos pos) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return !level.canSeeSky(pos) && surfaceY - pos.getY() > 8;
    }

    public static ObstructionResult validateObstructions(ServerLevel level, Vec3 target, StratagemDefinition definition) {
        int worstSpan = 0;
        int strictestLimit = Integer.MAX_VALUE;

        for (StratagemFirePhase phase : definition.firePlan().phases()) {
            for (StratagemFireEntry entry : phase.entries()) {
                FirePath path = resolveFirePath(level, target, entry);
                int span = path.obstructionBlocks();
                int limit = entry.trajectory().maxObstructionBlocks();
                worstSpan = Math.max(worstSpan, span);
                strictestLimit = Math.min(strictestLimit, limit);
                if (span > limit) {
                    return new ObstructionResult(false, span, limit);
                }
            }
        }

        return new ObstructionResult(true, worstSpan, strictestLimit == Integer.MAX_VALUE ? 0 : strictestLimit);
    }

    public static FirePath resolveFirePath(ServerLevel level, Vec3 target, StratagemFireEntry entry) {
        return resolveFirePath(level, target, entry.trajectory(), entry.launch());
    }

    private static FirePath resolveFirePath(ServerLevel level, Vec3 target, StratagemTrajectorySpec trajectory, StratagemLaunchSpec launch) {
        if (trajectory.mode() == StratagemTrajectoryMode.FIXED) {
            return firePath(level, target, trajectory.fixedSpawnOffset(), launch.spawnHeight());
        }

        double maxByElevation = launch.spawnHeight() / Math.tan(Math.toRadians(trajectory.minElevationDegrees()));
        double searchRadius = Math.min(trajectory.searchRadius(), Math.max(0.0D, maxByElevation));
        int bearingSteps = Math.max(1, trajectory.bearingSteps());
        int radiusSteps = Math.max(1, trajectory.radiusSteps());
        if (searchRadius <= 0.0D) {
            return firePath(level, target, Vec3.ZERO, launch.spawnHeight());
        }

        FirePath best = null;
        for (int radiusIndex = 1; radiusIndex <= radiusSteps; radiusIndex++) {
            double radius = searchRadius * radiusIndex / radiusSteps;
            for (int bearingIndex = 0; bearingIndex < bearingSteps; bearingIndex++) {
                double radians = Math.PI * 2.0D * bearingIndex / bearingSteps;
                Vec3 offset = new Vec3(Math.sin(radians) * radius, 0.0D, -Math.cos(radians) * radius);
                FirePath candidate = firePath(level, target, offset, launch.spawnHeight());
                if (best == null || isBetterPath(candidate, best)) {
                    best = candidate;
                }
            }
        }

        return best;
    }

    private static FirePath firePath(ServerLevel level, Vec3 target, Vec3 horizontalOffset, int spawnHeight) {
        Vec3 firePoint = target.add(horizontalOffset).add(0.0D, spawnHeight, 0.0D);
        int span = blockingBlockSpan(level, target.add(0.0D, 0.25D, 0.0D), firePoint);
        return new FirePath(horizontalOffset, span);
    }

    private static boolean isBetterPath(FirePath candidate, FirePath current) {
        if (candidate.obstructionBlocks() != current.obstructionBlocks()) {
            return candidate.obstructionBlocks() < current.obstructionBlocks();
        }
        return candidate.horizontalOffset().lengthSqr() > current.horizontalOffset().lengthSqr();
    }

    private static int blockingBlockSpan(ServerLevel level, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance * SAMPLES_PER_BLOCK));
        Vec3 direction = to.subtract(from).normalize();
        Set<Long> visited = new HashSet<>();
        double firstObstruction = Double.MAX_VALUE;
        double lastObstruction = Double.NEGATIVE_INFINITY;

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / (double) steps;
            BlockPos pos = BlockPos.containing(from.lerp(to, t));
            long packed = pos.asLong();
            if (!visited.add(packed)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            double progress = Vec3.atCenterOf(pos).subtract(from).dot(direction);
            firstObstruction = Math.min(firstObstruction, progress);
            lastObstruction = Math.max(lastObstruction, progress);
        }

        if (firstObstruction == Double.MAX_VALUE) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(lastObstruction - firstObstruction) + 1);
    }

    public record ObstructionResult(boolean clear, int obstructionBlocks, int maxObstructionBlocks) {
    }

    public record FirePath(Vec3 horizontalOffset, int obstructionBlocks) {
    }
}
