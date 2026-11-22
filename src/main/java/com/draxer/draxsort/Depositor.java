package com.draxer.draxsort;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * "Depositar en cofres cercanos" (servidor): cada ítem del inventario principal se guarda en los cofres,
 * barriles y cajas de shulker cercanos que YA contienen ese mismo ítem. La barra rápida, la armadura,
 * la mano secundaria y los slots bloqueados no se tocan.
 */
public final class Depositor {
    public static final int RADIUS = 8;

    private Depositor() {}

    public static void deposit(ServerPlayer player, long lockedMask) {
        if (!(player.level() instanceof ServerLevel level)) return;

        // 1) Contenedores cercanos accesibles.
        List<BlockEntity> nearby = nearbyContainers(player, level, RADIUS);

        // 2) Repartir.
        Inventory inventory = player.getInventory();
        Set<BlockEntity> used = new LinkedHashSet<>();
        int moved = 0;
        for (int i = 9; i < 36; i++) {
            if ((lockedMask & (1L << i)) != 0) continue;
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            for (BlockEntity be : nearby) {
                Container container = (Container) be;
                if (!contains(container, stack)) continue;
                int before = stack.getCount();
                insert(container, stack);
                if (stack.getCount() < before) {
                    moved += before - stack.getCount();
                    used.add(be);
                }
                if (stack.isEmpty()) break;
            }
            inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }

        // 3) Aviso y efectos.
        for (BlockEntity be : used) {
            be.setChanged();
            BlockPos p = be.getBlockPos();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5D, p.getY() + 1.1D, p.getZ() + 0.5D, 6, 0.25D, 0.1D, 0.25D, 0.0D);
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        if (moved > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
            player.sendOverlayMessage(Component.translatable("drax_sort.deposit.done", moved, used.size()));
        } else {
            player.sendOverlayMessage(Component.translatable("drax_sort.deposit.none", RADIUS));
        }
    }

    /** Cofres, barriles y cajas de shulker accesibles dentro del radio (sin cofres de estructura sin abrir ni con llave). */
    public static List<BlockEntity> nearbyContainers(ServerPlayer player, ServerLevel level, int radius) {
        List<BlockEntity> nearby = new ArrayList<>();
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity)) continue;
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) radius * radius) continue;
            if (be instanceof RandomizableContainer loot && loot.getLootTable() != null) continue; // cofres de estructura sin abrir
            if (be instanceof BaseContainerBlockEntity base && !base.canOpen(player)) continue;   // cofres con llave
            nearby.add(be);
        }
        return nearby;
    }

    private static boolean contains(Container container, ItemStack stack) {
        for (int j = 0; j < container.getContainerSize(); j++) {
            if (ItemStack.isSameItemSameComponents(container.getItem(j), stack)) return true;
        }
        return false;
    }

    /** Primero completa pilas existentes, luego usa slots vacíos. Descuenta de {@code stack} lo que entró. */
    private static void insert(Container container, ItemStack stack) {
        int limit = Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
        for (int j = 0; j < container.getContainerSize() && !stack.isEmpty(); j++) {
            ItemStack existing = container.getItem(j);
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < limit) {
                int move = Math.min(stack.getCount(), limit - existing.getCount());
                existing.grow(move);
                stack.shrink(move);
            }
        }
        for (int j = 0; j < container.getContainerSize() && !stack.isEmpty(); j++) {
            if (container.getItem(j).isEmpty() && container.canPlaceItem(j, stack)) {
                container.setItem(j, stack.split(Math.min(stack.getCount(), limit)));
            }
        }
    }
}
