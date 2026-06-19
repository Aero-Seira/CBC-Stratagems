package com.aeroseira.cbcstratagems.stratagem.input;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemInputStatus implements StringRepresentable {
    INACTIVE("inactive"),
    ACTIVE("active"),
    FAILED("failed"),
    COMPLETE("complete");

    public static final StreamCodec<ByteBuf, StratagemInputStatus> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemInputStatus::bySerializedName,
            StratagemInputStatus::getSerializedName
    );

    private final String serializedName;

    StratagemInputStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemInputStatus bySerializedName(String serializedName) {
        for (StratagemInputStatus status : values()) {
            if (status.serializedName.equals(serializedName)) {
                return status;
            }
        }
        return INACTIVE;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
