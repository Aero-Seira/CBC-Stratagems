package com.aeroseira.cbcstratagems.client;

import com.aeroseira.cbcstratagems.CBCStratagems;
import com.aeroseira.cbcstratagems.entity.StratagemMarkerEntity;
import com.aeroseira.cbcstratagems.stratagem.StratagemDefinitionSummary;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class StratagemMarkerOverlay {
    private static final ResourceLocation THEME = CBCStratagems.id("cbc_stratagems/ui/default.json");
    private static final ResourceLocation FALLBACK_ICON = CBCStratagems.id("textures/item/stratagem_device.png");

    private static MarkerOverlayLayout layout;

    private StratagemMarkerOverlay() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }

        MarkerOverlayLayout currentLayout = layout(minecraft);
        StratagemMarkerEntity marker = findFocusedMarker(minecraft, currentLayout);
        if (marker == null) {
            return;
        }

        renderMarker(event.getGuiGraphics(), minecraft.font, minecraft, marker, currentLayout);
    }

    private static StratagemMarkerEntity findFocusedMarker(Minecraft minecraft, MarkerOverlayLayout layout) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getViewVector(1.0F).normalize();
        double maxDistanceSqr = layout.maxDistance() * layout.maxDistance();
        double minDot = Math.cos(Math.toRadians(layout.maxAngleDegrees()));

        StratagemMarkerEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof StratagemMarkerEntity marker)) {
                continue;
            }

            Vec3 target = closestBeamPoint(marker, eye);
            Vec3 toMarker = target.subtract(eye);
            double distanceSqr = toMarker.lengthSqr();
            if (distanceSqr > maxDistanceSqr) {
                continue;
            }
            if (toMarker.lengthSqr() < 1.0E-6D) {
                continue;
            }

            double dot = look.dot(toMarker.normalize());
            if (dot < minDot) {
                continue;
            }

            double distanceScore = 1.0D - Math.min(1.0D, Math.sqrt(distanceSqr) / layout.maxDistance());
            double score = dot * 2.0D + distanceScore;
            if (score > bestScore) {
                best = marker;
                bestScore = score;
            }
        }
        return best;
    }

    private static Vec3 closestBeamPoint(StratagemMarkerEntity marker, Vec3 eye) {
        double y = Math.max(marker.getY(), Math.min(marker.getY() + 256.0D, eye.y()));
        return new Vec3(marker.getX(), y, marker.getZ());
    }

    private static void renderMarker(
            GuiGraphics graphics,
            Font font,
            Minecraft minecraft,
            StratagemMarkerEntity marker,
            MarkerOverlayLayout layout
    ) {
        StratagemDefinitionSummary summary = marker.stratagemId() == null ? null : StratagemClientState.get(marker.stratagemId()).orElse(null);
        Component name = markerName(marker, summary);
        Component countdown = countdownText(marker.remainingTicks());
        ResourceLocation icon = summary == null ? layout.iconPlaceholder() : summary.icon();

        ScreenPoint screenPoint = projectBeamPoint(minecraft, graphics, marker, layout);
        if (screenPoint == null) {
            return;
        }

        int x = Math.round(screenPoint.x()) + layout.offsetX() - layout.width() / 2;
        int y = Math.round(screenPoint.y()) + layout.offsetY() - layout.height() / 2;
        x = Mth.clamp(x, layout.edgePadding(), graphics.guiWidth() - layout.width() - layout.edgePadding());
        y = Mth.clamp(y, layout.edgePadding(), graphics.guiHeight() - layout.height() - layout.edgePadding());

        graphics.fill(x, y, x + layout.width(), y + layout.height(), layout.backgroundColor());
        graphics.blit(
                icon,
                x + layout.iconX(),
                y + layout.iconY(),
                layout.iconSize(),
                layout.iconSize(),
                0.0F,
                0.0F,
                layout.iconSourceWidth(),
                layout.iconSourceHeight(),
                layout.iconTextureWidth(),
                layout.iconTextureHeight()
        );

        graphics.drawString(font, name, x + layout.nameX(), y + layout.nameY(), layout.nameColor(), false);
        graphics.drawString(font, countdown, x + layout.countdownX(), y + layout.countdownY(), layout.countdownColor(), false);
    }

    private static ScreenPoint projectBeamPoint(
            Minecraft minecraft,
            GuiGraphics graphics,
            StratagemMarkerEntity marker,
            MarkerOverlayLayout layout
    ) {
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 basePoint = closestBeamPoint(marker, eye);
        double beamY = Mth.clamp(basePoint.y() + layout.beamYoffset(), marker.getY(), marker.getY() + 256.0D);
        Vec3 relative = new Vec3(marker.getX(), beamY, marker.getZ()).subtract(camera);

        Vec3 forward = minecraft.player.getViewVector(1.0F).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        double depth = relative.dot(forward);
        if (depth <= 0.05D) {
            return null;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        double aspect = (double) screenWidth / Math.max(1, screenHeight);
        double fov = minecraft.options.fov().get();
        double tan = Math.tan(Math.toRadians(fov) * 0.5D);
        double horizontal = relative.dot(right);
        double vertical = relative.dot(up);

        float x = (float)(screenWidth * 0.5D + horizontal / (depth * tan * aspect) * screenWidth * 0.5D);
        float projectedY = (float)(screenHeight * 0.5D - vertical / (depth * tan) * screenHeight * 0.5D);
        return new ScreenPoint(x, projectedY);
    }

    private static Component markerName(StratagemMarkerEntity marker, StratagemDefinitionSummary summary) {
        if (summary != null) {
            return summary.name();
        }
        ResourceLocation stratagemId = marker.stratagemId();
        return stratagemId == null
                ? Component.translatable("entity.cbc_stratagems.stratagem_marker")
                : Component.literal(stratagemId.toString());
    }

    private static Component countdownText(int remainingTicks) {
        if (remainingTicks <= 0) {
            return Component.translatable("overlay.cbc_stratagems.marker.impact");
        }

        return Component.translatable("overlay.cbc_stratagems.marker.countdown", formatClock(remainingTicks));
    }

    private static String formatClock(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int seconds = totalSeconds % 60;
        return totalSeconds / 60 + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private static MarkerOverlayLayout layout(Minecraft minecraft) {
        if (layout == null) {
            layout = loadLayout(minecraft);
        }
        return layout;
    }

    private static MarkerOverlayLayout loadLayout(Minecraft minecraft) {
        try {
            return minecraft.getResourceManager().getResource(THEME)
                    .map(resource -> {
                        try (BufferedReader reader = resource.openAsReader()) {
                            return MarkerOverlayLayout.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
                        } catch (IOException | RuntimeException exception) {
                            CBCStratagems.LOGGER.warn("Failed to parse stratagem UI theme {}", THEME, exception);
                            return MarkerOverlayLayout.DEFAULT;
                        }
                    })
                    .orElse(MarkerOverlayLayout.DEFAULT);
        } catch (RuntimeException exception) {
            CBCStratagems.LOGGER.warn("Failed to load stratagem UI theme {}", THEME, exception);
            return MarkerOverlayLayout.DEFAULT;
        }
    }

    private record MarkerOverlayLayout(
            double maxDistance,
            double maxAngleDegrees,
            float anchorX,
            float anchorY,
            int offsetX,
            int offsetY,
            int edgePadding,
            int width,
            int height,
            int padding,
            int gap,
            int iconSize,
            int iconX,
            int iconY,
            int iconSourceWidth,
            int iconSourceHeight,
            int iconTextureWidth,
            int iconTextureHeight,
            int nameX,
            int nameY,
            int countdownX,
            int countdownY,
            int backgroundColor,
            int nameColor,
            int countdownColor,
            double beamYoffset,
            ResourceLocation iconPlaceholder
    ) {
        private static final MarkerOverlayLayout DEFAULT = new MarkerOverlayLayout(
                128.0D,
                12.0D,
                0.5F,
                0.62F,
                0,
                -8,
                4,
                124,
                26,
                3,
                5,
                16,
                4,
                5,
                32,
                32,
                32,
                32,
                25,
                3,
                25,
                14,
                0x99000000,
                0xFFFFFFFF,
                0xFFFF5555,
                8.0D,
                FALLBACK_ICON
        );

        private static MarkerOverlayLayout fromJson(JsonObject root) {
            JsonObject overlay = object(root, "marker_overlay");
            int width = intValue(overlay, "width", DEFAULT.width);
            int height = intValue(overlay, "height", DEFAULT.height);
            int padding = intValue(overlay, "padding", DEFAULT.padding);
            int gap = intValue(overlay, "gap", DEFAULT.gap);
            int iconSize = intValue(overlay, "icon_size", DEFAULT.iconSize);
            int iconX = intValue(overlay, "icon_x", padding);
            int iconY = intValue(overlay, "icon_y", Math.max(0, (height - iconSize) / 2));
            int textX = iconX + iconSize + gap;
            int nameY = intValue(overlay, "name_y", padding);
            return new MarkerOverlayLayout(
                    doubleValue(overlay, "max_distance", DEFAULT.maxDistance),
                    doubleValue(overlay, "max_angle_degrees", DEFAULT.maxAngleDegrees),
                    floatValue(overlay, "anchor_x", DEFAULT.anchorX),
                    floatValue(overlay, "anchor_y", DEFAULT.anchorY),
                    intValue(overlay, "offset_x", DEFAULT.offsetX),
                    intValue(overlay, "offset_y", DEFAULT.offsetY),
                    intValue(overlay, "edge_padding", DEFAULT.edgePadding),
                    width,
                    height,
                    padding,
                    gap,
                    iconSize,
                    iconX,
                    iconY,
                    intValue(overlay, "icon_source_width", DEFAULT.iconSourceWidth),
                    intValue(overlay, "icon_source_height", DEFAULT.iconSourceHeight),
                    intValue(overlay, "icon_texture_width", DEFAULT.iconTextureWidth),
                    intValue(overlay, "icon_texture_height", DEFAULT.iconTextureHeight),
                    intValue(overlay, "name_x", textX),
                    nameY,
                    intValue(overlay, "countdown_x", textX),
                    intValue(overlay, "countdown_y", nameY + 12),
                    colorValue(overlay, "background_color", DEFAULT.backgroundColor),
                    colorValue(overlay, "name_color", DEFAULT.nameColor),
                    colorValue(overlay, "countdown_color", DEFAULT.countdownColor),
                    doubleValue(overlay, "beam_y_offset", DEFAULT.beamYoffset),
                    resourceValue(overlay, "icon_placeholder", DEFAULT.iconPlaceholder)
            );
        }

        private static JsonObject object(JsonObject root, String key) {
            JsonElement element = root.get(key);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }

        private static double doubleValue(JsonObject object, String key, double fallback) {
            JsonElement element = object.get(key);
            return element == null ? fallback : element.getAsDouble();
        }

        private static float floatValue(JsonObject object, String key, float fallback) {
            JsonElement element = object.get(key);
            return element == null ? fallback : element.getAsFloat();
        }

        private static int intValue(JsonObject object, String key, int fallback) {
            JsonElement element = object.get(key);
            return element == null ? fallback : element.getAsInt();
        }

        private static int colorValue(JsonObject object, String key, int fallback) {
            JsonElement element = object.get(key);
            if (element == null) {
                return fallback;
            }
            String value = element.getAsString();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            long parsed = Long.parseLong(value, 16);
            return value.length() <= 6 ? (int)(0xFF000000L | parsed) : (int) parsed;
        }

        private static ResourceLocation resourceValue(JsonObject object, String key, ResourceLocation fallback) {
            JsonElement element = object.get(key);
            return element == null ? fallback : ResourceLocation.parse(element.getAsString());
        }
    }

    private record ScreenPoint(float x, float y) {
    }
}
