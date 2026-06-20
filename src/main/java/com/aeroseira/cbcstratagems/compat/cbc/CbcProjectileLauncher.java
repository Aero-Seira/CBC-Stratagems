package com.aeroseira.cbcstratagems.compat.cbc;

import com.aeroseira.cbcstratagems.stratagem.StratagemProjectileStackSpec;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.EndFluidStack;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedProjectileBlock;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock;

public final class CbcProjectileLauncher {
    private static final ResourceLocation DEFAULT_FUZE_ID = ResourceLocation.fromNamespaceAndPath("createbigcannons", "impact_fuze");
    private static final double MIN_DIRECTION_LENGTH_SQR = 1.0E-6D;

    private CbcProjectileLauncher() {
    }

    public static CbcProjectileLaunchResult launch(
            ServerLevel level,
            StratagemProjectileStackSpec projectileSpec,
            Vec3 spawnPos,
            Vec3 targetPos,
            float power,
            float spread,
            Entity owner
    ) {
        ItemStack projectileStack = createProjectileStack(level, projectileSpec);
        if (projectileStack.isEmpty()) {
            return CbcProjectileLaunchResult.CREATE_FAILED;
        }
        return launch(level, projectileStack, spawnPos, targetPos, power, spread, owner);
    }

    public static CbcProjectileLaunchResult launch(
            ServerLevel level,
            ItemStack projectileStack,
            Vec3 spawnPos,
            Vec3 targetPos,
            float power,
            float spread,
            Entity owner
    ) {
        if (!(projectileStack.getItem() instanceof BlockItem blockItem)) {
            return CbcProjectileLaunchResult.NOT_BLOCK_ITEM;
        }
        if (!(blockItem.getBlock() instanceof ProjectileBlock<?> projectileBlock)) {
            return CbcProjectileLaunchResult.NOT_CBC_PROJECTILE_BLOCK;
        }

        projectileStack = withDefaultFuzeIfNeeded(projectileStack, projectileBlock);
        AbstractBigCannonProjectile projectile = projectileBlock.getProjectile(level, projectileStack);
        if (projectile == null) {
            return CbcProjectileLaunchResult.CREATE_FAILED;
        }

        Vec3 direction = targetPos.subtract(spawnPos);
        if (direction.lengthSqr() < MIN_DIRECTION_LENGTH_SQR) {
            return CbcProjectileLaunchResult.INVALID_DIRECTION;
        }

        projectile.setOwner(owner);
        projectile.setPos(spawnPos);
        projectile.setChargePower(power);
        Vec3 normalized = direction.normalize();
        projectile.shoot(normalized.x(), normalized.y(), normalized.z(), power, spread);

        return level.addFreshEntity(projectile) ? CbcProjectileLaunchResult.SUCCESS : CbcProjectileLaunchResult.SPAWN_FAILED;
    }

    private static ItemStack createProjectileStack(ServerLevel level, StratagemProjectileStackSpec projectileSpec) {
        Item projectileItem = BuiltInRegistries.ITEM.get(projectileSpec.id());
        if (projectileItem == null) {
            return ItemStack.EMPTY;
        }

        ItemStack projectileStack = new ItemStack(projectileItem, projectileSpec.count());
        projectileSpec.fuze().ifPresent(fuzeSpec -> applyFuze(projectileStack, fuzeSpec));
        projectileSpec.tracer().ifPresent(tracerSpec -> applyTracer(projectileStack, tracerSpec));
        projectileSpec.fluid().ifPresent(fluidSpec -> applyFluid(level, projectileStack, fluidSpec));
        return projectileStack;
    }

    private static void applyFuze(ItemStack projectileStack, StratagemProjectileStackSpec.FuzeSpec fuzeSpec) {
        Item fuzeItem = BuiltInRegistries.ITEM.get(fuzeSpec.id());
        ItemStack fuzeStack = new ItemStack(fuzeItem);
        fuzeSpec.timerTicks().ifPresent(timerTicks -> fuzeStack.set(CBCDataComponents.FUZE_TIMER, timerTicks));
        fuzeSpec.detonationDistance().ifPresent(distance -> fuzeStack.set(CBCDataComponents.DETONATION_DISTANCE, distance));
        projectileStack.set(CBCDataComponents.FUZE, ItemContainerContents.fromItems(List.of(fuzeStack)));
    }

    private static void applyTracer(ItemStack projectileStack, StratagemProjectileStackSpec.TracerSpec tracerSpec) {
        Item tracerItem = BuiltInRegistries.ITEM.get(tracerSpec.id());
        projectileStack.set(CBCDataComponents.TRACER, ItemContainerContents.fromItems(List.of(new ItemStack(tracerItem))));
    }

    private static void applyFluid(ServerLevel level, ItemStack projectileStack, StratagemProjectileStackSpec.FluidSpec fluidSpec) {
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidSpec.id());
        EndFluidStack fluidStack = new EndFluidStack(fluid, fluidSpec.amount(), DataComponentPatch.EMPTY);
        CompoundTag tag = fluidStack.writeTag(new CompoundTag(), level.registryAccess());
        projectileStack.set(CBCDataComponents.FLUID_CONTENT, CustomData.of(tag));
    }

    private static ItemStack withDefaultFuzeIfNeeded(ItemStack projectileStack, ProjectileBlock<?> projectileBlock) {
        if (!(projectileBlock instanceof FuzedProjectileBlock<?, ?>)) {
            return projectileStack;
        }
        if (!projectileStack.getOrDefault(CBCDataComponents.FUZE, ItemContainerContents.EMPTY).copyOne().isEmpty()) {
            return projectileStack;
        }

        Item fuze = BuiltInRegistries.ITEM.get(DEFAULT_FUZE_ID);
        ItemStack copy = projectileStack.copy();
        copy.set(CBCDataComponents.FUZE, ItemContainerContents.fromItems(List.of(new ItemStack(fuze))));
        return copy;
    }
}
