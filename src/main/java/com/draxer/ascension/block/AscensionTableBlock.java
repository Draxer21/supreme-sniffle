package com.draxer.ascension.block;

import com.draxer.ascension.AscensionMod;
import com.draxer.ascension.menu.AscensionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class AscensionTableBlock extends Block implements EntityBlock {
    public static final EnumProperty<TableTrim> TRIM = EnumProperty.create("trim", TableTrim.class);

    private static final Component TITLE = Component.translatable("container.ascension.ascension_table");
    /** Misma forma que la mesa de encantamientos vanilla: 12 píxeles de alto. */
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public AscensionTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIM, TableTrim.DIAMOND));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIM);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AscensionTableBlockEntity(pos, state);
    }

    /** Solo el cliente necesita "tickear" la mesa, para animar el libro. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || type != AscensionMod.ASCENSION_TABLE_BE.get()) return null;
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof AscensionTableBlockEntity table) {
                table.clientTick();
            }
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Animación ambiental (solo cliente): glifos de encantamiento que vuelan hacia el libro flotante
     * y, de vez en cuando, una chispa que sube desde la cubierta.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        double bookY = pos.getY() + 1.1D;

        for (int i = 0; i < 2; i++) {
            if (random.nextInt(3) == 0) {
                // ENCHANT viaja desde (posición + velocidad) hacia la posición: los glifos "entran" al libro.
                level.addParticle(ParticleTypes.ENCHANT,
                        cx, bookY, cz,
                        (random.nextFloat() - 0.5D) * 3.0D,
                        random.nextFloat() * 1.2D - 0.4D,
                        (random.nextFloat() - 0.5D) * 3.0D);
            }
        }

        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    cx + (random.nextFloat() - 0.5D) * 0.3D, pos.getY() + 0.8D, cz + (random.nextFloat() - 0.5D) * 0.3D,
                    0.0D, 0.03D + random.nextFloat() * 0.02D, 0.0D);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new AscensionMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                TITLE);
    }
}
