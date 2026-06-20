package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemTrajectoryMode implements StringRepresentable {
    AUTO("auto"),
    FIXED("fixed");

    public static final Codec<StratagemTrajectoryMode> CODEC = StringRepresentable.fromEnum(StratagemTrajectoryMode::values);
    public static final StreamCodec<ByteBuf, StratagemTrajectoryMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemTrajectoryMode::bySerializedName,
            StratagemTrajectoryMode::getSerializedName
    );

    private final String serializedName;

    StratagemTrajectoryMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemTrajectoryMode bySerializedName(String serializedName) {
        for (StratagemTrajectoryMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return mode;
            }
        }
        return AUTO;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
