package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StratagemLaunchSpec(
        int spawnHeight,
        double spawnScatter,
        float power,
        float spread
) {
    public static final int DEFAULT_SPAWN_HEIGHT = 96;
    public static final double DEFAULT_SPAWN_SCATTER = 0.0D;
    public static final float DEFAULT_POWER = 8.0F;
    public static final float DEFAULT_SPREAD = 0.0F;

    public static final StratagemLaunchSpec DEFAULT = new StratagemLaunchSpec(
            DEFAULT_SPAWN_HEIGHT,
            DEFAULT_SPAWN_SCATTER,
            DEFAULT_POWER,
            DEFAULT_SPREAD
    );

    public static final Codec<StratagemLaunchSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("spawn_height", DEFAULT.spawnHeight()).forGetter(StratagemLaunchSpec::spawnHeight),
            Codec.DOUBLE.optionalFieldOf("spawn_scatter", DEFAULT.spawnScatter()).forGetter(StratagemLaunchSpec::spawnScatter),
            Codec.FLOAT.optionalFieldOf("power", DEFAULT.power()).forGetter(StratagemLaunchSpec::power),
            Codec.FLOAT.optionalFieldOf("spread", DEFAULT.spread()).forGetter(StratagemLaunchSpec::spread)
    ).apply(instance, StratagemLaunchSpec::new));

    public static StratagemLaunchSpec legacy(StratagemArtilleryEntry entry) {
        return new StratagemLaunchSpec(entry.spawnHeight(), entry.spawnScatter(), entry.power(), entry.spread());
    }

    public StratagemLaunchSpec normalized() {
        return new StratagemLaunchSpec(spawnHeight, spawnScatter, power, spread);
    }
}
