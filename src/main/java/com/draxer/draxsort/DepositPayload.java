package com.draxer.draxsort;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cliente -> servidor: "deposita mi inventario en los cofres cercanos que ya tengan esos ítems".
 *
 * @param lockedMask bits de los slots bloqueados del inventario (no se depositan)
 */
public record DepositPayload(long lockedMask) implements CustomPacketPayload {
    public static final Type<DepositPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "deposit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DepositPayload::lockedMask,
            DepositPayload::new);

    @Override
    public Type<DepositPayload> type() {
        return TYPE;
    }
}
