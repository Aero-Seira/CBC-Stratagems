package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemTargetingType implements StringRepresentable {
    PRECISE("precise"),
    RANDOM_AREA("random_area"),
    VECTOR_PATH("vector_path"),
    AUTO_LOCK("auto_lock");

    public static final Codec<StratagemTargetingType> CODEC = StringRepresentable.fromEnum(StratagemTargetingType::values);
    public static final StreamCodec<ByteBuf, StratagemTargetingType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemTargetingType::bySerializedName,
            StratagemTargetingType::getSerializedName
    );

    private final String serializedName;

    StratagemTargetingType(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemTargetingType bySerializedName(String serializedName) {
        for (StratagemTargetingType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return type;
            }
        }
        return PRECISE;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
