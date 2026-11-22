package com.draxer.ascension.client;

import com.draxer.ascension.AscensionMod;
import com.draxer.ascension.UpgradeLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Con la Mesa de Ascensión abierta, el tooltip de cada ítem muestra cuántos puntos vale como material. */
@EventBusSubscriber(modid = AscensionMod.MODID, value = Dist.CLIENT)
public final class MaterialTooltip {
    private MaterialTooltip() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (!(Minecraft.getInstance().gui.screen() instanceof AscensionScreen)) return;
        int points = UpgradeLogic.pointsPerItem(event.getItemStack());
        if (points <= 0) return;
        int count = event.getItemStack().getCount();
        Component line = count > 1
                ? Component.translatable("gui.ascension.value.stack", points, (long) points * count)
                : Component.translatable("gui.ascension.value", points);
        event.getToolTip().add(line.copy().withColor(0xFFD86B));
    }
}
