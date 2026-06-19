package com.aeroseira.cbcstratagems.stratagem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

public final class StratagemValidator {
    private StratagemValidator() {
    }

    public static List<String> validateDefinition(StratagemDefinition definition) {
        List<String> errors = new ArrayList<>();

        if (definition.command().isEmpty()) {
            errors.add("command must not be empty");
        }
        if (definition.cooldownTicks() < 0) {
            errors.add("cooldown_ticks must be >= 0");
        }
        if (definition.countdownTicks() < 0) {
            errors.add("countdown_ticks must be >= 0");
        }
        if (definition.artillery().isEmpty()) {
            errors.add("artillery must not be empty");
        }

        for (int i = 0; i < definition.artillery().size(); i++) {
            validateArtilleryEntry("artillery[" + i + "]", definition.artillery().get(i), errors);
        }

        return errors;
    }

    public static List<String> validateNoPrefixConflicts(Collection<StratagemDefinition> definitions) {
        List<String> errors = new ArrayList<>();
        List<StratagemDefinition> ordered = List.copyOf(definitions);

        for (int i = 0; i < ordered.size(); i++) {
            for (int j = i + 1; j < ordered.size(); j++) {
                StratagemDefinition left = ordered.get(i);
                StratagemDefinition right = ordered.get(j);
                if (isPrefix(left.command(), right.command())) {
                    errors.add(left.id() + " command is a prefix of " + right.id());
                } else if (isPrefix(right.command(), left.command())) {
                    errors.add(right.id() + " command is a prefix of " + left.id());
                }
            }
        }

        return errors;
    }

    private static void validateArtilleryEntry(String path, StratagemArtilleryEntry entry, List<String> errors) {
        if (entry.projectile().count() < 1) {
            errors.add(path + ".projectile.count must be >= 1");
        }
        BuiltInRegistries.ITEM.getOptional(entry.projectile().id()).ifPresentOrElse(item -> {
            if (item == Items.AIR) {
                errors.add(path + ".projectile.id must not be minecraft:air");
            }
        }, () -> errors.add(path + ".projectile.id is not a registered item: " + entry.projectile().id()));

        if (entry.count() < 1) {
            errors.add(path + ".count must be >= 1");
        }
        if (entry.intervalTicks() < 0) {
            errors.add(path + ".interval_ticks must be >= 0");
        }
        if (entry.spawnHeight() <= 0) {
            errors.add(path + ".spawn_height must be > 0");
        }
        if (entry.spawnScatter() < 0.0) {
            errors.add(path + ".spawn_scatter must be >= 0");
        }
        if (entry.targetScatter() < 0.0) {
            errors.add(path + ".target_scatter must be >= 0");
        }
        if (entry.power() <= 0.0F) {
            errors.add(path + ".power must be > 0");
        }
        if (entry.spread() < 0.0F) {
            errors.add(path + ".spread must be >= 0");
        }
        if (entry.maxObstructionBlocks() < 0) {
            errors.add(path + ".max_obstruction_blocks must be >= 0");
        }
    }

    private static boolean isPrefix(List<StratagemCommand> possiblePrefix, List<StratagemCommand> command) {
        if (possiblePrefix.size() > command.size()) {
            return false;
        }
        for (int i = 0; i < possiblePrefix.size(); i++) {
            if (possiblePrefix.get(i) != command.get(i)) {
                return false;
            }
        }
        return true;
    }
}
