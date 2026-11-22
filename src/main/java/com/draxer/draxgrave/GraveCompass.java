package com.draxer.draxgrave;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

/**
 * Brújula de tumba: apunta a la tumba y además sirve de llave. Cualquiera que la tenga en el
 * inventario puede abrir esa tumba, así el dueño se la puede pasar a un amigo.
 */
public final class GraveCompass {
    private GraveCompass() {}

    public static ItemStack create(ResourceKey<Level> dimension, BlockPos pos, String ownerName) {
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(dimension, pos)), false));
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("drax_grave.compass.name", ownerName)
                .withColor(0xFFAA00).withStyle(s -> s.withItalic(false)));
        return compass;
    }

    /** ¿Esta pila es una brújula que apunta a esa tumba? */
    public static boolean pointsTo(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        if (!stack.is(Items.COMPASS)) return false;
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) return false;
        GlobalPos target = tracker.target().get();
        return target.dimension().equals(dimension) && target.pos().equals(pos);
    }

    public static boolean has(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (pointsTo(inventory.getItem(i), dimension, pos)) return true;
        }
        return false;
    }

    /** Quita del inventario las brújulas de esa tumba (ya no sirven). */
    public static void removeFrom(Player player, ResourceKey<Level> dimension, BlockPos pos) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (pointsTo(inventory.getItem(i), dimension, pos)) inventory.setItem(i, ItemStack.EMPTY);
        }
    }
}
