package com.aeroseira.cbcstratagems.item;

import com.aeroseira.cbcstratagems.entity.StratagemBeaconProjectile;
import com.aeroseira.cbcstratagems.player.PlayerStratagemDataManager;
import com.aeroseira.cbcstratagems.registry.ModDataComponents;
import com.aeroseira.cbcstratagems.registry.ModItems;
import com.aeroseira.cbcstratagems.registry.ModSoundEvents;
import com.aeroseira.cbcstratagems.stratagem.StratagemEnvironment;
import com.aeroseira.cbcstratagems.stratagem.StratagemRegistry;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class StratagemDeviceItem extends Item {
    public StratagemDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack deviceStack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(deviceStack);
        }

        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.is(ModItems.STRATAGEM_LICENSE.get())) {
            return useDevice(level, player, usedHand, deviceStack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            unlockFromLicense(serverPlayer, offhandStack);
        }

        return InteractionResultHolder.sidedSuccess(deviceStack, level.isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            StratagemInputManager.cancel(player);
        }
    }

    private static InteractionResultHolder<ItemStack> useDevice(Level level, Player player, InteractionHand usedHand, ItemStack deviceStack) {
        StratagemDeviceMode mode = deviceStack.getOrDefault(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.CALLER);
        if (mode == StratagemDeviceMode.BEACON) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                throwBeacon(serverPlayer, deviceStack);
            }
            return InteractionResultHolder.sidedSuccess(deviceStack, level.isClientSide());
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            StratagemInputManager.start(serverPlayer);
        }
        return InteractionResultHolder.consume(deviceStack);
    }

    private static void throwBeacon(ServerPlayer player, ItemStack deviceStack) {
        ResourceLocation stratagemId = deviceStack.get(ModDataComponents.SELECTED_STRATAGEM);
        if (stratagemId == null) {
            resetDevice(deviceStack);
            player.displayClientMessage(Component.translatable("message.cbc_stratagems.beacon.missing_stratagem"), true);
            return;
        }

        StratagemRegistry.get(stratagemId).ifPresentOrElse(definition -> {
            if (!StratagemEnvironment.hasOpenSky(player.serverLevel(), player.blockPosition())) {
                player.displayClientMessage(Component.translatable("message.cbc_stratagems.input.no_sky"), true);
                return;
            }

            StratagemBeaconProjectile beacon = new StratagemBeaconProjectile(player.level(), player, stratagemId);
            beacon.setItem(new ItemStack(ModItems.STRATAGEM_DEVICE.get()));
            beacon.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            player.level().addFreshEntity(beacon);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSoundEvents.BEACON_THROW.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            resetDevice(deviceStack);
        }, () -> {
            resetDevice(deviceStack);
            player.displayClientMessage(Component.translatable("message.cbc_stratagems.beacon.unknown_stratagem", stratagemId.toString()), true);
        });
    }

    private static void resetDevice(ItemStack deviceStack) {
        deviceStack.set(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.CALLER);
        deviceStack.remove(ModDataComponents.SELECTED_STRATAGEM);
    }

    private static void unlockFromLicense(ServerPlayer player, ItemStack licenseStack) {
        ResourceLocation stratagemId = licenseStack.get(ModDataComponents.LICENSE_STRATAGEM);
        if (stratagemId == null) {
            player.displayClientMessage(Component.translatable("message.cbc_stratagems.license.missing_stratagem"), true);
            return;
        }

        StratagemRegistry.get(stratagemId).ifPresentOrElse(definition -> {
            if (!PlayerStratagemDataManager.unlock(player, stratagemId)) {
                player.displayClientMessage(Component.translatable("message.cbc_stratagems.license.already_unlocked", definition.name()), true);
                return;
            }

            if (!player.getAbilities().instabuild) {
                licenseStack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.cbc_stratagems.license.unlocked", definition.name()), true);
        }, () -> player.displayClientMessage(
                Component.translatable("message.cbc_stratagems.license.unknown_stratagem", stratagemId.toString()),
                true
        ));
    }
}
