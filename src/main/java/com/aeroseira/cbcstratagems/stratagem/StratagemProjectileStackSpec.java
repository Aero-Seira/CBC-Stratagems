package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record StratagemProjectileStackSpec(
        ResourceLocation id,
        int count,
        Optional<FuzeSpec> fuze,
        Optional<TracerSpec> tracer,
        Optional<FluidSpec> fluid
) {
    public static final Codec<StratagemProjectileStackSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StratagemProjectileStackSpec::id),
            Codec.INT.optionalFieldOf("count", 1).forGetter(StratagemProjectileStackSpec::count),
            FuzeSpec.CODEC.optionalFieldOf("fuze").forGetter(StratagemProjectileStackSpec::fuze),
            TracerSpec.CODEC.optionalFieldOf("tracer").forGetter(StratagemProjectileStackSpec::tracer),
            FluidSpec.CODEC.optionalFieldOf("fluid").forGetter(StratagemProjectileStackSpec::fluid)
    ).apply(instance, StratagemProjectileStackSpec::new));

    public static StratagemProjectileStackSpec legacy(StratagemProjectileSpec spec) {
        return new StratagemProjectileStackSpec(spec.id(), spec.count(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public StratagemProjectileStackSpec normalized() {
        return new StratagemProjectileStackSpec(id, count, fuze, tracer, fluid);
    }

    public record FuzeSpec(ResourceLocation id, Optional<Integer> timerTicks, Optional<Integer> detonationDistance) {
        public static final Codec<FuzeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(FuzeSpec::id),
                Codec.INT.optionalFieldOf("timer_ticks").forGetter(FuzeSpec::timerTicks),
                Codec.INT.optionalFieldOf("detonation_distance").forGetter(FuzeSpec::detonationDistance)
        ).apply(instance, FuzeSpec::new));
    }

    public record TracerSpec(ResourceLocation id) {
        public static final Codec<TracerSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(TracerSpec::id)
        ).apply(instance, TracerSpec::new));
    }

    public record FluidSpec(ResourceLocation id, int amount) {
        public static final Codec<FluidSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(FluidSpec::id),
                Codec.INT.fieldOf("amount").forGetter(FluidSpec::amount)
        ).apply(instance, FluidSpec::new));
    }
}
