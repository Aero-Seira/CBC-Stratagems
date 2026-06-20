package com.aeroseira.cbcstratagems.stratagem;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class StratagemEnvironment {
    private static final double SAMPLES_PER_BLOCK = 4.0D;

    private StratagemEnvironment() {
    }

    public static boolean hasOpenSky(ServerLevel level, BlockPos pos) {
        return !level.dimensionType().hasCeiling() && level.canSeeSky(pos);
    }

    public static ObstructionResult validateObstructions(ServerLevel level, Vec3 target, StratagemDefinition definition) {
        int worstSpan = 0;
        int strictestLimit = Integer.MAX_VALUE;

        for (StratagemArtilleryEntry entry : definition.artillery()) {
            Vec3 firePoint = target.add(0.0D, entry.spawnHeight(), 0.0D);
            int span = blockingBlockSpan(level, target.add(0.0D, 0.25D, 0.0D), firePoint);
            worstSpan = Math.max(worstSpan, span);
            strictestLimit = Math.min(strictestLimit, entry.maxObstructionBlocks());
            if (span > entry.maxObstructionBlocks()) {
                return new ObstructionResult(false, span, entry.maxObstructionBlocks());
            }
        }

        return new ObstructionResult(true, worstSpan, strictestLimit == Integer.MAX_VALUE ? 0 : strictestLimit);
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
}
