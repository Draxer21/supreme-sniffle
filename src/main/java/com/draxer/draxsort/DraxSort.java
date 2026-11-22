package com.draxer.draxsort;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Drax Sort: ordena cofres e inventario. El cliente pide el orden con un paquete
 * y el servidor lo hace (así funciona igual en un servidor multijugador).
 */
@Mod(DraxSort.MODID)
public class DraxSort {
    public static final String MODID = "drax_sort";

    public DraxSort(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
        // Preferencias del jugador (modo y filas/columnas): config/drax_sort-client.toml
        modContainer.registerConfig(ModConfig.Type.CLIENT, SortConfig.SPEC);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("5")
                .playToServer(SortPayload.TYPE, SortPayload.STREAM_CODEC, DraxSort::handleSort)
                .playToServer(DepositPayload.TYPE, DepositPayload.STREAM_CODEC, DraxSort::handleDeposit)
                .playToServer(QuickMovePayload.TYPE, QuickMovePayload.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) QuickMovePayload.handle(player, payload);
                        }))
                .playToServer(Restocker.Request.TYPE, Restocker.Request.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) Restocker.restock(player);
                        }))
                .playToServer(NearbySearch.Query.TYPE, NearbySearch.Query.STREAM_CODEC, DraxSort::handleSearch)
                .playToClient(NearbySearch.Result.TYPE, NearbySearch.Result.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> NearbySearch.Highlights.set(payload.positions())));
    }

    private static void handleSort(SortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null || menu.containerId != payload.containerId()) return;
            if (payload.slotIndex() < 0 || payload.slotIndex() >= menu.slots.size()) return;

            Slot origin = menu.getSlot(payload.slotIndex());
            List<Slot> section = Sorter.sectionOf(menu, origin);
            if (section.isEmpty()) return;

            Sorter.sort(player, section, SortMode.byIndex(payload.mode()), payload.columns(), payload.lockedMask(), payload.template());
            menu.broadcastChanges();
        });
    }

    private static void handleSearch(NearbySearch.Query payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NearbySearch.search(player, payload.text());
            }
        });
    }

    private static void handleDeposit(DepositPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Depositor.deposit(player, payload.lockedMask());
            }
        });
    }
}
