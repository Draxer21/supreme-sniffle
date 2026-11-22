package com.draxer.draxsort;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cliente -> servidor: "ordena la sección del menú a la que pertenece este slot".
 *
 * @param containerId id del menú abierto (para ignorar paquetes de un menú ya cerrado)
 * @param slotIndex   índice de cualquier slot de la sección a ordenar
 * @param mode        índice de {@link SortMode}
 * @param columns     true = llenar por columnas (de arriba a abajo); false = por filas
 * @param lockedMask  bits de los slots bloqueados del inventario del jugador (bit i = slot i, 0..35)
 * @param template    plantilla del inventario: entradas "slot=namespace:item" (slot 9..35); vacía si no hay
 */
public record SortPayload(int containerId, int slotIndex, int mode, boolean columns, long lockedMask, List<String> template) implements CustomPacketPayload {
    public static final Type<SortPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SortPayload::containerId,
            ByteBufCodecs.VAR_INT, SortPayload::slotIndex,
            ByteBufCodecs.VAR_INT, SortPayload::mode,
            ByteBufCodecs.BOOL, SortPayload::columns,
            ByteBufCodecs.VAR_LONG, SortPayload::lockedMask,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), SortPayload::template,
            SortPayload::new);

    @Override
    public Type<SortPayload> type() {
        return TYPE;
    }
}
