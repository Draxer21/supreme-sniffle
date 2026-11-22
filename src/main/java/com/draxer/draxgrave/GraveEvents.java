package com.draxer.draxgrave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jspecify.annotations.Nullable;

/**
 * Al morir un jugador, en vez de soltar sus ítems se crea una tumba con todo adentro.
 * También guarda toda la experiencia, y al reaparecer da una brújula que apunta a la tumba
 * (y sirve de llave para que un amigo la abra).
 * Respeta keepInventory y la Maldición de desaparición (vanilla ya los quita antes de este evento).
 * Si no hay espacio para la tumba, los ítems caen como siempre.
 */
@EventBusSubscriber(modid = DraxGrave.MODID)
public final class GraveEvents {
    private GraveEvents() {}

    /** XP guardada al morir, a la espera de que se cree la tumba (el evento de XP llega antes que el de ítems). */
    private static final Map<UUID, Integer> PENDING_XP = new HashMap<>();
    /** Última tumba de cada jugador, para darle la brújula al reaparecer. */
    private static final Map<UUID, GlobalPos> LAST_GRAVE = new HashMap<>();

    /**
     * Si el jugador iba a soltar experiencia (es decir, no hay keepInventory), en vez de orbes se guarda
     * TODA su experiencia para meterla en la tumba (no solo los 100 puntos que suelta vanilla).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onPlayerXp(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.isCanceled() || event.getDroppedExperience() <= 0) return;
        PENDING_XP.put(player.getUUID(), totalPoints(player));
        event.setDroppedExperience(0);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int xp = PENDING_XP.getOrDefault(player.getUUID(), 0);
        PENDING_XP.remove(player.getUUID());
        if (!(player.level() instanceof ServerLevel level)) return;
        if (event.getDrops().isEmpty() && xp <= 0) return;

        BlockPos spot = findSpot(level, player.blockPosition());
        if (spot == null) {
            player.sendSystemMessage(Component.translatable("drax_grave.no_space").withColor(0xFF5555));
            if (xp > 0) ExperienceOrb.award(level, player.position(), xp);
            return;
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity entity : event.getDrops()) {
            stacks.add(entity.getItem().copy());
        }

        BlockState state = DraxGrave.GRAVE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, player.getDirection().getOpposite());
        level.setBlock(spot, state, 3);
        if (!(level.getBlockEntity(spot) instanceof GraveBlockEntity grave)) { // algo falló: se sueltan normal
            if (xp > 0) ExperienceOrb.award(level, player.position(), xp);
            return;
        }
        grave.fill(player, stacks);
        grave.setExperience(xp);
        event.getDrops().clear();
        LAST_GRAVE.put(player.getUUID(), GlobalPos.of(level.dimension(), spot));

        player.sendSystemMessage(Component.translatable("drax_grave.created",
                spot.getX(), spot.getY(), spot.getZ(), stacks.size()).withColor(0xFFAA00));
    }

    /** Al reaparecer, el jugador recibe la brújula que apunta a su tumba. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        GlobalPos grave = LAST_GRAVE.remove(player.getUUID());
        if (grave == null) return;
        ItemStack compass = GraveCompass.create(grave.dimension(), grave.pos(), player.getName().getString());
        if (!player.getInventory().add(compass)) player.drop(compass, false);
        player.sendSystemMessage(Component.translatable("drax_grave.compass.given").withColor(0xFFAA00));
    }

    /** Puntos de experiencia totales del jugador, calculados desde su nivel y su barra (más exacto que totalExperience). */
    private static int totalPoints(ServerPlayer player) {
        int level = player.experienceLevel;
        long points;
        if (level <= 16) points = (long) level * level + 6L * level;
        else if (level <= 31) points = (long) (2.5D * level * level - 40.5D * level + 360.0D);
        else points = (long) (4.5D * level * level - 162.5D * level + 2220.0D);
        points += Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, points));
    }

    /**
     * Busca dónde poner la tumba: el lugar de la muerte o cerca (un poco más arriba y alrededor).
     * Solo reemplaza aire, agua, lava, pasto y similares. Si moriste en el vacío, la pone en el borde inferior.
     */
    private static @Nullable BlockPos findSpot(ServerLevel level, BlockPos death) {
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 1;
        BlockPos start = new BlockPos(death.getX(), Math.max(minY, Math.min(maxY, death.getY())), death.getZ());

        for (int dy = 0; dy <= 8; dy++) {
            for (int r = 0; r <= 3; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // solo el anillo de radio r
                        BlockPos pos = start.offset(dx, dy, dz);
                        if (pos.getY() > maxY) continue;
                        BlockState here = level.getBlockState(pos);
                        if (here.canBeReplaced() && level.getBlockEntity(pos) == null) return pos;
                    }
                }
            }
        }
        return null;
    }
}
