package com.draxer.draxbuilder.client;

import com.draxer.draxbuilder.BuildingWandItem;
import com.draxer.draxbuilder.DraxBuilder;
import com.draxer.draxbuilder.ExchangeWandItem;
import com.draxer.draxbuilder.UndoPayload;
import com.draxer.draxbuilder.WandData;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

/**
 * Vista previa de las varitas: mientras tienes una en la mano y apuntas a un bloque, unas partículas
 * marcan qué va a pasar. Celeste = bloques que se pondrán; naranjo = bloques que se cambiarán;
 * dorado = la esquina ya marcada del modo relleno.
 * También maneja la tecla Ctrl+Z para deshacer.
 */
@EventBusSubscriber(modid = DraxBuilder.MODID, value = Dist.CLIENT)
public final class WandPreview {
    public static final KeyMapping UNDO_KEY = new KeyMapping("key.drax_builder.undo", KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL, InputConstants.Type.KEYSYM, InputConstants.KEY_Z, KeyMapping.Category.MISC);

    private static final DustParticleOptions PLACE = new DustParticleOptions(0x40E0FF, 0.7F);
    private static final DustParticleOptions SWAP = new DustParticleOptions(0xFF8A30, 0.7F);
    private static final DustParticleOptions CORNER = new DustParticleOptions(0xFFD040, 1.2F);
    private static final double[][] CORNERS = {
            {0.15, 0.15, 0.15}, {0.85, 0.15, 0.15}, {0.15, 0.15, 0.85}, {0.85, 0.15, 0.85},
            {0.15, 0.85, 0.15}, {0.85, 0.85, 0.15}, {0.15, 0.85, 0.85}, {0.85, 0.85, 0.85}};
    private static int ticks = 0;

    private WandPreview() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(UNDO_KEY);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;

        while (UNDO_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(UndoPayload.INSTANCE);
        }

        if (++ticks % 4 != 0 || minecraft.gui.screen() != null) return;
        ItemStack wand = player.getMainHandItem();
        boolean building = wand.getItem() instanceof BuildingWandItem;
        boolean exchange = wand.getItem() instanceof ExchangeWandItem;
        if (!building && !exchange) return;

        // Esquina marcada del modo relleno: siempre visible mientras tengas la varita.
        if (building && WandData.mode(wand) == WandData.MODE_FILL) {
            Optional<BlockPos> corner = WandData.corner(wand);
            corner.ifPresent(c -> mark(level, c, CORNER, true));
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        List<BlockPos> targets = building
                ? ((BuildingWandItem) wand.getItem()).plannedTargets(level, player, wand, hit.getBlockPos(), hit.getDirection())
                : ((ExchangeWandItem) wand.getItem()).plannedTargets(level, player, wand, hit.getBlockPos(), hit.getDirection());
        DustParticleOptions color = building ? PLACE : SWAP;
        // Con muchas posiciones, una partícula por bloque; con pocas, las 8 esquinas para que se vea el cubo.
        boolean corners = targets.size() <= 16;
        for (BlockPos pos : targets) mark(level, pos, color, corners);
    }

    private static void mark(ClientLevel level, BlockPos pos, DustParticleOptions color, boolean corners) {
        if (corners) {
            for (double[] c : CORNERS) {
                level.addParticle(color, pos.getX() + c[0], pos.getY() + c[1], pos.getZ() + c[2], 0.0D, 0.0D, 0.0D);
            }
        } else {
            level.addParticle(color, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }
}
