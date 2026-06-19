package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record StratagemProjectileSpec(ResourceLocation id, int count) {
    public static final Codec<StratagemProjectileSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StratagemProjectileSpec::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(StratagemProjectileSpec::count)
    ).apply(instance, StratagemProjectileSpec::new));
}
