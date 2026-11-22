package com.draxer.ascensionenchants.enchant;

import com.draxer.ascensionenchants.AscensionEnchants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Claves de los encantamientos del mod. Las definiciones están en data/ascension/enchantment/*.json;
 * los que necesitan lógica propia la tienen en {@link EnchantmentEvents}.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> LIFE_STEAL = key("life_steal");
    public static final ResourceKey<Enchantment> TELEKINESIS = key("telekinesis");
    public static final ResourceKey<Enchantment> VEIN_MINER = key("vein_miner");
    public static final ResourceKey<Enchantment> ETHEREAL_STEP = key("ethereal_step");
    public static final ResourceKey<Enchantment> PARRY = key("parry");
    public static final ResourceKey<Enchantment> FROST_THORNS = key("frost_thorns");
    public static final ResourceKey<Enchantment> SECOND_WIND = key("second_wind");
    public static final ResourceKey<Enchantment> REFLEXES = key("reflexes");
    public static final ResourceKey<Enchantment> HOMING = key("homing");
    public static final ResourceKey<Enchantment> ARROW_RAIN = key("arrow_rain");
    public static final ResourceKey<Enchantment> EXPLOSIVE_ARROWS = key("explosive_arrows");
    public static final ResourceKey<Enchantment> TIMBER = key("timber");
    public static final ResourceKey<Enchantment> SMELTING = key("smelting");
    public static final ResourceKey<Enchantment> EXCAVATION = key("excavation");
    // swift_strike, illager_slayer, ender_slayer y nether_slayer funcionan solo con JSON.

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AscensionEnchants.MODID, name));
    }

    /** Versión Giga (giga_enchants:mismo_nombre) de uno de nuestros encantamientos. */
    public static ResourceKey<Enchantment> giga(ResourceKey<Enchantment> key) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("giga_enchants", key.identifier().getPath()));
    }

    /**
     * Nivel más alto del encantamiento entre todo lo que lleva puesto la entidad (armadura, manos).
     * La versión Giga cuenta como el doble de niveles (Giga II = nivel IV).
     */
    public static int worn(LivingEntity entity, ResourceKey<Enchantment> key) {
        var lookup = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int normal = lookup.get(key).map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, entity)).orElse(0);
        int giga = lookup.get(giga(key)).map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, entity)).orElse(0);
        return Math.max(normal, giga * 2);
    }

    /** Nivel del encantamiento en el ítem (0 si no lo tiene o si el mundo no lo conoce). */
    public static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int normal = lookup.get(key).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
        int giga = lookup.get(giga(key)).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
        return Math.max(normal, giga * 2); // la versión Giga cuenta doble
    }
}
