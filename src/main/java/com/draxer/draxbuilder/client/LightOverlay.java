package com.draxer.draxbuilder.client;

import com.draxer.draxbuilder.DraxBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Indicador de luz (tecla F7): marca con partículas el suelo donde pueden aparecer mobs hostiles.
 *  - Rojo: sin luz de bloques ni de cielo -> aparecen siempre.
 *  - Amarillo: sin luz de bloques pero con cielo -> aparecen de noche (o con tormenta).
 * Solo cliente: no necesita estar en el servidor.
 */
@EventBusSubscriber(modid = DraxBuilder.MODID, value = Dist.CLIENT)
public final class LightOverlay {
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.drax_builder.light_overlay", InputConstants.KEY_F7, KeyMapping.Category.MISC);

    private static final int RADIUS = 12;
    private static final int HEIGHT = 6;
    private static final int MAX_PARTICLES = 500;
    private static final DustParticleOptions RED = new DustParticleOptions(0xFF2020, 1.0F);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(0xFFD020, 1.0F);

    private static boolean enabled = false;
    private static int ticks = 0;

    private LightOverlay() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;

        while (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
            player.sendOverlayMessage(Component.translatable(enabled
                    ? "drax_builder.light.on" : "drax_builder.light.off"));
        }
        if (!enabled || ++ticks % 8 != 0) return;

        BlockPos center = player.blockPosition();
        int spawned = 0;
        for (int dx = -RADIUS; dx <= RADIUS && spawned < MAX_PARTICLES; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS && spawned < MAX_PARTICLES; dz++) {
                for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!isSpawnableSpot(level, pos)) continue;
                    if (level.getBrightness(LightLayer.BLOCK, pos) > 0) continue;
                    boolean sky = level.getBrightness(LightLayer.SKY, pos) > 0;
                    level.addParticle(sky ? YELLOW : RED,
                            pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
                    spawned++;
                }
            }
        }
    }

    /** Con el indicador activo, muestra bajo la mira el nivel de luz del bloque al que apuntas. */
    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.gui.screen() != null) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos().relative(hit.getDirection()); // el espacio de aire delante de la cara
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int color = block > 0 ? 0xFF55FF55 : (sky > 0 ? 0xFFFFD020 : 0xFFFF4040);
        Component text = Component.translatable("drax_builder.light.reading", block, sky);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = (width - minecraft.font.width(text)) / 2;
        event.getGuiGraphics().text(minecraft.font, text, x, height / 2 + 12, color, true);
    }

    /** Espacio vacío (sin colisión ni líquido) sobre un bloque con la cara de arriba sólida. */
    private static boolean isSpawnableSpot(ClientLevel level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (!here.getCollisionShape(level, pos).isEmpty() || !here.getFluidState().isEmpty()) return false;
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
