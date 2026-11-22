package com.draxer.draxsort;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Reabastecer la mano: cuando gastas el último bloque (o se rompe la herramienta) de la mano,
 * se trae automáticamente otra pila igual del resto de tu inventario al mismo slot.
 * Solo cuando la pila llegó a 0 desde 1 (gastada), no cuando la tiras entera ni cuando cambias de slot.
 */
@EventBusSubscriber(modid = DraxSort.MODID)
public final class AutoRefill {
    private record Held(int slot, ItemStack stack) {}

    private static final Map<UUID, Held> LAST = new HashMap<>();

    private AutoRefill() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        ItemStack current = inventory.getItem(selected);

        Held last = LAST.get(player.getUUID());
        if (last != null && last.slot() == selected && current.isEmpty() && last.stack().getCount() == 1
                && !player.containerMenu.getCarried().is(last.stack().getItem())) {
            refill(player, inventory, selected, last.stack());
            current = inventory.getItem(selected);
        }
        LAST.put(player.getUUID(), new Held(selected, current.isEmpty() ? ItemStack.EMPTY : current.copy()));
    }

    private static void refill(ServerPlayer player, Inventory inventory, int selected, ItemStack used) {
        int best = -1;
        for (int i = 0; i < 36; i++) {
            if (i == selected) continue;
            ItemStack candidate = inventory.getItem(i);
            if (candidate.isEmpty()) continue;
            // Bloques/ítems apilables: mismo ítem y mismos componentes. Herramientas: basta el mismo ítem.
            boolean same = used.isDamageableItem() ? candidate.is(used.getItem()) : ItemStack.isSameItemSameComponents(candidate, used);
            if (!same) continue;
            if (best < 0 || i >= 9 && best < 9) best = i; // preferir el inventario principal antes que otra ranura de la barra
            if (best >= 9) break;
        }
        if (best < 0) return;
        inventory.setItem(selected, inventory.getItem(best));
        inventory.setItem(best, ItemStack.EMPTY);
        inventory.setChanged();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.6F);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }
}
