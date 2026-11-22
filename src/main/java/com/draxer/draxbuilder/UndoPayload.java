package com.draxer.draxbuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: "deshacer la última acción de varita". */
public record UndoPayload() implements CustomPacketPayload {
    public static final UndoPayload INSTANCE = new UndoPayload();
    public static final Type<UndoPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxBuilder.MODID, "undo"));
    public static final StreamCodec<ByteBuf, UndoPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<UndoPayload> type() {
        return TYPE;
    }
}
