package com.aeroseira.cbcstratagems.stratagem.input;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemInputFeedback implements StringRepresentable {
    NONE("none"),
    ERROR("error"),
    COOLDOWN("cooldown");

    public static final StreamCodec<ByteBuf, StratagemInputFeedback> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemInputFeedback::bySerializedName,
            StratagemInputFeedback::getSerializedName
    );

    private final String serializedName;

    StratagemInputFeedback(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemInputFeedback bySerializedName(String serializedName) {
        for (StratagemInputFeedback feedback : values()) {
            if (feedback.serializedName.equals(serializedName)) {
                return feedback;
            }
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
