package com.draxer.draxbuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Historial de las últimas acciones de las varitas de cada jugador, para deshacer (Ctrl+Z).
 * Solo vive en memoria del servidor: al reiniciar el servidor se pierde.
 */
public final class BuildHistory {
    public static final int MAX_ACTIONS = 10;

    public record Change(BlockPos pos, BlockState before, BlockState after) {}

    public record Action(ResourceKey<Level> dimension, List<Change> changes) {}

    private static final Map<UUID, Deque<Action>> HISTORY = new HashMap<>();

    private BuildHistory() {}

    public static void record(ServerPlayer player, Level level, List<Change> changes) {
        if (changes.isEmpty()) return;
        Deque<Action> deque = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        deque.push(new Action(level.dimension(), List.copyOf(changes)));
        while (deque.size() > MAX_ACTIONS) deque.removeLast();
    }

    /**
     * Deshace la última acción: vuelve cada bloque a como estaba (solo si nadie lo cambió después),
     * devuelve los bloques colocados al inventario y, si había un bloque antes, lo saca del inventario.
     */
    public static void undo(ServerPlayer player) {
        Deque<Action> deque = HISTORY.get(player.getUUID());
        Action action = deque == null ? null : deque.poll();
        if (action == null) {
            player.sendOverlayMessage(Component.translatable("drax_builder.undo.empty"));
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(action.dimension())) {
            player.sendOverlayMessage(Component.translatable("drax_builder.undo.other_dimension"));
            deque.push(action);
            return;
        }

        boolean creative = player.isCreative();
        int restored = 0;
        List<Change> changes = new ArrayList<>(action.changes());
        for (int i = changes.size() - 1; i >= 0; i--) {
            Change change = changes.get(i);
            if (!level.getBlockState(change.pos()).equals(change.after())) continue; // alguien lo cambió
            if (!player.mayUseItemAt(change.pos(), net.minecraft.core.Direction.UP, ItemStack.EMPTY)) continue;

            Item beforeItem = change.before().getBlock().asItem();
            boolean needsItem = !creative && beforeItem != Items.AIR && !change.before().canBeReplaced();
            if (needsItem && !BuildingWandItem.takeOne(player.getInventory(), beforeItem)) continue;

            level.setBlock(change.pos(), change.before(), 3);
            Item afterItem = change.after().getBlock().asItem();
            if (!creative && afterItem != Items.AIR) give(player, new ItemStack(afterItem));
            restored++;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
        player.sendOverlayMessage(Component.translatable("drax_builder.undo.done", restored));
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }
}
