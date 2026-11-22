package com.draxer.ascension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Reglas compartidas entre servidor y cliente: qué encantamientos se pueden subir,
 * cuánto cuesta y cuántos puntos vale un material.
 */
public final class UpgradeLogic {
    private UpgradeLogic() {}

    /**
     * Encantamientos del ítem que se pueden mejorar, en un orden estable
     * (el mismo en servidor y cliente, para que el índice del botón coincida).
     * Se excluyen maldiciones y encantamientos de un solo nivel (Reparación, Toque de seda...).
     */
    public static List<Holder<Enchantment>> upgradeable(ItemStack stack) {
        List<Holder<Enchantment>> result = new ArrayList<>();
        if (stack.isEmpty()) return result;
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(EnchantmentTags.CURSE)) continue;
            if (holder.value().getMaxLevel() <= 1) continue;
            result.add(holder);
        }
        result.sort(Comparator.comparing(Holder::getRegisteredName));
        return result;
    }

    public static int currentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    /** Costo en puntos para subir AL nivel indicado. */
    public static int costForLevel(int targetLevel) {
        double raw = AscensionConfig.costBase() * Math.pow(targetLevel, AscensionConfig.costExponent());
        return (int) Math.max(1, Math.ceil(raw));
    }

    public static int pointsPerItem(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer fixed = AscensionConfig.materialValues().get(id);
        if (fixed != null) return fixed; // la lista manda (también para poner un bloque en 0)
        return autoBlockValue(stack);
    }

    /**
     * Valor automático de cualquier bloque: su dureza redondeada (1..maxAutoBlockValue) multiplicada
     * por la rareza. 0 para lo que no es bloque, bloques irrompibles y bloques con contenido guardado.
     */
    public static int autoBlockValue(ItemStack stack) {
        if (!AscensionConfig.autoBlockValues()) return 0;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return 0;
        if (stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.BUNDLE_CONTENTS)) return 0; // no tragarse cofres llenos
        Block block = blockItem.getBlock();
        float hardness = block.defaultDestroyTime();
        if (hardness < 0.0F) return 0; // roca madre, bloques de comando, etc.
        int value = Math.max(1, Math.min(AscensionConfig.maxAutoBlockValue(), Math.round(hardness)));
        int rarity = switch (stack.getRarity()) {
            case UNCOMMON -> 2;
            case RARE -> 4;
            case EPIC -> 8;
            default -> 1;
        };
        return value * rarity;
    }

    public static boolean isUpgradeTarget(ItemStack stack) {
        return !stack.isEmpty() && EnchantmentHelper.hasAnyEnchantments(stack);
    }

    // ---------------------------------------------------------------------
    // Conversión a Giga (solo si el mod Giga Enchantments está instalado)
    // ---------------------------------------------------------------------

    /** Namespace del mod Giga Enchantments. No hay dependencia: se busca en el registro por nombre. */
    public static final String GIGA_NAMESPACE = "giga_enchants";

    /**
     * Versión Giga de un encantamiento vanilla (minecraft:sharpness -> giga_enchants:sharpness),
     * o vacío si no existe (encantamiento de otro mod, o Giga Enchantments no instalado).
     */
    public static Optional<Holder.Reference<Enchantment>> gigaCounterpart(RegistryAccess registries, Holder<Enchantment> enchantment) {
        Identifier id = Identifier.parse(enchantment.getRegisteredName());
        if (!"minecraft".equals(id.getNamespace())) return Optional.empty();
        ResourceKey<Enchantment> gigaKey = ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(GIGA_NAMESPACE, id.getPath()));
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(gigaKey);
    }

    /** Para convertir, el encantamiento debe estar al menos en su nivel máximo normal (Filo V, Protección IV...). */
    public static boolean meetsGigaLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return currentLevel(stack, enchantment) >= enchantment.value().getMaxLevel();
    }
}
