package com.aeroseira.cbcstratagems.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.aeroseira.cbcstratagems.item.StratagemDeviceMode;
import com.aeroseira.cbcstratagems.network.ServerboundStratagemInputControlPacket;
import com.aeroseira.cbcstratagems.network.ServerboundStratagemInputPacket;
import com.aeroseira.cbcstratagems.registry.ModDataComponents;
import com.aeroseira.cbcstratagems.registry.ModItems;
import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputFeedback;
import com.aeroseira.cbcstratagems.stratagem.input.StratagemInputStatus;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class StratagemInputClient {
    private static boolean controlActive;
    private static boolean completedUntilRelease;
    private static boolean inputBlockedUntilRelease;
    private static boolean rightMouseHeld;
    private static int activeSlot = -1;
    private static boolean rawUpDown;
    private static boolean rawDownDown;
    private static boolean rawLeftDown;
    private static boolean rawRightDown;
    private static boolean wasUpDown;
    private static boolean wasDownDown;
    private static boolean wasLeftDown;
    private static boolean wasRightDown;

    private StratagemInputClient() {
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        captureRawMovementKeys(minecraft);
        updateInputLifecycle(minecraft);
        if (StratagemClientState.isInputActive()) {
            suppressMovementKeys(minecraft);
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        StratagemClientState.tickInputFeedback();
        updateInputLifecycle(minecraft);

        if (minecraft.player == null || minecraft.screen != null || !StratagemClientState.isInputActive()) {
            restoreMovementKeys(minecraft);
            rememberKeyStates(minecraft);
            return;
        }
        if (StratagemClientState.isInputBlocked()) {
            rememberKeyStates(minecraft);
            return;
        }

        sendOnRisingEdge(StratagemCommand.UP, rawUpDown, wasUpDown);
        sendOnRisingEdge(StratagemCommand.DOWN, rawDownDown, wasDownDown);
        sendOnRisingEdge(StratagemCommand.LEFT, rawLeftDown, wasLeftDown);
        sendOnRisingEdge(StratagemCommand.RIGHT, rawRightDown, wasRightDown);
        rememberKeyStates(minecraft);
    }

    private static void updateInputLifecycle(Minecraft minecraft) {
        boolean useHeld = isUseHeld(minecraft);
        if (!useHeld) {
            closeInput(true);
            completedUntilRelease = false;
            inputBlockedUntilRelease = false;
            return;
        }

        boolean callerDeviceHeld = isCallerDeviceHeld(minecraft);
        boolean selectedSlotStillActive = activeSlot < 0 || minecraft.player != null && minecraft.player.getInventory().selected == activeSlot;
        if (StratagemClientState.inputStatus() == StratagemInputStatus.FAILED) {
            inputBlockedUntilRelease = true;
            closeInput(false);
            return;
        }
        if (StratagemClientState.shouldRenderInputOverlay() && (!selectedSlotStillActive || minecraft.screen != null)) {
            closeInput(true);
            return;
        }

        if (StratagemClientState.inputStatus() == StratagemInputStatus.COMPLETE) {
            completedUntilRelease = true;
            if (!isStratagemDeviceHeld(minecraft)) {
                closeInput(false);
            } else {
                suspendInputControl();
            }
            return;
        }

        if ((controlActive || StratagemClientState.isInputActive()) && selectedSlotStillActive && isBeaconDeviceHeld(minecraft)) {
            completedUntilRelease = true;
            suspendInputControl();
            return;
        }
        if ((controlActive || StratagemClientState.isInputActive()) && (!callerDeviceHeld || !selectedSlotStillActive || minecraft.screen != null)) {
            closeInput(true);
            return;
        }

        boolean shouldBeOpen = minecraft.player != null
                && minecraft.screen == null
                && callerDeviceHeld
                && !completedUntilRelease
                && !inputBlockedUntilRelease;

        if (shouldBeOpen && !controlActive) {
            controlActive = true;
            activeSlot = minecraft.player.getInventory().selected;
            if (hasLocalOpenSky(minecraft)) {
                StratagemClientState.beginLocalInput();
            } else {
                StratagemClientState.beginLocalBlockedInput(Component.translatable("message.cbc_stratagems.input.no_sky"));
            }
            PacketDistributor.sendToServer(new ServerboundStratagemInputControlPacket(true));
        } else if (!shouldBeOpen && controlActive) {
            closeInput(true);
        }

        if (controlActive && !StratagemClientState.isInputActive()) {
            StratagemClientState.beginLocalInput();
            PacketDistributor.sendToServer(new ServerboundStratagemInputControlPacket(true));
        }
    }

    public static void onInteractionKeyMapping(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        if (isBeaconDeviceHeld(minecraft)
                && !completedUntilRelease
                && !inputBlockedUntilRelease
                && !controlActive
                && !StratagemClientState.isInputActive()) {
            inputBlockedUntilRelease = true;
            return;
        }

        if (!shouldCancelUseItem(minecraft)) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightMouseHeld = event.getAction() != GLFW.GLFW_RELEASE;
        }
    }

    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!StratagemClientState.isInputActive()) {
            return;
        }

        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!StratagemClientState.shouldRenderInputOverlay()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int y = Math.max(12, minecraft.getWindow().getGuiScaledHeight() / 5);

        List<StratagemCommand> input = StratagemClientState.currentInput();
        Component title = StratagemClientState.inputMessage();
        if (title.getString().isEmpty()) {
            title = Component.translatable("overlay.cbc_stratagems.input.active");
        }

        String sequence = formatInput(input);
        int panelWidth = Math.max(minecraft.font.width(title), minecraft.font.width(sequence)) + 24;
        int x = (width - panelWidth) / 2;

        graphics.fill(x, y, x + panelWidth, y + 32, 0xAA000000);
        graphics.drawCenteredString(minecraft.font, title, width / 2, y + 6, 0xFFFFFF);
        graphics.drawCenteredString(minecraft.font, sequence, width / 2, y + 18, statusColor());
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        closeInput(false);
        completedUntilRelease = false;
        inputBlockedUntilRelease = false;
        rightMouseHeld = false;
        wasUpDown = false;
        wasDownDown = false;
        wasLeftDown = false;
        wasRightDown = false;
    }

    private static void closeInput(boolean notifyServer) {
        if (notifyServer && (controlActive || StratagemClientState.isInputActive())) {
            PacketDistributor.sendToServer(new ServerboundStratagemInputControlPacket(false));
        }
        controlActive = false;
        activeSlot = -1;
        StratagemClientState.clearInputState();
    }

    private static void suspendInputControl() {
        controlActive = false;
    }

    private static void sendOnRisingEdge(StratagemCommand command, boolean isDown, boolean wasDown) {
        if (isDown && !wasDown) {
            StratagemClientState.clearTransientInputFeedback();
            PacketDistributor.sendToServer(new ServerboundStratagemInputPacket(command));
        }
    }

    private static void rememberKeyStates(Minecraft minecraft) {
        if (minecraft.options == null) {
            wasUpDown = false;
            wasDownDown = false;
            wasLeftDown = false;
            wasRightDown = false;
            return;
        }

        wasUpDown = rawUpDown;
        wasDownDown = rawDownDown;
        wasLeftDown = rawLeftDown;
        wasRightDown = rawRightDown;
    }

    private static void captureRawMovementKeys(Minecraft minecraft) {
        if (minecraft.options == null || minecraft.getWindow() == null) {
            rawUpDown = false;
            rawDownDown = false;
            rawLeftDown = false;
            rawRightDown = false;
            return;
        }

        long window = minecraft.getWindow().getWindow();
        rawUpDown = isPhysicallyDown(window, minecraft.options.keyUp);
        rawDownDown = isPhysicallyDown(window, minecraft.options.keyDown);
        rawLeftDown = isPhysicallyDown(window, minecraft.options.keyLeft);
        rawRightDown = isPhysicallyDown(window, minecraft.options.keyRight);
    }

    private static boolean isPhysicallyDown(long window, KeyMapping keyMapping) {
        InputConstants.Key key = keyMapping.getKey();
        return switch (key.getType()) {
            case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> keyMapping.isDown();
            case SCANCODE -> keyMapping.isDown();
        };
    }

    private static void suppressMovementKeys(Minecraft minecraft) {
        if (minecraft.options == null) {
            return;
        }

        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
    }

    private static void restoreMovementKeys(Minecraft minecraft) {
        if (minecraft.options == null) {
            return;
        }

        minecraft.options.keyUp.setDown(rawUpDown);
        minecraft.options.keyDown.setDown(rawDownDown);
        minecraft.options.keyLeft.setDown(rawLeftDown);
        minecraft.options.keyRight.setDown(rawRightDown);
    }

    private static boolean isCallerDeviceHeld(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (!mainHand.is(ModItems.STRATAGEM_DEVICE.get())) {
            return false;
        }
        if (minecraft.player.getOffhandItem().is(ModItems.STRATAGEM_LICENSE.get())) {
            return false;
        }
        return mainHand.getOrDefault(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.CALLER) == StratagemDeviceMode.CALLER
                && !mainHand.has(ModDataComponents.SELECTED_STRATAGEM);
    }

    private static boolean isBeaconDeviceHeld(Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        ItemStack mainHand = minecraft.player.getMainHandItem();
        return mainHand.is(ModItems.STRATAGEM_DEVICE.get())
                && mainHand.getOrDefault(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.CALLER) == StratagemDeviceMode.BEACON;
    }

    private static boolean isStratagemDeviceHeld(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.getMainHandItem().is(ModItems.STRATAGEM_DEVICE.get());
    }

    private static boolean shouldCancelUseItem(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (!mainHand.is(ModItems.STRATAGEM_DEVICE.get())) {
            return false;
        }
        if (minecraft.player.getOffhandItem().is(ModItems.STRATAGEM_LICENSE.get())) {
            return completedUntilRelease || inputBlockedUntilRelease || StratagemClientState.isInputActive();
        }
        StratagemDeviceMode mode = mainHand.getOrDefault(ModDataComponents.DEVICE_MODE, StratagemDeviceMode.CALLER);
        return mode == StratagemDeviceMode.CALLER || completedUntilRelease || inputBlockedUntilRelease || StratagemClientState.isInputActive();
    }

    private static boolean isUseHeld(Minecraft minecraft) {
        return rightMouseHeld
                || minecraft.options != null && minecraft.options.keyUse.isDown()
                || minecraft.mouseHandler != null && minecraft.mouseHandler.isRightPressed();
    }

    private static String formatInput(List<StratagemCommand> input) {
        if (input.isEmpty()) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        for (StratagemCommand command : input) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(switch (command) {
                case UP -> "↑";
                case DOWN -> "↓";
                case LEFT -> "←";
                case RIGHT -> "→";
            });
        }
        return builder.toString();
    }

    private static int statusColor() {
        if (StratagemClientState.inputFeedback() == StratagemInputFeedback.ERROR) {
            return 0xFF5555;
        }
        if (StratagemClientState.inputFeedback() == StratagemInputFeedback.BLOCKED) {
            return 0xFF5555;
        }
        if (StratagemClientState.inputFeedback() == StratagemInputFeedback.COOLDOWN) {
            return 0xFFFF55;
        }
        return switch (StratagemClientState.inputStatus()) {
            case COMPLETE -> 0x55FF55;
            case FAILED -> 0xFF5555;
            case ACTIVE -> 0xFFFFFF;
            case INACTIVE -> 0xAAAAAA;
        };
    }

    private static boolean hasLocalOpenSky(Minecraft minecraft) {
        return minecraft.player != null
                && !minecraft.player.level().dimensionType().hasCeiling()
                && minecraft.player.level().canSeeSky(minecraft.player.blockPosition());
    }
}
