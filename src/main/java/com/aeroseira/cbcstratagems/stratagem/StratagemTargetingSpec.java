package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record StratagemTargetingSpec(
        StratagemTargetingType type,
        StratagemTargetMode mode,
        StratagemTargetDistribution distribution,
        double radius,
        double startOffsetX,
        double startOffsetZ,
        double endOffsetX,
        double endOffsetZ,
        double lockRadius,
        boolean hostileOnly
) {
    public static final StratagemTargetingSpec DEFAULT = new StratagemTargetingSpec(
            StratagemTargetingType.PRECISE,
            StratagemTargetMode.MARKER,
            StratagemTargetDistribution.POINT,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            32.0D,
            true
    );

    public static final Codec<StratagemTargetingSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemTargetingType.CODEC.optionalFieldOf("type", DEFAULT.type()).forGetter(StratagemTargetingSpec::type),
            StratagemTargetMode.CODEC.optionalFieldOf("mode", DEFAULT.mode()).forGetter(StratagemTargetingSpec::mode),
            StratagemTargetDistribution.CODEC.optionalFieldOf("distribution", DEFAULT.distribution()).forGetter(StratagemTargetingSpec::distribution),
            Codec.DOUBLE.optionalFieldOf("radius", DEFAULT.radius()).forGetter(StratagemTargetingSpec::radius),
            Codec.DOUBLE.optionalFieldOf("start_offset_x", DEFAULT.startOffsetX()).forGetter(StratagemTargetingSpec::startOffsetX),
            Codec.DOUBLE.optionalFieldOf("start_offset_z", DEFAULT.startOffsetZ()).forGetter(StratagemTargetingSpec::startOffsetZ),
            Codec.DOUBLE.optionalFieldOf("end_offset_x", DEFAULT.endOffsetX()).forGetter(StratagemTargetingSpec::endOffsetX),
            Codec.DOUBLE.optionalFieldOf("end_offset_z", DEFAULT.endOffsetZ()).forGetter(StratagemTargetingSpec::endOffsetZ),
            Codec.DOUBLE.optionalFieldOf("lock_radius", DEFAULT.lockRadius()).forGetter(StratagemTargetingSpec::lockRadius),
            Codec.BOOL.optionalFieldOf("hostile_only", DEFAULT.hostileOnly()).forGetter(StratagemTargetingSpec::hostileOnly)
    ).apply(instance, StratagemTargetingSpec::new));

    public StratagemTargetingSpec {
        if (type == StratagemTargetingType.PRECISE && distribution == StratagemTargetDistribution.RANDOM_DISC) {
            type = StratagemTargetingType.RANDOM_AREA;
        }
    }

    public static StratagemTargetingSpec legacy(double targetScatter) {
        return new StratagemTargetingSpec(
                targetScatter > 0.0D ? StratagemTargetingType.RANDOM_AREA : StratagemTargetingType.PRECISE,
                StratagemTargetMode.MARKER,
                targetScatter > 0.0D ? StratagemTargetDistribution.RANDOM_DISC : StratagemTargetDistribution.POINT,
                targetScatter,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                DEFAULT.lockRadius(),
                DEFAULT.hostileOnly()
        );
    }

    public StratagemTargetingSpec normalized() {
        return new StratagemTargetingSpec(
                type,
                mode,
                distribution,
                radius,
                startOffsetX,
                startOffsetZ,
                endOffsetX,
                endOffsetZ,
                lockRadius,
                hostileOnly
        );
    }

    public Vec3 targetPosition(ServerLevel level, Vec3 markerPosition, int shotIndex, int shotCount, Entity owner) {
        return switch (type) {
            case PRECISE -> markerPosition;
            case RANDOM_AREA -> markerPosition.add(randomDiscOffset(level, radius));
            case VECTOR_PATH -> markerPosition.add(vectorPathOffset(shotIndex, shotCount));
            case AUTO_LOCK -> autoLockTarget(level, markerPosition, owner).orElse(markerPosition);
        };
    }

    @Deprecated
    public Vec3 offset(ServerLevel level) {
        return randomDiscOffset(level, radius);
    }

    private Vec3 randomDiscOffset(ServerLevel level, double radius) {
        if (radius <= 0.0D) {
            return Vec3.ZERO;
        }

        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(level.random.nextDouble()) * radius;
        return new Vec3(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private Vec3 vectorPathOffset(int shotIndex, int shotCount) {
        double progress = shotCount <= 1 ? 0.0D : (double) shotIndex / (double) (shotCount - 1);
        double x = startOffsetX + (endOffsetX - startOffsetX) * progress;
        double z = startOffsetZ + (endOffsetZ - startOffsetZ) * progress;
        return new Vec3(x, 0.0D, z);
    }

    private Optional<Vec3> autoLockTarget(ServerLevel level, Vec3 markerPosition, Entity owner) {
        if (lockRadius <= 0.0D) {
            return Optional.empty();
        }

        AABB searchBox = AABB.ofSize(markerPosition, lockRadius * 2.0D, lockRadius * 2.0D, lockRadius * 2.0D);
        return level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> isValidLockTarget(entity, owner))
                .stream()
                .min((left, right) -> Double.compare(left.distanceToSqr(markerPosition), right.distanceToSqr(markerPosition)))
                .map(LivingEntity::position);
    }

    private boolean isValidLockTarget(LivingEntity entity, Entity owner) {
        if (!entity.isAlive() || entity == owner) {
            return false;
        }
        return !hostileOnly || entity instanceof Enemy;
    }
}
