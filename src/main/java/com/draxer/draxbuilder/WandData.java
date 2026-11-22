package com.draxer.draxbuilder;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Datos guardados en la propia varita (se sincronizan solos con el cliente para la vista previa):
 * modo de la varita de construcción, primera esquina del modo relleno y bloque elegido de la varita de intercambio.
 */
public final class WandData {
    public static final int MODE_EXTEND = 0;
    public static final int MODE_FILL = 1;

    private WandData() {}

    private static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int mode(ItemStack stack) {
        return read(stack).getIntOr("drax_mode", MODE_EXTEND);
    }

    public static void setMode(ItemStack stack, int mode) {
        CompoundTag tag = read(stack);
        tag.putInt("drax_mode", mode);
        tag.remove("drax_corner");
        write(stack, tag);
    }

    public static Optional<BlockPos> corner(ItemStack stack) {
        CompoundTag tag = read(stack);
        if (!tag.contains("drax_corner")) return Optional.empty();
        return Optional.of(BlockPos.of(tag.getLongOr("drax_corner", 0L)));
    }

    public static void setCorner(ItemStack stack, BlockPos pos) {
        CompoundTag tag = read(stack);
        if (pos == null) tag.remove("drax_corner");
        else tag.putLong("drax_corner", pos.asLong());
        write(stack, tag);
    }

    public static Optional<Block> selectedBlock(ItemStack stack) {
        String id = read(stack).getStringOr("drax_block", "");
        if (id.isEmpty()) return Optional.empty();
        Identifier key = Identifier.tryParse(id);
        if (key == null) return Optional.empty();
        Block block = BuiltInRegistries.BLOCK.getValue(key);
        return block == Blocks.AIR ? Optional.empty() : Optional.of(block);
    }

    public static void setSelectedBlock(ItemStack stack, Block block) {
        CompoundTag tag = read(stack);
        tag.putString("drax_block", BuiltInRegistries.BLOCK.getKey(block).toString());
        write(stack, tag);
    }
}
