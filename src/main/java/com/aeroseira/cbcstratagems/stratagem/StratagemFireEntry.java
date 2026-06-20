package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StratagemFireEntry(
        StratagemProjectileStackSpec projectileStack,
        int shots,
        int shotIntervalTicks,
        StratagemTargetingSpec targeting,
        StratagemTrajectorySpec trajectory,
        StratagemLaunchSpec launch
) {
    public static final Codec<StratagemFireEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemProjectileStackSpec.CODEC.fieldOf("projectile_stack").forGetter(StratagemFireEntry::projectileStack),
            Codec.INT.optionalFieldOf("shots", 1).forGetter(StratagemFireEntry::shots),
            Codec.INT.optionalFieldOf("shot_interval_ticks", 0).forGetter(StratagemFireEntry::shotIntervalTicks),
            StratagemTargetingSpec.CODEC.optionalFieldOf("targeting", StratagemTargetingSpec.DEFAULT).forGetter(StratagemFireEntry::targeting),
            StratagemTrajectorySpec.CODEC.optionalFieldOf("trajectory", StratagemTrajectorySpec.DEFAULT).forGetter(StratagemFireEntry::trajectory),
            StratagemLaunchSpec.CODEC.optionalFieldOf("launch", StratagemLaunchSpec.DEFAULT).forGetter(StratagemFireEntry::launch)
    ).apply(instance, StratagemFireEntry::new));

    public static StratagemFireEntry legacy(StratagemArtilleryEntry entry) {
        return new StratagemFireEntry(
                StratagemProjectileStackSpec.legacy(entry.projectile()),
                entry.count(),
                entry.intervalTicks(),
                StratagemTargetingSpec.legacy(entry.targetScatter()),
                StratagemTrajectorySpec.legacy(entry),
                StratagemLaunchSpec.legacy(entry)
        );
    }

    public StratagemFireEntry normalized() {
        return new StratagemFireEntry(
                projectileStack.normalized(),
                shots,
                shotIntervalTicks,
                targeting.normalized(),
                trajectory.normalized(),
                launch.normalized()
        );
    }
}
