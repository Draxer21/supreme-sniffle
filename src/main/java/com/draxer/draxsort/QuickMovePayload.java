package com.draxer.draxsort;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Cliente -> servidor: "haz Shift + clic en este slot" (lo usan la rueda del mouse y Shift + arrastrar).
 * El servidor hace el mismo clic que haría vanilla y luego sincroniza el inventario.
 */
public record QuickMovePayload(int containerId, int slotIndex) implements CustomPacketPayload {
    public static final Type<QuickMovePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "quick_move"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuickMovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QuickMovePayload::containerId,
            ByteBufCodecs.VAR_INT, QuickMovePayload::slotIndex,
            QuickMovePayload::new);

    @Override
    public Type<QuickMovePayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, QuickMovePayload payload) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != payload.containerId()) return;
        if (payload.slotIndex() < 0 || payload.slotIndex() >= menu.slots.size()) return;
        if (!menu.getCarried().isEmpty()) return;
        menu.clicked(payload.slotIndex(), 0, ContainerInput.QUICK_MOVE, player);
        menu.broadcastChanges();
    }
}
