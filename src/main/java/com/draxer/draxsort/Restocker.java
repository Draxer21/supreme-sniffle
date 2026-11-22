package com.draxer.draxsort;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.netty.buffer.ByteBuf;

/**
 * "Rellenar desde cofres cercanos" (tecla R): completa las pilas de la barra rápida y de la mano
 * secundaria sacando ítems iguales de los cofres, barriles y cajas de shulker a 8 bloques.
 */
public final class Restocker {
    private Restocker() {}

    /** Cliente -> servidor (sin datos). */
    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "restock"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public static void restock(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        List<BlockEntity> nearby = Depositor.nearbyContainers(player, level, Depositor.RADIUS);
        Inventory inventory = player.getInventory();

        int moved = 0;
        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, Inventory.SLOT_OFFHAND};
        for (int slotIndex : slots) {
            ItemStack target = inventory.getItem(slotIndex);
            if (target.isEmpty() || !target.isStackable()) continue;
            int missing = target.getMaxStackSize() - target.getCount();
            for (BlockEntity be : nearby) {
                if (missing <= 0) break;
                Container container = (Container) be;
                boolean changed = false;
                for (int i = 0; i < container.getContainerSize() && missing > 0; i++) {
                    ItemStack source = container.getItem(i);
                    if (source.isEmpty() || !ItemStack.isSameItemSameComponents(source, target)) continue;
                    int take = Math.min(missing, source.getCount());
                    source.shrink(take);
                    target.grow(take);
                    missing -= take;
                    moved += take;
                    changed = true;
                }
                if (changed) container.setChanged();
            }
        }
        inventory.setChanged();

        if (moved > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.0D);
            player.sendOverlayMessage(Component.translatable("drax_sort.restock.done", moved));
        } else {
            player.sendOverlayMessage(Component.translatable("drax_sort.restock.none", Depositor.RADIUS));
        }
    }
}
