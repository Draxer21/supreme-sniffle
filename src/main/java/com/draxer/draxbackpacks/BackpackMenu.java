package com.draxer.draxbackpacks;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Menú de la mochila: un cofre vanilla (el cliente usa la pantalla normal de cofre) cuyo contenido
 * se guarda en la propia mochila cada vez que cambia.
 * La mochila abierta no se puede mover mientras está abierta, y dentro no se pueden meter
 * otras mochilas ni cajas de shulker (vuelven a tu inventario).
 */
public class BackpackMenu extends ChestMenu {
    private final ItemStack backpack;
    private final InteractionHand hand;
    private final Player player;
    private final int lockedSlot;

    public BackpackMenu(int containerId, Inventory inventory, ItemStack backpack, InteractionHand hand, int rows) {
        this(containerId, inventory, backpack, hand, rows, new Storage(rows * 9, backpack, inventory.player));
    }

    private BackpackMenu(int containerId, Inventory inventory, ItemStack backpack, InteractionHand hand, int rows, Storage storage) {
        super(typeFor(rows), containerId, inventory, storage, rows);
        this.backpack = backpack;
        this.hand = hand;
        this.player = inventory.player;
        // En el menú de cofre, la barra rápida va después del contenedor y de las 27 casillas del inventario.
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? rows * 9 + 27 + inventory.getSelectedSlot() : -1;
    }

    private static MenuType<ChestMenu> typeFor(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    public ItemStack backpack() {
        return this.backpack;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex == this.lockedSlot) return;                                        // no mover la mochila abierta
        if (input == ContainerInput.SWAP) {
            if (this.hand == InteractionHand.MAIN_HAND && button == player.getInventory().getSelectedSlot()) return;
            if (this.hand == InteractionHand.OFF_HAND && button == 40) return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return !this.backpack.isEmpty() && player.getItemInHand(this.hand) == this.backpack;
    }

    /** ¿Se puede guardar esto en una mochila? (no mochilas ni cajas de shulker, para no anidar inventarios). */
    public static boolean allowedInside(ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem) return false;
        return !(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock);
    }

    /** Contenedor que lee y escribe el contenido de la mochila. */
    private static final class Storage extends SimpleContainer {
        private final ItemStack backpack;
        private final Player player;
        private boolean loading = true;

        Storage(int size, ItemStack backpack, Player player) {
            super(size);
            this.backpack = backpack;
            this.player = player;
            NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
            for (int i = 0; i < size; i++) this.setItem(i, items.get(i));
            this.loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (this.loading || this.player.level().isClientSide()) return;
            // Saca lo que no puede ir dentro.
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack stack = this.getItem(i);
                if (!stack.isEmpty() && !allowedInside(stack)) {
                    super.setItem(i, ItemStack.EMPTY);
                    if (!this.player.getInventory().add(stack)) this.player.drop(stack, false);
                }
            }
            NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < this.getContainerSize(); i++) items.set(i, this.getItem(i).copy());
            this.backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }
}
