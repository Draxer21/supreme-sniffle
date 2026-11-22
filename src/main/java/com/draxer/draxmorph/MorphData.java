package com.draxer.draxmorph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Lo que sabe el jugador: formas absorbidas (ids de tipos de entidad) y la forma actual ("" = humano).
 * Se guarda con el jugador, se conserva al morir y se sincroniza con todos los clientes que lo ven.
 */
public record MorphData(List<String> unlocked, String current) {
    public static final MorphData EMPTY = new MorphData(List.of(), "");

    public static final MapCodec<MorphData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(MorphData::unlocked),
            Codec.STRING.optionalFieldOf("current", "").forGetter(MorphData::current)
    ).apply(i, MorphData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), MorphData::unlocked,
            ByteBufCodecs.STRING_UTF8, MorphData::current,
            MorphData::new);

    public boolean isMorphed() {
        return !this.current.isEmpty();
    }

    public boolean has(String id) {
        return this.unlocked.contains(id);
    }

    public MorphData withUnlocked(String id) {
        if (has(id)) return this;
        List<String> list = new ArrayList<>(this.unlocked);
        list.add(id);
        return new MorphData(List.copyOf(list), this.current);
    }

    public MorphData withCurrent(String id) {
        return new MorphData(this.unlocked, id);
    }
}
