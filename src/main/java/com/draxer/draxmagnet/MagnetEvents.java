package com.draxer.draxmagnet;

import java.util.List;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Cada tick, si el jugador tiene un imán activo en el inventario, atrae los ítems y orbes cercanos.
 * Agachado: el imán se pausa (para poder tirar cosas sin que vuelvan).
 * No atrae lo que el propio jugador acaba de tirar (2 segundos).
 */
@EventBusSubscriber(modid = DraxMagnet.MODID)
public final class MagnetEvents {
    private MagnetEvents() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator() || player.isShiftKeyDown()) return;

        double radius = activeRadius(player.getInventory());
        if (radius <= 0) return;

        Vec3 target = player.position().add(0.0D, 0.75D, 0.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.getItem().isEmpty());
        for (ItemEntity item : items) {
            // Lo que tiraste tú hace poco no vuelve (si no, no podrías soltar nada).
            if (item.getOwner() == player && item.getAge() < 40) continue;
            if (item.hasPickUpDelay() && item.getAge() < 10) continue;
            pull(item, target);
        }
        List<ExperienceOrb> orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive);
        for (ExperienceOrb orb : orbs) pull(orb, target);
    }

    private static void pull(net.minecraft.world.entity.Entity entity, Vec3 target) {
        Vec3 to = target.subtract(entity.position());
        double dist = to.length();
        if (dist < 0.5D) return;
        Vec3 velocity = to.scale(Math.min(0.45D, 0.12D + 0.6D / dist) / dist);
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true; // que el cliente reciba el movimiento
    }

    private static double activeRadius(Inventory inventory) {
        double best = 0.0D;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof MagnetItem magnet && MagnetItem.isActive(stack)) best = Math.max(best, magnet.radius());
        }
        return best;
    }
}
