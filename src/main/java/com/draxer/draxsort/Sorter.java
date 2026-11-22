package com.draxer.draxsort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Lógica de orden (se ejecuta en el servidor). Compartida con el cliente solo para decidir
 * qué secciones se pueden ordenar y dónde dibujar los botones.
 */
public final class Sorter {
    /** Contenedores con menos slots que esto no se ordenan (hornos, tolvas, yunques...). */
    private static final int MIN_CONTAINER_SLOTS = 9;

    private Sorter() {}

    // ---------------------------------------------------------------------
    // Qué se ordena
    // ---------------------------------------------------------------------

    /** Slots principales del inventario del jugador (sin barra rápida, armadura ni mano secundaria). */
    public static boolean isPlayerMainSlot(Slot slot) {
        return slot.container instanceof Inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36;
    }

    /**
     * Slots de la misma sección que {@code origin}: el inventario principal del jugador, o todos los
     * slots del mismo contenedor (cofre, barril, caja de shulker...). Vacío si la sección no se puede ordenar.
     */
    public static List<Slot> sectionOf(AbstractContainerMenu menu, Slot origin) {
        List<Slot> result = new ArrayList<>();
        if (isPlayerMainSlot(origin)) {
            for (Slot slot : menu.slots) {
                if (slot.container == origin.container && isPlayerMainSlot(slot)) result.add(slot);
            }
            return result;
        }
        Container container = origin.container;
        if (container instanceof Inventory || container instanceof CraftingContainer) return result;
        for (Slot slot : menu.slots) {
            if (slot.container == container) result.add(slot);
        }
        if (result.size() < MIN_CONTAINER_SLOTS) result.clear();
        return result;
    }

    /** Primer slot de un contenedor ordenable que no sea el inventario del jugador (o null). */
    public static Slot firstContainerSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory) && !sectionOf(menu, slot).isEmpty()) return slot;
        }
        return null;
    }

    /** Primer slot del inventario principal del jugador en este menú (o null). */
    public static Slot firstPlayerSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (isPlayerMainSlot(slot)) return slot;
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Cómo se ordena
    // ---------------------------------------------------------------------

    /** ¿Está bloqueado este slot del inventario del jugador? (bit i del mask = slot i del inventario, 0..35). */
    public static boolean isLocked(Slot slot, long lockedMask) {
        if (!(slot.container instanceof Inventory)) return false;
        int i = slot.getContainerSlot();
        return i >= 0 && i < 36 && (lockedMask & (1L << i)) != 0;
    }

    public static void sort(Player player, List<Slot> section, SortMode mode, boolean columns, long lockedMask) {
        sort(player, section, mode, columns, lockedMask, List.of());
    }

    /** Convierte las entradas "slot=item" de la plantilla en un mapa slot del inventario -> id del ítem. */
    public static Map<Integer, String> parseTemplate(List<String> template) {
        Map<Integer, String> map = new HashMap<>();
        for (String entry : template) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            try {
                int slot = Integer.parseInt(entry.substring(0, eq).trim());
                if (slot >= 0 && slot < 36) map.put(slot, entry.substring(eq + 1).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    /**
     * Ordena la sección. Si hay plantilla (solo aplica al inventario del jugador), primero se pone en cada
     * slot de la plantilla una pila del ítem que le corresponde, y el resto se ordena en los slots que quedan.
     */
    public static void sort(Player player, List<Slot> section, SortMode mode, boolean columns, long lockedMask, List<String> template) {
        Map<Integer, String> plan = parseTemplate(template);
        // Solo slots de los que el jugador puede sacar ítems y que no estén bloqueados.
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : section) {
            if (isLocked(slot, lockedMask)) continue;
            if (!slot.hasItem() || slot.mayPickup(player)) slots.add(slot);
        }
        if (slots.isEmpty()) return;

        // 1) Juntar y vaciar.
        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) items.add(stack.copy());
            slot.set(ItemStack.EMPTY);
        }

        // 2) Combinar pilas incompletas del mismo ítem.
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : items) {
            for (ItemStack existing : merged) {
                if (stack.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                    int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(move);
                    stack.shrink(move);
                }
            }
            if (!stack.isEmpty()) merged.add(stack);
        }

        // 3) Ordenar.
        merged.sort(comparator(mode, merged));

        // 4) Colocar en orden de filas (izquierda->derecha, arriba->abajo) o de columnas.
        Comparator<Slot> position = columns
                ? Comparator.<Slot>comparingInt(s -> s.x).thenComparingInt(s -> s.y)
                : Comparator.<Slot>comparingInt(s -> s.y).thenComparingInt(s -> s.x);
        slots.sort(position);

        // 4b) Plantilla: cada slot recibe primero su ítem asignado (si hay alguno).
        List<Slot> free = new ArrayList<>();
        for (Slot slot : slots) {
            String wanted = slot.container instanceof Inventory ? plan.get(slot.getContainerSlot()) : null;
            if (wanted != null) {
                ItemStack match = null;
                for (ItemStack stack : merged) {
                    if (itemId(stack).equals(wanted) && slot.mayPlace(stack)) {
                        match = stack;
                        break;
                    }
                }
                if (match != null) {
                    merged.remove(match);
                    slot.set(match);
                    continue;
                }
            }
            free.add(slot);
        }

        int i = 0;
        for (Slot slot : free) {
            if (i >= merged.size()) break;
            slot.set(merged.get(i++));
        }
        // Por seguridad: si algo no cupo (no debería pasar), se devuelve al jugador.
        for (; i < merged.size(); i++) {
            ItemStack rest = merged.get(i);
            if (!player.getInventory().add(rest)) player.drop(rest, false);
        }
        for (Slot slot : slots) slot.setChanged();
    }

    private static Comparator<ItemStack> comparator(SortMode mode, List<ItemStack> all) {
        Comparator<ItemStack> byId = Comparator.comparing(Sorter::itemId);
        Comparator<ItemStack> byCountDesc = Comparator.comparingInt(ItemStack::getCount).reversed();
        return switch (mode) {
            case NAME -> Comparator.<ItemStack, String>comparing(s -> s.getHoverName().getString().toLowerCase())
                    .thenComparing(byId).thenComparing(byCountDesc);
            case COUNT -> {
                Map<String, Integer> totals = new HashMap<>();
                for (ItemStack s : all) totals.merge(itemId(s), s.getCount(), Integer::sum);
                yield Comparator.<ItemStack>comparingInt(s -> -totals.getOrDefault(itemId(s), 0))
                        .thenComparing(byId).thenComparing(byCountDesc);
            }
            case CATEGORY -> Comparator.comparingInt(Sorter::category).thenComparing(byId).thenComparing(byCountDesc);
        };
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** 0 bloques, 1 armas, 2 herramientas, 3 armadura, 4 comida, 5 resto. */
    private static int category(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem) return 0;
        if (stack.has(DataComponents.WEAPON)) return 1;
        if (stack.has(DataComponents.TOOL)) return 2;
        if (stack.has(DataComponents.EQUIPPABLE)) return 3;
        if (stack.has(DataComponents.FOOD)) return 4;
        return 5;
    }
}
