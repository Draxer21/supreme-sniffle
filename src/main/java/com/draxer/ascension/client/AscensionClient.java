package com.draxer.ascension.client;

import com.draxer.ascension.AscensionMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Registro de la pantalla y del renderizador del libro. Esta clase solo se carga en el cliente. */
@EventBusSubscriber(modid = AscensionMod.MODID, value = Dist.CLIENT)
public final class AscensionClient {
    private AscensionClient() {}

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AscensionMod.ASCENSION_MENU.get(), AscensionScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AscensionMod.ASCENSION_TABLE_BE.get(), FloatingTomeRenderer::new);
    }
}
