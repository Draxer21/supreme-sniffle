package com.draxer.draxgrave;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Drax Grave: al morir, los ítems del jugador se guardan en una tumba en el lugar de la muerte.
 * Solo el dueño (o alguien en creativo) puede abrirla; es indestructible para que nada la rompa.
 */
@Mod(DraxGrave.MODID)
public class DraxGrave {
    public static final String MODID = "drax_grave";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<GraveBlock> GRAVE = BLOCKS.registerBlock(
            "grave",
            GraveBlock::new,
            p -> p.mapColor(MapColor.STONE)
                    .strength(-1.0F, 3600000.0F)   // indestructible, inmune a explosiones
                    .noLootTable()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.STONE));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE_BE =
            BLOCK_ENTITY_TYPES.register("grave", () -> new BlockEntityType<>(GraveBlockEntity::new, GRAVE.get()));

    public DraxGrave(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
