package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemTargetDistribution implements StringRepresentable {
    POINT("point"),
    RANDOM_DISC("random_disc");

    public static final Codec<StratagemTargetDistribution> CODEC = StringRepresentable.fromEnum(StratagemTargetDistribution::values);
    public static final StreamCodec<ByteBuf, StratagemTargetDistribution> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemTargetDistribution::bySerializedName,
            StratagemTargetDistribution::getSerializedName
    );

    private final String serializedName;

    StratagemTargetDistribution(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemTargetDistribution bySerializedName(String serializedName) {
        for (StratagemTargetDistribution distribution : values()) {
            if (distribution.serializedName.equals(serializedName)) {
                return distribution;
            }
        }
        return POINT;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
