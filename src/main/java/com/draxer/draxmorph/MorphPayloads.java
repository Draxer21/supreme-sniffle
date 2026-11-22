package com.draxer.draxmorph;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MorphPayloads {
    private MorphPayloads() {}

    /** Cliente -> servidor: transformarse en la forma {@code id} ("" = volver a humano). */
    public record Select(String id) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxMorph.MODID, "select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Select::id, Select::new);

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: usar la habilidad de la forma actual. */
    public record Ability() implements CustomPacketPayload {
        public static final Ability INSTANCE = new Ability();
        public static final Type<Ability> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxMorph.MODID, "ability"));
        public static final StreamCodec<ByteBuf, Ability> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Ability> type() {
            return TYPE;
        }
    }
}
