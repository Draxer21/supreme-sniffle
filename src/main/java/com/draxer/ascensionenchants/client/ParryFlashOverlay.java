package com.draxer.ascensionenchants.client;

import com.draxer.ascensionenchants.AscensionEnchants;
import com.draxer.ascensionenchants.parry.ParryFlash;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Destello blanco-azulado que se desvanece sobre toda la pantalla al hacer un parry perfecto. */
@EventBusSubscriber(modid = AscensionEnchants.MODID, value = Dist.CLIENT)
public final class ParryFlashOverlay {
    private ParryFlashOverlay() {}

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        float alpha = ParryFlash.currentAlpha();
        if (alpha <= 0.0F) return;
        int a = Math.min(255, (int) (alpha * 140.0F)); // como mucho ~55 % de opacidad
        int color = (a << 24) | 0xE8F4FF;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, width, height, color);
    }
}
