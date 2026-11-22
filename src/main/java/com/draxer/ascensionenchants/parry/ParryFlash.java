package com.draxer.ascensionenchants.parry;

import com.draxer.ascensionenchants.AscensionEnchants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Servidor -> cliente: "hiciste un parry perfecto", para el destello en pantalla.
 * {@code strength} va de 0 a 1 (los proyectiles desviados dan un destello más suave).
 */
public record ParryFlash(float strength) implements CustomPacketPayload {
    public static final Type<ParryFlash> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AscensionEnchants.MODID, "parry_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParryFlash> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ParryFlash::strength, ParryFlash::new);

    /** Duración del destello en milisegundos. */
    public static final long DURATION_MS = 260L;

    // Estado del destello en el cliente (sin clases de cliente, así el servidor puede cargar esta clase).
    private static volatile long startMillis = 0L;
    private static volatile float lastStrength = 0.0F;

    @Override
    public Type<ParryFlash> type() {
        return TYPE;
    }

    public static void trigger(float strength) {
        lastStrength = Math.max(0.0F, Math.min(1.0F, strength));
        startMillis = System.currentTimeMillis();
    }

    /** Opacidad actual del destello (0 = nada), se desvanece a lo largo de {@link #DURATION_MS}. */
    public static float currentAlpha() {
        long elapsed = System.currentTimeMillis() - startMillis;
        if (elapsed < 0 || elapsed >= DURATION_MS) return 0.0F;
        float t = 1.0F - (float) elapsed / DURATION_MS;
        return lastStrength * t * t;
    }
}
