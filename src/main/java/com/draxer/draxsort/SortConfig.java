package com.draxer.draxsort;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Preferencias del jugador. Los botones ◆/A/# y ≡/‖ las cambian y se guardan solas. */
public final class SortConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<SortMode> MODE = BUILDER
            .comment("Criterio de orden: CATEGORY (categoría), NAME (nombre) o COUNT (cantidad).")
            .defineEnum("mode", SortMode.CATEGORY);

    public static final ModConfigSpec.BooleanValue COLUMNS = BUILDER
            .comment("true = ordenar por columnas (de arriba a abajo); false = por filas (de izquierda a derecha).")
            .define("columns", false);

    public static final ModConfigSpec.BooleanValue MIDDLE_CLICK = BUILDER
            .comment("Clic central (rueda) sobre un cofre o el inventario para ordenarlo. No funciona en creativo (ahí el clic central copia ítems).")
            .define("middleClick", true);

    public static final ModConfigSpec.BooleanValue SCROLL_MOVE = BUILDER
            .comment("Rueda del mouse sobre un slot: abajo manda la pila al otro lado, arriba trae una pila igual.")
            .define("scrollMove", true);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> LOCKED_SLOTS = BUILDER
            .comment("Slots bloqueados del inventario (0-8 barra rápida, 9-35 inventario). Se cambian con Alt + clic.")
            .defineListAllowEmpty("lockedSlots", List.of(), () -> 0, o -> o instanceof Integer i && i >= 0 && i < 36);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEMPLATE = BUILDER
            .comment("Plantilla del inventario: \"slot=namespace:item\" (slot 9-35). Al ordenar el inventario, cada ítem",
                    "vuelve a su slot de la plantilla. Se guarda con el botón 📌 (Shift + clic lo borra).")
            .defineListAllowEmpty("template", List.of(), () -> "9=minecraft:torch", o -> o instanceof String);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static List<String> templateCache = null;

    public static List<String> template() {
        if (templateCache != null) return templateCache;
        try {
            templateCache = List.copyOf(TEMPLATE.get());
        } catch (IllegalStateException e) {
            return List.of();
        }
        return templateCache;
    }

    public static void setTemplate(List<String> entries) {
        templateCache = List.copyOf(entries);
        try {
            TEMPLATE.set(templateCache);
            TEMPLATE.save();
        } catch (RuntimeException ignored) {
        }
    }

    /** Copia en memoria (por si la config aún no cargó o no se pudo guardar). */
    private static long lockedCache = -1L;

    private SortConfig() {}

    public static SortMode mode() {
        try {
            return MODE.get();
        } catch (IllegalStateException e) {
            return SortMode.CATEGORY;
        }
    }

    public static boolean columns() {
        try {
            return COLUMNS.getAsBoolean();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static boolean middleClick() {
        try {
            return MIDDLE_CLICK.getAsBoolean();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static boolean scrollMove() {
        try {
            return SCROLL_MOVE.getAsBoolean();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public static void setMode(SortMode mode) {
        try {
            MODE.set(mode);
            MODE.save();
        } catch (RuntimeException ignored) { // config aún no cargada: el cambio dura solo esta sesión
        }
    }

    public static void setColumns(boolean columns) {
        try {
            COLUMNS.set(columns);
            COLUMNS.save();
        } catch (RuntimeException ignored) {
        }
    }

    /** Bits de los slots bloqueados: bit i = slot i del inventario. */
    public static long lockedMask() {
        if (lockedCache >= 0) return lockedCache;
        long mask = 0L;
        try {
            for (Integer i : LOCKED_SLOTS.get()) mask |= 1L << i;
        } catch (IllegalStateException e) {
            return 0L;
        }
        lockedCache = mask;
        return mask;
    }

    public static boolean isLocked(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot < 36 && (lockedMask() & (1L << inventorySlot)) != 0;
    }

    public static void toggleLocked(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= 36) return;
        long mask = lockedMask() ^ (1L << inventorySlot);
        lockedCache = mask;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 36; i++) if ((mask & (1L << i)) != 0) list.add(i);
        try {
            LOCKED_SLOTS.set(list);
            LOCKED_SLOTS.save();
        } catch (RuntimeException ignored) {
        }
    }
}
