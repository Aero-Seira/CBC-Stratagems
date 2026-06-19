package com.aeroseira.cbcstratagems.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemDeviceMode implements StringRepresentable {
    CALLER("caller"),
    BEACON("beacon");

    public static final Codec<StratagemDeviceMode> CODEC = StringRepresentable.fromEnum(StratagemDeviceMode::values);
    public static final StreamCodec<ByteBuf, StratagemDeviceMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemDeviceMode::bySerializedName,
            StratagemDeviceMode::getSerializedName
    );

    private final String serializedName;

    StratagemDeviceMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemDeviceMode bySerializedName(String serializedName) {
        for (StratagemDeviceMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return mode;
            }
        }
        return CALLER;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
