package com.draxer.draxbackpacks;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Recolección automática: si una mochila del inventario la tiene activada (brilla), lo que recoges
 * y que ya hay en esa mochila entra directo en ella. Así la mochila de minería se llena sola de piedra y minerales.
 */
@EventBusSubscriber(modid = DraxBackpacks.MODID)
public final class AutoPickup {
    private AutoPickup() {}

    @SubscribeEvent
    static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;
        ItemEntity entity = event.getItemEntity();
        ItemStack incoming = entity.getItem();
        if (incoming.isEmpty() || entity.hasPickUpDelay() || !BackpackMenu.allowedInside(incoming)) return;

        Inventory inventory = player.getInventory();
        int before = incoming.getCount();
        for (int i = 0; i < inventory.getContainerSize() && !incoming.isEmpty(); i++) {
            ItemStack bag = inventory.getItem(i);
            if (!(bag.getItem() instanceof BackpackItem backpack) || !BackpackItem.autoPickup(bag)) continue;
            // Si esa mochila está abierta, no se toca (el menú abierto la sobrescribiría).
            if (player.containerMenu instanceof BackpackMenu open && open.backpack() == bag) continue;
            insert(bag, backpack.size(), incoming);
        }
        int moved = before - incoming.getCount();
        if (moved <= 0) return;

        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 1.4F + 2.0F);
        if (incoming.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        } else {
            entity.setItem(incoming);
        }
    }

    /** Mete {@code incoming} en la mochila: primero completa pilas iguales; si ya había de ese ítem, usa casillas vacías. */
    private static void insert(ItemStack bag, int size, ItemStack incoming) {
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        boolean present = false;
        for (ItemStack stack : items) {
            if (ItemStack.isSameItemSameComponents(stack, incoming)) {
                present = true;
                int move = Math.min(incoming.getCount(), stack.getMaxStackSize() - stack.getCount());
                if (move > 0) {
                    stack.grow(move);
                    incoming.shrink(move);
                }
            }
        }
        if (!present) return; // solo recoge lo que la mochila ya tiene (funciona como filtro)
        for (int i = 0; i < size && !incoming.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, incoming.split(Math.min(incoming.getCount(), incoming.getMaxStackSize())));
            }
        }
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
}
