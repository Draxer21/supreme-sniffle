package com.draxer.draxmagnet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Drax Magnet: anillos imán que atraen los ítems y orbes de experiencia cercanos mientras están
 * activos en cualquier parte del inventario. Clic derecho o tecla M para activarlos/desactivarlos.
 */
@Mod(DraxMagnet.MODID)
public class DraxMagnet {
    public static final String MODID = "drax_magnet";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<MagnetItem> MAGNET = ITEMS.registerItem("magnet_ring",
            p -> new MagnetItem(p.stacksTo(1), 8.0D));
    public static final DeferredItem<MagnetItem> ENDER_MAGNET = ITEMS.registerItem("ender_magnet_ring",
            p -> new MagnetItem(p.stacksTo(1).rarity(Rarity.UNCOMMON), 14.0D));

    public DraxMagnet(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(DraxMagnet::addCreative);
        modEventBus.addListener(DraxMagnet::registerPayloads);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGNET);
            event.accept(ENDER_MAGNET);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TogglePayload.TYPE, TogglePayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    // Cambia el primer imán que encuentre (y deja todos los demás igual que ese).
                    Boolean newState = null;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!(stack.getItem() instanceof MagnetItem)) continue;
                        if (newState == null) newState = !MagnetItem.isActive(stack);
                        MagnetItem.setActive(stack, newState);
                    }
                    if (newState != null) MagnetItem.announce(player, newState);
                }));
    }
}
