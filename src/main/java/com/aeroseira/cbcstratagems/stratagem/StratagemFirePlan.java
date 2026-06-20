package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record StratagemFirePlan(StratagemFirePlanType type, List<StratagemFirePhase> phases) {
    public static final Codec<StratagemFirePlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StratagemFirePlanType.CODEC.optionalFieldOf("type", StratagemFirePlanType.INSTANT).forGetter(StratagemFirePlan::type),
            StratagemFirePhase.CODEC.listOf().fieldOf("phases").forGetter(StratagemFirePlan::phases)
    ).apply(instance, StratagemFirePlan::new));

    public StratagemFirePlan {
        phases = List.copyOf(phases);
    }

    public static StratagemFirePlan legacy(List<StratagemArtilleryEntry> artillery) {
        return new StratagemFirePlan(StratagemFirePlanType.INSTANT, List.of(new StratagemFirePhase(
                0,
                1,
                0,
                artillery.stream().map(StratagemFireEntry::legacy).toList()
        )));
    }

    public StratagemFirePlan normalized() {
        return new StratagemFirePlan(type, phases.stream().map(StratagemFirePhase::normalized).toList());
    }
}
