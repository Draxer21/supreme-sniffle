package com.draxer.ascension.block;

import com.draxer.ascension.AscensionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Shift + clic derecho sobre la mesa con diamante, lingote de hierro/oro/cobre o fragmento de amatista
 * cambia el mineral de los bordes (gasta 1 unidad fuera de creativo).
 * Se usa el evento porque, agachado y con un ítem en la mano, vanilla no llama al bloque.
 */
@EventBusSubscriber(modid = AscensionMod.MODID)
public final class TrimHandler {
    private TrimHandler() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(AscensionMod.ASCENSION_TABLE.get())) return;

        ItemStack stack = event.getItemStack();
        TableTrim trim = TableTrim.fromItem(stack);
        if (trim == null || state.getValue(AscensionTableBlock.TRIM) == trim) return;

        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(AscensionTableBlock.TRIM, trim), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                        12, 0.35D, 0.1D, 0.35D, 0.02D);
            }
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
