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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Varita de construcción: clic derecho sobre la cara de un bloque y se extiende esa cara,
 * colocando una capa de bloques iguales delante de todos los bloques iguales conectados
 * (hasta {@code maxBlocks}). Usa los bloques de tu inventario (en creativo, gratis).
 * Agachado: solo una fila, en la dirección horizontal hacia la que miras.
 * Agachado + clic al aire: cambia al modo Relleno (dos esquinas y se llena la caja).
 * Todo lo que hace se puede deshacer con Ctrl+Z.
 */
public class BuildingWandItem extends Item {
    private final int maxBlocks;

    public BuildingWandItem(Properties properties, int maxBlocks) {
        super(properties);
        this.maxBlocks = maxBlocks;
    }

    public int maxBlocks() {
        return this.maxBlocks;
    }

    /**
     * Posiciones donde se colocarían bloques si el jugador usara la varita ahora mismo sobre esa cara.
     * La usa el servidor al colocar y el cliente para la vista previa.
     */
    public List<BlockPos> plannedTargets(Level level, Player player, ItemStack wand, BlockPos clicked, Direction face) {
        if (WandData.mode(wand) == WandData.MODE_FILL) return plannedFill(level, player, wand, clicked, face);
        BlockState state = level.getBlockState(clicked);
        Item blockItem = state.getBlock().asItem();
        if (blockItem == Items.AIR || state.hasBlockEntity()) return List.of();
        int available = player.isCreative() ? Integer.MAX_VALUE : count(player.getInventory(), blockItem);
        if (available <= 0) return List.of();
        int limit = Math.min(this.maxBlocks, available);
        Direction line = player.isShiftKeyDown() ? lineDirection(player, face) : null;
        return collect(level, player, wand, clicked, face, state, limit, line);
    }

    /** Límite de bloques del modo relleno (más grande que el de extender). */
    public int fillLimit() {
        return this.maxBlocks * 4;
    }

