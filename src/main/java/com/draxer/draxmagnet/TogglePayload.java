package com.draxer.draxmagnet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Cliente -> servidor: "activa/desactiva mis imanes" (tecla M). */
public record TogglePayload() implements CustomPacketPayload {
    public static final TogglePayload INSTANCE = new TogglePayload();
    public static final Type<TogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxMagnet.MODID, "toggle"));
    public static final StreamCodec<ByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<TogglePayload> type() {
        return TYPE;
    }
}
