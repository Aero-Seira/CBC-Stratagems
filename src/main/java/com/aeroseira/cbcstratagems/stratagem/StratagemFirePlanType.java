package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemFirePlanType implements StringRepresentable {
    INSTANT("instant"),
    FIRE_GRID("fire_grid");

    public static final Codec<StratagemFirePlanType> CODEC = StringRepresentable.fromEnum(StratagemFirePlanType::values);
    public static final StreamCodec<ByteBuf, StratagemFirePlanType> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemFirePlanType::bySerializedName,
            StratagemFirePlanType::getSerializedName
    );

    private final String serializedName;

    StratagemFirePlanType(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemFirePlanType bySerializedName(String serializedName) {
        for (StratagemFirePlanType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return type;
            }
        }
        return INSTANT;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
