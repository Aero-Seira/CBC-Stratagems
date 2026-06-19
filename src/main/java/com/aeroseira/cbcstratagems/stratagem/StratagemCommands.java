package com.aeroseira.cbcstratagems.stratagem;

import com.aeroseira.cbcstratagems.registry.ModDataComponents;
import com.aeroseira.cbcstratagems.registry.ModItems;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class StratagemCommands {
    private StratagemCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StratagemCommands::registerCommands);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cbcstratagems")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("license")
                        .then(Commands.argument("stratagem", ResourceLocationArgument.id())
                                .executes(context -> giveLicense(context.getSource().getPlayerOrException(), ResourceLocationArgument.getId(context, "stratagem"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveLicense(
                                                context.getSource().getPlayerOrException(),
                                                ResourceLocationArgument.getId(context, "stratagem"),
                                                IntegerArgumentType.getInteger(context, "count")
                                        ))
                                )
                        )
                )
        );
    }

    private static int giveLicense(ServerPlayer player, ResourceLocation stratagemId, int count) {
        if (!StratagemRegistry.contains(stratagemId)) {
            player.displayClientMessage(Component.translatable("message.cbc_stratagems.license.unknown_stratagem", stratagemId.toString()), false);
            return 0;
        }

        ItemStack stack = new ItemStack(ModItems.STRATAGEM_LICENSE.get(), count);
        stack.set(ModDataComponents.LICENSE_STRATAGEM, stratagemId);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        player.displayClientMessage(Component.translatable("message.cbc_stratagems.command.license_given", count, stratagemId.toString()), false);
        return count;
    }
}
