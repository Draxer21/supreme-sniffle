package com.draxer.ascension.block;

import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Mineral de los bordes de la mesa. Se cambia con Shift + clic derecho usando el material. */
public enum TableTrim implements StringRepresentable {
    DIAMOND("diamond", () -> Items.DIAMOND),
    IRON("iron", () -> Items.IRON_INGOT),
    GOLD("gold", () -> Items.GOLD_INGOT),
    COPPER("copper", () -> Items.COPPER_INGOT),
    AMETHYST("amethyst", () -> Items.AMETHYST_SHARD);

    private final String name;
    private final Supplier<Item> item;

    TableTrim(String name, Supplier<Item> item) {
        this.name = name;
        this.item = item;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static @Nullable TableTrim fromItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (TableTrim trim : values()) {
            if (stack.is(trim.item.get())) return trim;
        }
        return null;
    }
}
