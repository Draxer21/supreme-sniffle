package com.draxer.gigaenchants.client;

import com.draxer.gigaenchants.GigaCheck;
import com.draxer.gigaenchants.GigaEnchants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Lo que distingue a un ítem Giga a simple vista:
 *  - su nombre sale en dorado y el tooltip dice "✦ Giga";
 *  - quien lo tiene en la mano suelta destellos dorados.
 */
@EventBusSubscriber(modid = GigaEnchants.MODID, value = Dist.CLIENT)
public final class GigaShine {
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xFFC830, 0.6F);
    private static int ticks = 0;

    private GigaShine() {}

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!GigaCheck.hasGiga(stack)) return;
        List<Component> lines = event.getToolTip();
        if (!lines.isEmpty()) lines.set(0, lines.get(0).copy().withColor(0xFFC830));
        lines.add(Math.min(1, lines.size()), Component.translatable("giga_enchants.tooltip.giga").withColor(0xFFD966));
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (++ticks % 4 != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;
        for (Player player : level.players()) {
            if (player.distanceToSqr(minecraft.player) > 32.0D * 32.0D) continue;
            boolean main = GigaCheck.hasGiga(player.getMainHandItem());
            boolean off = GigaCheck.hasGiga(player.getOffhandItem());
            if (!main && !off) continue;
            // En primera persona, los destellos van un poco delante para que se vean.
            Vec3 look = player.getViewVector(1.0F);
            Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
            double hand = (main ? 0.35D : -0.35D);
            Vec3 at = player.getEyePosition().add(look.scale(0.6D)).add(side.scale(hand)).add(0.0D, -0.45D, 0.0D);
            level.addParticle(GOLD,
                    at.x + (player.getRandom().nextDouble() - 0.5D) * 0.3D,
                    at.y + (player.getRandom().nextDouble() - 0.5D) * 0.3D,
                    at.z + (player.getRandom().nextDouble() - 0.5D) * 0.3D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}
