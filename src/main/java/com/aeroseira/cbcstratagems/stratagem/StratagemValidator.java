package com.aeroseira.cbcstratagems.stratagem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;
import rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem;

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
        if (definition.firePlan().phases().isEmpty()) {
            errors.add("fire_plan.phases must not be empty");
        }

        for (int i = 0; i < definition.firePlan().phases().size(); i++) {
            validateFirePhase("fire_plan.phases[" + i + "]", definition.firePlan().phases().get(i), errors);
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

    private static void validateFirePhase(String path, StratagemFirePhase phase, List<String> errors) {
        if (phase.delayTicks() < 0) {
            errors.add(path + ".delay_ticks must be >= 0");
        }
        if (phase.iterations() < 1) {
            errors.add(path + ".iterations must be >= 1");
        }
        if (phase.iterationIntervalTicks() < 0) {
            errors.add(path + ".iteration_interval_ticks must be >= 0");
        }
        if (phase.entries().isEmpty()) {
            errors.add(path + ".elements must not be empty");
        }

        for (int i = 0; i < phase.entries().size(); i++) {
            validateFireEntry(path + ".elements[" + i + "]", phase.entries().get(i), errors);
        }
    }

    private static void validateFireEntry(String path, StratagemFireEntry entry, List<String> errors) {
        validateProjectileStack(path + ".projectile_stack", entry.projectileStack(), errors);

        if (entry.shots() < 1) {
            errors.add(path + ".shots must be >= 1");
        }
        if (entry.shotIntervalTicks() < 0) {
            errors.add(path + ".shot_interval_ticks must be >= 0");
        }

        validateTargeting(path + ".targeting", entry.targeting(), errors);
        validateTrajectory(path + ".trajectory", entry.trajectory(), errors);
        validateLaunch(path + ".launch", entry.launch(), errors);
    }

    private static void validateProjectileStack(String path, StratagemProjectileStackSpec spec, List<String> errors) {
        if (spec.count() < 1) {
            errors.add(path + ".count must be >= 1");
        }

        BuiltInRegistries.ITEM.getOptional(spec.id()).ifPresentOrElse(item -> validateProjectileItem(path + ".id", spec.id().toString(), item, errors),
                () -> errors.add(path + ".id is not a registered item: " + spec.id()));

        spec.fuze().ifPresent(fuze -> BuiltInRegistries.ITEM.getOptional(fuze.id()).ifPresentOrElse(item -> {
            if (!(item instanceof FuzeItem)) {
                errors.add(path + ".fuze.id is not a CBC fuze item: " + fuze.id());
            }
            fuze.timerTicks().ifPresent(timerTicks -> {
                if (timerTicks < 0) {
                    errors.add(path + ".fuze.timer_ticks must be >= 0");
                }
            });
            fuze.detonationDistance().ifPresent(distance -> {
                if (distance < 1) {
                    errors.add(path + ".fuze.detonation_distance must be >= 1");
                }
            });
        }, () -> errors.add(path + ".fuze.id is not a registered item: " + fuze.id())));

        spec.tracer().ifPresent(tracer -> BuiltInRegistries.ITEM.getOptional(tracer.id()).ifPresentOrElse(item -> {
            if (item == Items.AIR) {
                errors.add(path + ".tracer.id must not be minecraft:air");
            }
        }, () -> errors.add(path + ".tracer.id is not a registered item: " + tracer.id())));

        spec.fluid().ifPresent(fluid -> {
            if (fluid.amount() < 1) {
                errors.add(path + ".fluid.amount must be >= 1");
            }
            BuiltInRegistries.FLUID.getOptional(fluid.id()).ifPresentOrElse(registeredFluid -> {
                if (registeredFluid == Fluids.EMPTY) {
                    errors.add(path + ".fluid.id must not be minecraft:empty");
                }
            }, () -> errors.add(path + ".fluid.id is not a registered fluid: " + fluid.id()));
        });
    }

    private static void validateProjectileItem(String path, String encodedId, Item item, List<String> errors) {
        if (item == Items.AIR) {
            errors.add(path + " must not be minecraft:air");
            return;
        }
        if (!(item instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ProjectileBlock<?>)) {
            errors.add(path + " is not a CBC big cannon projectile item: " + encodedId);
        }
    }

    private static void validateTargeting(String path, StratagemTargetingSpec targeting, List<String> errors) {
        if (targeting.radius() < 0.0D) {
            errors.add(path + ".radius must be >= 0");
        }
        if (targeting.lockRadius() < 0.0D) {
            errors.add(path + ".lock_radius must be >= 0");
        }
        if (targeting.type() == StratagemTargetingType.AUTO_LOCK && targeting.lockRadius() <= 0.0D) {
            errors.add(path + ".lock_radius must be > 0 for auto_lock targeting");
        }
    }

    private static void validateTrajectory(String path, StratagemTrajectorySpec trajectory, List<String> errors) {
        if (trajectory.maxObstructionBlocks() < 0) {
            errors.add(path + ".max_obstruction_blocks must be >= 0");
        }
        if (trajectory.spawnDistance() < 0.0D) {
            errors.add(path + ".spawn_distance must be >= 0");
        }
        if (trajectory.searchRadius() < 0.0D) {
            errors.add(path + ".search_radius must be >= 0");
        }
        if (trajectory.minElevationDegrees() <= 0.0D || trajectory.minElevationDegrees() >= 90.0D) {
            errors.add(path + ".min_elevation_degrees must be > 0 and < 90");
        }
        if (trajectory.bearingSteps() < 1) {
            errors.add(path + ".bearing_steps must be >= 1");
        }
        if (trajectory.radiusSteps() < 1) {
            errors.add(path + ".radius_steps must be >= 1");
        }
    }

    private static void validateLaunch(String path, StratagemLaunchSpec launch, List<String> errors) {
        if (launch.spawnHeight() <= 0) {
            errors.add(path + ".spawn_height must be > 0");
        }
        if (launch.spawnScatter() < 0.0D) {
            errors.add(path + ".spawn_scatter must be >= 0");
        }
        if (launch.power() <= 0.0F) {
            errors.add(path + ".power must be > 0");
        }
        if (launch.spread() < 0.0F) {
            errors.add(path + ".spread must be >= 0");
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