    /** Agachado + clic derecho al aire: cambia entre modo Extender y modo Relleno. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack wand = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int mode = WandData.mode(wand) == WandData.MODE_FILL ? WandData.MODE_EXTEND : WandData.MODE_FILL;
            WandData.setMode(wand, mode);
            player.sendOverlayMessage(Component.translatable(mode == WandData.MODE_FILL
                    ? "drax_builder.wand.mode.fill" : "drax_builder.wand.mode.extend"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (WandData.mode(context.getItemInHand()) == WandData.MODE_FILL) return useFill(context, level, player);

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        Item blockItem = state.getBlock().asItem();
        if (blockItem == Items.AIR || state.hasBlockEntity()) return InteractionResult.FAIL; // ni aire ni cofres/hornos
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean creative = player.isCreative();
        int available = creative ? Integer.MAX_VALUE : count(player.getInventory(), blockItem);
        if (available <= 0) {
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.no_blocks", state.getBlock().getName()));
            return InteractionResult.FAIL;
        }

        int limit = Math.min(this.maxBlocks, available);
        Direction line = player.isShiftKeyDown() ? lineDirection(player, face) : null;
        List<BlockPos> targets = collect(level, player, context.getItemInHand(), clicked, face, state, limit, line);
        if (targets.isEmpty()) return InteractionResult.FAIL;

        int placed = placeAll(level, player, targets, state);
        if (placed == 0) return InteractionResult.FAIL;

        if (!creative) {
            take(player.getInventory(), blockItem, placed);
            context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        SoundType sound = state.getSoundType();
        level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }

    /** Coloca el bloque en todas las posiciones y lo anota en el historial para poder deshacer. */
    private static int placeAll(Level level, Player player, List<BlockPos> targets, BlockState state) {
        List<BuildHistory.Change> changes = new ArrayList<>();
        for (BlockPos target : targets) {
            BlockState before = level.getBlockState(target);
            BlockState toPlace = state;
            if (toPlace.hasProperty(BlockStateProperties.WATERLOGGED)) {
                toPlace = toPlace.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(target).is(Fluids.WATER));
            }
            if (level.setBlock(target, toPlace, 3)) changes.add(new BuildHistory.Change(target.immutable(), before, level.getBlockState(target)));
        }
        if (player instanceof ServerPlayer serverPlayer) BuildHistory.record(serverPlayer, level, changes);
        return changes.size();
    }

    // ---------------------------------------------------------------------
    // Modo relleno: dos esquinas y se llena la caja
    // ---------------------------------------------------------------------

    private InteractionResult useFill(UseOnContext context, Level level, Player player) {
        ItemStack wand = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Agachado: cancelar la esquina marcada.
        if (player.isShiftKeyDown()) {
            WandData.setCorner(wand, null);
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.fill.cleared"));
            return InteractionResult.SUCCESS;
        }

        Optional<BlockPos> corner = WandData.corner(wand);
        if (corner.isEmpty()) {
            BlockState state = level.getBlockState(clicked);
            if (state.getBlock().asItem() == Items.AIR || state.hasBlockEntity() || state.canBeReplaced()) return InteractionResult.FAIL;
            WandData.setSelectedBlock(wand, state.getBlock());
            WandData.setCorner(wand, target);
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.fill.first", state.getBlock().getName(),
                    target.getX(), target.getY(), target.getZ()));
            return InteractionResult.SUCCESS;
        }

        Optional<Block> chosen = WandData.selectedBlock(wand);
        if (chosen.isEmpty()) {
            WandData.setCorner(wand, null);
            return InteractionResult.FAIL;
        }
        BlockState state = chosen.get().defaultBlockState();
        Item blockItem = chosen.get().asItem();
        long volume = boxVolume(corner.get(), target);
        if (volume > this.fillLimit()) {
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.fill.too_big", volume, this.fillLimit()));
            return InteractionResult.FAIL;
        }

        boolean creative = player.isCreative();
        int available = creative ? Integer.MAX_VALUE : count(player.getInventory(), blockItem);
        if (available <= 0) {
            player.sendOverlayMessage(Component.translatable("drax_builder.wand.no_blocks", chosen.get().getName()));
            return InteractionResult.FAIL;
        }
        List<BlockPos> targets = fillTargets(level, player, wand, corner.get(), target, state, Math.min(available, this.fillLimit()));
        WandData.setCorner(wand, null);
        if (targets.isEmpty()) return InteractionResult.FAIL;

        int placed = placeAll(level, player, targets, state);
        if (placed == 0) return InteractionResult.FAIL;
        if (!creative) {
            take(player.getInventory(), blockItem, placed);
            wand.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        SoundType sound = state.getSoundType();
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        player.sendOverlayMessage(Component.translatable("drax_builder.wand.fill.done", placed));
        return InteractionResult.SUCCESS;
    }

    private List<BlockPos> plannedFill(Level level, Player player, ItemStack wand, BlockPos clicked, Direction face) {
        BlockPos target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        Optional<BlockPos> corner = WandData.corner(wand);
        if (corner.isEmpty()) return List.of(target);
        if (boxVolume(corner.get(), target) > this.fillLimit()) return List.of(corner.get(), target);
        BlockState state = WandData.selectedBlock(wand).map(Block::defaultBlockState).orElse(null);
        if (state == null) return List.of();
        return fillTargets(level, player, wand, corner.get(), target, state, this.fillLimit());
    }

    public static long boxVolume(BlockPos a, BlockPos b) {
        return (long) (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) * (Math.abs(a.getZ() - b.getZ()) + 1);
    }

    private static List<BlockPos> fillTargets(Level level, Player player, ItemStack wand, BlockPos a, BlockPos b, BlockState state, int limit) {
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
            if (result.size() >= limit) break;
            if (canPlace(level, player, wand, pos, Direction.UP, state)) result.add(pos.immutable());
        }
        return result;
    }

    /**
     * Búsqueda en anchura sobre el plano de la cara: bloques idénticos conectados al clicado,
     * cuya posición de enfrente esté libre.
     */
    public static List<BlockPos> collect(Level level, Player player, ItemStack wand, BlockPos start, Direction face,
                                          BlockState state, int limit, Direction line) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        List<Direction> spread = new ArrayList<>();
        if (line != null) {
            spread.add(line);
            spread.add(line.getOpposite());
        } else {
            for (Direction d : Direction.values()) {
                if (d.getAxis() != face.getAxis()) spread.add(d);
            }
        }

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos base = queue.poll();
            if (!level.getBlockState(base).equals(state)) continue;
            BlockPos target = base.relative(face);
            if (!canPlace(level, player, wand, target, face, state)) continue;
            result.add(target);
            for (Direction d : spread) {
                BlockPos next = base.relative(d);
                if (visited.add(next)) queue.add(next);
            }
        }
        return result;
    }

    public static boolean canPlace(Level level, Player player, ItemStack wand, BlockPos target, Direction face, BlockState state) {
        return level.getBlockState(target).canBeReplaced()
                && player.mayUseItemAt(target, face, wand)
                && level.isUnobstructed(state, target, CollisionContext.empty());
    }

    /** Dirección horizontal (perpendicular a la cara) hacia la que mira el jugador, para el modo fila. */
    public static Direction lineDirection(Player player, Direction face) {
        for (Direction d : Direction.orderedByNearest(player)) {
            if (d.getAxis() != face.getAxis() && d.getAxis().isHorizontal()) return d;
        }
        for (Direction d : Direction.orderedByNearest(player)) {
            if (d.getAxis() != face.getAxis()) return d;
        }
        return Direction.NORTH;
    }

    public static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** Saca una unidad del ítem del inventario; false si no había. */
    public static boolean takeOne(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    public static void take(Inventory inventory, Item item, int amount) {
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
        inventory.setChanged();
    }
}
