package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemTargetMode implements StringRepresentable {
    MARKER("marker");

    public static final Codec<StratagemTargetMode> CODEC = StringRepresentable.fromEnum(StratagemTargetMode::values);
    public static final StreamCodec<ByteBuf, StratagemTargetMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemTargetMode::bySerializedName,
            StratagemTargetMode::getSerializedName
    );

    private final String serializedName;

    StratagemTargetMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemTargetMode bySerializedName(String serializedName) {
        for (StratagemTargetMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return mode;
            }
        }
        return MARKER;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
