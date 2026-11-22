package com.draxer.gigaenchants;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** ¿El ítem (o libro) tiene algún encantamiento Giga? Cuenta también los Giga que añade Ascension Enchantments. */
public final class GigaCheck {
    private GigaCheck() {}

    public static boolean hasGiga(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Holder<Enchantment> holder : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            if (holder.unwrapKey().map(key -> key.identifier().getNamespace().equals(GigaEnchants.MODID)).orElse(false)) {
                return true;
            }
        }
        return false;
    }
}
