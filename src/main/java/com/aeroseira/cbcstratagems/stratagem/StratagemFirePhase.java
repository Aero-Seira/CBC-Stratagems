package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

public record StratagemFirePhase(
        int delayTicks,
        int iterations,
        int iterationIntervalTicks,
        List<StratagemFireEntry> entries
) {
    public static final Codec<StratagemFirePhase> CODEC = JsonPhase.CODEC.flatXmap(
            JsonPhase::toPhase,
            phase -> DataResult.success(JsonPhase.fromPhase(phase))
    );

    public StratagemFirePhase {
        entries = List.copyOf(entries);
    }

    public StratagemFirePhase normalized() {
        return new StratagemFirePhase(
                delayTicks,
                iterations,
                iterationIntervalTicks,
                entries.stream().map(StratagemFireEntry::normalized).toList()
        );
    }

    private record JsonPhase(
            int delayTicks,
            int iterations,
            int iterationIntervalTicks,
            Optional<List<StratagemFireEntry>> elements,
            Optional<List<StratagemFireEntry>> entries
    ) {
        private static final Codec<JsonPhase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(JsonPhase::delayTicks),
                Codec.INT.optionalFieldOf("iterations", 1).forGetter(JsonPhase::iterations),
                Codec.INT.optionalFieldOf("iteration_interval_ticks", 0).forGetter(JsonPhase::iterationIntervalTicks),
                StratagemFireEntry.CODEC.listOf().optionalFieldOf("elements").forGetter(JsonPhase::elements),
                StratagemFireEntry.CODEC.listOf().optionalFieldOf("entries").forGetter(JsonPhase::entries)
        ).apply(instance, JsonPhase::new));

        private static JsonPhase fromPhase(StratagemFirePhase phase) {
            return new JsonPhase(
                    phase.delayTicks(),
                    phase.iterations(),
                    phase.iterationIntervalTicks(),
                    Optional.of(phase.entries()),
                    Optional.empty()
            );
        }

        private DataResult<StratagemFirePhase> toPhase() {
            if (elements.isPresent() && entries.isPresent()) {
                return DataResult.error(() -> "fire phase must not define both elements and legacy entries");
            }
            if (elements.isEmpty() && entries.isEmpty()) {
                return DataResult.error(() -> "fire phase must define elements or legacy entries");
            }
            return DataResult.success(new StratagemFirePhase(
                    delayTicks,
                    iterations,
                    iterationIntervalTicks,
                    elements.orElseGet(entries::orElseThrow)
            ));
        }
    }
}
