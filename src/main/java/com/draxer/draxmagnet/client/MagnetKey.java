package com.draxer.draxmagnet.client;

import com.draxer.draxmagnet.DraxMagnet;
import com.draxer.draxmagnet.TogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Tecla M (configurable): activa o desactiva los imanes del inventario sin sacarlos. */
@EventBusSubscriber(modid = DraxMagnet.MODID, value = Dist.CLIENT)
public final class MagnetKey {
    public static final KeyMapping TOGGLE = new KeyMapping("key.drax_magnet.toggle", InputConstants.KEY_M, KeyMapping.Category.MISC);

    private MagnetKey() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;
        while (TOGGLE.consumeClick()) {
            ClientPacketDistributor.sendToServer(TogglePayload.INSTANCE);
        }
    }
}
