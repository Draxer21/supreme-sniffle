package com.draxer.ascension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuración editable en config/ascension-common.toml.
 */
public final class AscensionConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_LEVEL = BUILDER
            .comment("Nivel máximo al que la Mesa de Ascensión puede subir un encantamiento.")
            .defineInRange("maxLevel", 15, 1, 255);

    public static final ModConfigSpec.BooleanValue AUTO_BLOCK_VALUES;
    public static final ModConfigSpec.IntValue MAX_AUTO_BLOCK_VALUE;
    public static final ModConfigSpec.DoubleValue COST_BASE;
    public static final ModConfigSpec.DoubleValue COST_EXPONENT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MATERIALS;

    static {
        // Sección nueva en 1.3.0: los valores cambiaron de escala (×10), así que se guardan en otras claves
        // para que una config vieja no mezcle precios antiguos con los nuevos.
        BUILDER.push("economy");
        AUTO_BLOCK_VALUES = BUILDER
                .comment("Si es true, CUALQUIER bloque (también de otros mods) vale puntos aunque no esté en la lista:",
                        "  valor = dureza del bloque redondeada (mínimo 1, máximo maxAutoBlockValue)",
                        "  × 2 si es poco común, × 4 si es raro, × 8 si es épico.",
                        "Tierra/arena = 1, piedra/troncos = 2, pizarra profunda = 3, obsidiana = 10 (está en la lista).",
                        "No cuentan los bloques con contenido guardado (cajas de shulker, etc.) ni los irrompibles (roca madre).")
                .define("autoBlockValues", true);
        MAX_AUTO_BLOCK_VALUE = BUILDER
                .comment("Valor máximo que puede tener un bloque calculado automáticamente (antes de la rareza).")
                .defineInRange("maxAutoBlockValue", 10, 1, 100000);
        COST_BASE = BUILDER
                .comment("Costo en puntos para subir al nivel N = ceil(costBase * N ^ costExponent).",
                        "Con los valores por defecto: nivel 6 = 842, nivel 10 = 2005, nivel 15 = 3995 puntos",
                        "(un diamante vale 100, así que XV cuesta unos 40 diamantes o ~31 stacks de piedra).")
                .defineInRange("costBase", 40.0D, 0.1D, 1000000.0D);
        COST_EXPONENT = BUILDER
                .defineInRange("costExponent", 1.7D, 0.0D, 10.0D);
        MATERIALS = BUILDER
                .comment("Valores fijos en puntos, formato \"namespace:item=puntos\". Tienen prioridad sobre el valor",
                        "automático de los bloques y sirven también para ítems que no son bloques (lingotes, gemas...).",
                        "Funciona con ítems de otros mods. Para que un bloque NO valga nada: \"namespace:bloque=0\".")
                .defineListAllowEmpty("materials", List.of(
                    "minecraft:lapis_lazuli=10",
                    "minecraft:quartz=10",
                    "minecraft:redstone=5",
                    "minecraft:coal=3",
                    "minecraft:copper_ingot=8",
                    "minecraft:raw_copper=6",
                    "minecraft:amethyst_shard=15",
                    "minecraft:iron_ingot=20",
                    "minecraft:raw_iron=15",
                    "minecraft:gold_ingot=30",
                    "minecraft:raw_gold=25",
                    "minecraft:experience_bottle=30",
                    "minecraft:emerald=60",
                    "minecraft:diamond=100",
                    "minecraft:echo_shard=150",
                    "minecraft:netherite_scrap=200",
                    "minecraft:netherite_ingot=600",
                    "minecraft:nether_star=1200",
                    "minecraft:ender_pearl=15",
                    "minecraft:blaze_rod=15",
                    "minecraft:breeze_rod=15",
                    "minecraft:ghast_tear=25",
                    "minecraft:shulker_shell=40",
                    "minecraft:dragon_breath=40",
                    "minecraft:golden_apple=35",
                    "minecraft:enchanted_golden_apple=500",
                    "minecraft:totem_of_undying=300",
                    "minecraft:heart_of_the_sea=400",
                    "minecraft:nautilus_shell=30",
                    "minecraft:ominous_trial_key=60",
                    "minecraft:trial_key=20",
                    "minecraft:coal_block=27",
                    "minecraft:copper_block=72",
                    "minecraft:raw_copper_block=54",
                    "minecraft:redstone_block=45",
                    "minecraft:lapis_block=90",
                    "minecraft:quartz_block=40",
                    "minecraft:amethyst_block=60",
                    "minecraft:iron_block=180",
                    "minecraft:raw_iron_block=135",
                    "minecraft:gold_block=270",
                    "minecraft:raw_gold_block=225",
                    "minecraft:emerald_block=540",
                    "minecraft:diamond_block=900",
                    "minecraft:netherite_block=5400",
                    "minecraft:coal_ore=5",
                    "minecraft:deepslate_coal_ore=5",
                    "minecraft:copper_ore=8",
                    "minecraft:deepslate_copper_ore=8",
                    "minecraft:iron_ore=15",
                    "minecraft:deepslate_iron_ore=15",
                    "minecraft:gold_ore=25",
                    "minecraft:deepslate_gold_ore=25",
                    "minecraft:nether_gold_ore=10",
                    "minecraft:redstone_ore=15",
                    "minecraft:deepslate_redstone_ore=15",
                    "minecraft:lapis_ore=30",
                    "minecraft:deepslate_lapis_ore=30",
                    "minecraft:emerald_ore=80",
                    "minecraft:deepslate_emerald_ore=80",
                    "minecraft:diamond_ore=120",
                    "minecraft:deepslate_diamond_ore=120",
                    "minecraft:nether_quartz_ore=12",
                    "minecraft:ancient_debris=220",
                    "minecraft:obsidian=10",
                    "minecraft:crying_obsidian=15",
                    "minecraft:sponge=20",
                    "minecraft:wet_sponge=20",
                    "minecraft:sculk_catalyst=50",
                    "minecraft:sculk_shrieker=40",
                    "minecraft:enchanting_table=250",
                    "minecraft:conduit=500",
                    "minecraft:heavy_core=300",
                    "minecraft:beacon=1500",
                    "minecraft:dragon_egg=3000",
                    "minecraft:dragon_head=500",
                    "minecraft:wither_skeleton_skull=150"
                ), () -> "minecraft:diamond=100", AscensionConfig::isValidEntry);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static List<? extends String> cachedSource;
    private static Map<String, Integer> cachedValues = Map.of();

    private AscensionConfig() {}

    private static boolean isValidEntry(Object obj) {
        if (!(obj instanceof String s)) return false;
        int eq = s.lastIndexOf('=');
        if (eq <= 0 || eq == s.length() - 1) return false;
        try {
            return Integer.parseInt(s.substring(eq + 1).trim()) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Mapa id de ítem -> puntos. Se reconstruye solo si la lista de la config cambió. */
    public static synchronized Map<String, Integer> materialValues() {
        List<? extends String> source;
        try {
            source = MATERIALS.get();
        } catch (IllegalStateException notLoadedYet) {
            return Map.of();
        }
        if (source != cachedSource) {
            Map<String, Integer> map = new HashMap<>();
            for (String entry : source) {
                if (!isValidEntry(entry)) continue;
                int eq = entry.lastIndexOf('=');
                map.put(entry.substring(0, eq).trim(), Integer.parseInt(entry.substring(eq + 1).trim()));
            }
            cachedValues = Map.copyOf(map);
            cachedSource = source;
        }
        return cachedValues;
    }

    public static int maxLevel() {
        try {
            return MAX_LEVEL.getAsInt();
        } catch (IllegalStateException e) {
            return 15;
        }
    }

    public static double costBase() {
        try {
            return COST_BASE.getAsDouble();
        } catch (IllegalStateException e) {
            return 40.0D;
        }
    }

    public static double costExponent() {
        try {
            return COST_EXPONENT.getAsDouble();
        } catch (IllegalStateException e) {
            return 1.7D;
        }
    }

    public static boolean autoBlockValues() {
        try {
            return AUTO_BLOCK_VALUES.getAsBoolean();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static int maxAutoBlockValue() {
        try {
            return MAX_AUTO_BLOCK_VALUE.getAsInt();
        } catch (IllegalStateException e) {
            return 10;
        }
    }
}
