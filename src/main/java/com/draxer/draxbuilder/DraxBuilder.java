package com.draxer.draxbuilder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Drax Builder: varitas de construcción (extender y rellenar), varitas de intercambio, deshacer (Ctrl+Z)
 * y un indicador de luz (tecla F7) para ver dónde pueden aparecer mobs.
 */
@Mod(DraxBuilder.MODID)
public class DraxBuilder {
    public static final String MODID = "drax_builder";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<BuildingWandItem> IRON_WAND = ITEMS.registerItem("iron_building_wand",
            p -> new BuildingWandItem(p.durability(250), 9));
    public static final DeferredItem<BuildingWandItem> DIAMOND_WAND = ITEMS.registerItem("diamond_building_wand",
            p -> new BuildingWandItem(p.durability(1561), 25));
    public static final DeferredItem<BuildingWandItem> NETHERITE_WAND = ITEMS.registerItem("netherite_building_wand",
            p -> new BuildingWandItem(p.durability(4062).fireResistant(), 64));

    public static final DeferredItem<ExchangeWandItem> IRON_EXCHANGE = ITEMS.registerItem("iron_exchange_wand",
            p -> new ExchangeWandItem(p.durability(250), 9));
    public static final DeferredItem<ExchangeWandItem> DIAMOND_EXCHANGE = ITEMS.registerItem("diamond_exchange_wand",
            p -> new ExchangeWandItem(p.durability(1561), 25));
    public static final DeferredItem<ExchangeWandItem> NETHERITE_EXCHANGE = ITEMS.registerItem("netherite_exchange_wand",
            p -> new ExchangeWandItem(p.durability(4062).fireResistant(), 64));

    public DraxBuilder(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(DraxBuilder::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(UndoPayload.TYPE, UndoPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) BuildHistory.undo(player);
                }));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(IRON_WAND);
            event.accept(DIAMOND_WAND);
            event.accept(NETHERITE_WAND);
            event.accept(IRON_EXCHANGE);
            event.accept(DIAMOND_EXCHANGE);
            event.accept(NETHERITE_EXCHANGE);
        }
    }
}
