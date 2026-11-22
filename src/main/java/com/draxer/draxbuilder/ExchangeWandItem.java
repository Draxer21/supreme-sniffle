package com.draxer.draxbuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Varita de intercambio: agachado + clic derecho sobre un bloque lo elige como "nuevo".
 * Clic derecho sobre otro bloque cambia ese bloque y los iguales conectados en el mismo plano
 * (hasta {@code maxBlocks}) por el elegido. Gasta los bloques nuevos del inventario y te devuelve los viejos.
 * Conserva la orientación cuando ambos bloques la tienen (troncos, escaleras...).
 */
public class ExchangeWandItem extends Item {
    private final int maxBlocks;

    public ExchangeWandItem(Properties properties, int maxBlocks) {
        super(properties);
        this.maxBlocks = maxBlocks;
    }

    public int maxBlocks() {
        return this.maxBlocks;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack wand = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        // Elegir bloque
        if (player.isShiftKeyDown()) {
            if (!isSwappable(level, clicked, clickedState)) return InteractionResult.FAIL;
            if (!level.isClientSide()) {
                WandData.setSelectedBlock(wand, clickedState.getBlock());
                player.sendOverlayMessage(Component.translatable("drax_builder.exchange.selected", clickedState.getBlock().getName()));
            }
            return InteractionResult.SUCCESS;
        }

        Optional<Block> chosen = WandData.selectedBlock(wand);
        if (chosen.isEmpty()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("drax_builder.exchange.none"));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Block newBlock = chosen.get();
        Item newItem = newBlock.asItem();
        boolean creative = player.isCreative();
        int available = creative ? Integer.MAX_VALUE : BuildingWandItem.count(player.getInventory(), newItem);
        if (available <= 0) {
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.no_blocks", newBlock.getName()));
            return InteractionResult.FAIL;
        }

        List<BlockPos> targets = plannedTargets(level, player, wand, clicked, context.getClickedFace());
        if (targets.isEmpty()) return InteractionResult.FAIL;
        targets = targets.subList(0, Math.min(targets.size(), available));

        List<BuildHistory.Change> changes = new ArrayList<>();
        for (BlockPos pos : targets) {
            BlockState before = level.getBlockState(pos);
            BlockState after = copyProperties(before, newBlock.defaultBlockState());
            if (!level.setBlock(pos, after, 3)) continue;
            changes.add(new BuildHistory.Change(pos.immutable(), before, level.getBlockState(pos)));
            if (!creative) {
                BuildingWandItem.takeOne(player.getInventory(), newItem);
                Item oldItem = before.getBlock().asItem();
                if (oldItem != Items.AIR && player instanceof ServerPlayer sp) BuildHistory.give(sp, new ItemStack(oldItem));
            }
        }
        if (changes.isEmpty()) return InteractionResult.FAIL;
        if (player instanceof ServerPlayer sp) BuildHistory.record(sp, level, changes);
        if (!creative) wand.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());

        SoundType sound = newBlock.defaultBlockState().getSoundType();
        level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }

    /** Bloques que se cambiarían: iguales al clicado, conectados, en el plano de la cara. También lo usa la vista previa. */
    public List<BlockPos> plannedTargets(Level level, Player player, ItemStack wand, BlockPos clicked, Direction face) {
        Optional<Block> chosen = WandData.selectedBlock(wand);
        BlockState start = level.getBlockState(clicked);
        if (chosen.isEmpty() || start.is(chosen.get()) || !isSwappable(level, clicked, start)) return List.of();

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(clicked);
        visited.add(clicked);
        while (!queue.isEmpty() && result.size() < this.maxBlocks) {
            BlockPos pos = queue.poll();
            if (!level.getBlockState(pos).is(start.getBlock())) continue;
            if (!player.mayUseItemAt(pos, face, wand)) continue;
            result.add(pos);
            for (Direction d : Direction.values()) {
                if (d.getAxis() == face.getAxis()) continue;
                BlockPos next = pos.relative(d);
                if (visited.add(next)) queue.add(next);
            }
        }
        return result;
    }

    private static boolean isSwappable(Level level, BlockPos pos, BlockState state) {
        return state.getBlock().asItem() != Items.AIR
                && !state.hasBlockEntity()
                && !state.canBeReplaced()
                && state.getDestroySpeed(level, pos) >= 0.0F; // nada de roca madre
    }

    /** Copia las propiedades que ambos bloques tienen en común (orientación, eje, mitad de losa...). */
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) to = copy(from, to, property);
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}
