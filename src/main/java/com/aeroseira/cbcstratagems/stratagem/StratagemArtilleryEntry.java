package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StratagemArtilleryEntry(
        StratagemProjectileSpec projectile,
        int count,
        int intervalTicks,
        int spawnHeight,
        double spawnScatter,
        double targetScatter,
        float power,
        float spread,
        int maxObstructionBlocks
) {
    public static final Codec<StratagemArtilleryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemProjectileSpec.CODEC.fieldOf("projectile").forGetter(StratagemArtilleryEntry::projectile),
            Codec.INT.fieldOf("count").forGetter(StratagemArtilleryEntry::count),
            Codec.INT.fieldOf("interval_ticks").forGetter(StratagemArtilleryEntry::intervalTicks),
            Codec.INT.fieldOf("spawn_height").forGetter(StratagemArtilleryEntry::spawnHeight),
            Codec.DOUBLE.fieldOf("spawn_scatter").forGetter(StratagemArtilleryEntry::spawnScatter),
            Codec.DOUBLE.fieldOf("target_scatter").forGetter(StratagemArtilleryEntry::targetScatter),
            Codec.FLOAT.fieldOf("power").forGetter(StratagemArtilleryEntry::power),
            Codec.FLOAT.fieldOf("spread").forGetter(StratagemArtilleryEntry::spread),
            Codec.INT.optionalFieldOf("max_obstruction_blocks", 8).forGetter(StratagemArtilleryEntry::maxObstructionBlocks)
    ).apply(instance, StratagemArtilleryEntry::new));
}
