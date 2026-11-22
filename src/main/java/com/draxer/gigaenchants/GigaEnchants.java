package com.draxer.gigaenchants;

import net.neoforged.fml.common.Mod;

/**
 * Giga Enchantments: versiones el doble de fuertes de los encantamientos vanilla de combate y herramientas.
 * Casi todos son solo JSON (data/giga_enchants/enchantment). Giga Fortuna y Giga Botín dependen de las
 * tablas de botín vanilla, así que su efecto está en {@link GigaEvents}.
 */
@Mod(GigaEnchants.MODID)
public class GigaEnchants {
    public static final String MODID = "giga_enchants";

    public GigaEnchants() {
    }
}
