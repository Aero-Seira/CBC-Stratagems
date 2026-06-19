package com.aeroseira.cbcstratagems.stratagem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum StratagemCommand implements StringRepresentable {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right");

    public static final Codec<StratagemCommand> CODEC = StringRepresentable.fromEnum(StratagemCommand::values);
    public static final StreamCodec<ByteBuf, StratagemCommand> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            StratagemCommand::bySerializedName,
            StratagemCommand::getSerializedName
    );

    private final String serializedName;

    StratagemCommand(String serializedName) {
        this.serializedName = serializedName;
    }

    public static StratagemCommand bySerializedName(String serializedName) {
        for (StratagemCommand command : values()) {
            if (command.serializedName.equals(serializedName)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown stratagem command direction: " + serializedName);
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
