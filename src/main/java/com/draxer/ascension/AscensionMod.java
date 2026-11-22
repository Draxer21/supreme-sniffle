package com.draxer.ascension;

import com.draxer.ascension.block.AscensionTableBlock;
import com.draxer.ascension.block.AscensionTableBlockEntity;
import com.draxer.ascension.menu.AscensionMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Mesa de Ascensión: permite subir encantamientos existentes de un ítem
 * (hasta el nivel configurado, 15 por defecto) gastando materiales con valor en puntos.
 */
@Mod(AscensionMod.MODID)
public class AscensionMod {
    public static final String MODID = "ascension";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredBlock<AscensionTableBlock> ASCENSION_TABLE = BLOCKS.registerBlock(
            "ascension_table",
            AscensionTableBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.STONE));

    public static final DeferredItem<BlockItem> ASCENSION_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem("ascension_table", ASCENSION_TABLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AscensionTableBlockEntity>> ASCENSION_TABLE_BE =
            BLOCK_ENTITY_TYPES.register("ascension_table",
                    () -> new BlockEntityType<>(AscensionTableBlockEntity::new, ASCENSION_TABLE.get()));

    /*
     * Ítems decorativos (no aparecen en el menú creativo): sus modelos 3D son las piezas del libro
     * que flota sobre la mesa. Una mitad (tapa + páginas) se dibuja dos veces, en espejo, y aparte
     * una hoja suelta que se va pasando.
     */
    public static final DeferredItem<Item> TOME_HALF = ITEMS.registerSimpleItem("tome_half", p -> p.stacksTo(1));
    public static final DeferredItem<Item> TOME_PAGE = ITEMS.registerSimpleItem("tome_page", p -> p.stacksTo(1));

    public static final DeferredHolder<MenuType<?>, MenuType<AscensionMenu>> ASCENSION_MENU = MENUS.register(
            "ascension_table",
            () -> IMenuTypeExtension.create((containerId, inventory, extraData) -> new AscensionMenu(containerId, inventory)));

    public AscensionMod(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        // Archivo: config/ascension-common.toml
        modContainer.registerConfig(ModConfig.Type.COMMON, AscensionConfig.SPEC);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ASCENSION_TABLE_ITEM);
        }
    }
}
