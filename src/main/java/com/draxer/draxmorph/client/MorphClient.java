package com.draxer.draxmorph.client;

import com.draxer.draxmorph.DraxMorph;
import com.draxer.draxmorph.MorphForms;
import com.draxer.draxmorph.MorphPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Lado del cliente:
 *  - Teclas: G = elegir forma, V = habilidad.
 *  - Dibujar al jugador transformado: en vez del modelo del jugador se dibuja un mob "de mentira"
 *    (no existe en el mundo) que copia su posición, giro y animación de caminar.
 */
@EventBusSubscriber(modid = DraxMorph.MODID, value = Dist.CLIENT)
public final class MorphClient {
    public static final KeyMapping MENU_KEY = new KeyMapping("key.drax_morph.menu", InputConstants.KEY_G, KeyMapping.Category.MISC);
    public static final KeyMapping ABILITY_KEY = new KeyMapping("key.drax_morph.ability", InputConstants.KEY_V, KeyMapping.Category.MISC);

    /** Lo que hay que dibujar en lugar del jugador. */
    private record MorphRender(EntityRenderer<?, ?> renderer, EntityRenderState state) {}

    private static final ContextKey<MorphRender> MORPH_RENDER = new ContextKey<>(Identifier.fromNamespaceAndPath(DraxMorph.MODID, "morph_render"));

    /** Mob de mentira por jugador (se recrea si cambia la forma). */
    private static final Map<UUID, LivingEntity> FAKES = new HashMap<>();
    private static final Map<UUID, String> LAST_FORM = new HashMap<>();
    private static CameraRenderState camera;
    private static int nextFakeId = -100000;

    private MorphClient() {}

    @EventBusSubscriber(modid = DraxMorph.MODID, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(MENU_KEY);
            event.register(ABILITY_KEY);
        }

        @SubscribeEvent
        static void registerRenderState(RegisterRenderStateModifiersEvent event) {
            event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
                @Override
                public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(
                        T avatar, net.minecraft.client.renderer.entity.state.AvatarRenderState renderState) {
                    renderState.setRenderData(MORPH_RENDER, null);
                    if (!(avatar instanceof Player player)) return;
                    LivingEntity fake = fakeFor(player);
                    if (fake == null) return;
                    EntityRenderer<? super LivingEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(fake);
                    EntityRenderState state = renderer.createRenderState(fake, renderState.partialTick);
                    state.nameTag = null;
                    renderState.setRenderData(MORPH_RENDER, new MorphRender(renderer, state));
                }
            });
        }
    }

    /** Mob de mentira para este jugador, o null si no está transformado. */
    private static LivingEntity fakeFor(Player player) {
        String id = MorphForms.current(player);
        if (id.isEmpty()) return null;
        LivingEntity fake = FAKES.get(player.getUUID());
        if (fake != null && MorphForms.idOf(fake.getType()).equals(id) && fake.level() == player.level()) return fake;
        Entity created = MorphForms.type(id).map(t -> t.create(player.level(), EntitySpawnReason.LOAD)).orElse(null);
        if (!(created instanceof LivingEntity living)) return null;
        // En 26.x una entidad sin id hace fallar el render: le damos un id negativo que nunca usa el mundo.
        living.setId(nextFakeId--);
        copyPose(player, living, true);
        FAKES.put(player.getUUID(), living);
        return living;
    }

    /** Copia posición, giros y edad del jugador al mob de mentira. */
    private static void copyPose(Player player, LivingEntity fake, boolean snap) {
        if (snap) {
            fake.setPos(player.getX(), player.getY(), player.getZ());
        }
        fake.xo = fake.getX();
        fake.yo = fake.getY();
        fake.zo = fake.getZ();
        fake.xOld = fake.getX();
        fake.yOld = fake.getY();
        fake.zOld = fake.getZ();
        fake.setPos(player.getX(), player.getY(), player.getZ());
        fake.yRotO = player.yRotO;
        fake.setYRot(player.getYRot());
        fake.xRotO = player.xRotO;
        fake.setXRot(player.getXRot());
        fake.yBodyRotO = player.yBodyRotO;
        fake.yBodyRot = player.yBodyRot;
        fake.yHeadRotO = player.yHeadRotO;
        fake.yHeadRot = player.yHeadRot;
        fake.setOnGround(player.onGround());
        fake.setShiftKeyDown(player.isShiftKeyDown());
        fake.setInvisible(player.isInvisible());
        fake.tickCount = player.tickCount;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            FAKES.clear();
            LAST_FORM.clear();
            return;
        }
        while (MENU_KEY.consumeClick()) minecraft.gui.setScreen(new MorphScreen());
        while (ABILITY_KEY.consumeClick()) ClientPacketDistributor.sendToServer(MorphPayloads.Ability.INSTANCE);

        for (Player player : level.players()) {
            String id = MorphForms.current(player);
            // Si cambió la forma, recalcular el tamaño también en el cliente.
            String last = LAST_FORM.put(player.getUUID(), id);
            if (last != null && !last.equals(id)) player.refreshDimensions();
            LivingEntity fake = FAKES.get(player.getUUID());
            if (id.isEmpty() || fake == null) continue;
            copyPose(player, fake, false);
            fake.calculateEntityAnimation(false); // animación de caminar según cuánto se movió
        }
        FAKES.keySet().removeIf(uuid -> level.getPlayerByUUID(uuid) == null);
    }

    @SubscribeEvent
    static void onExtract(ExtractLevelRenderStateEvent event) {
        camera = event.getRenderState().cameraRenderState;
    }

    /** En vez del jugador, dibuja el mob de mentira en el mismo lugar. */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        MorphRender morph = event.getRenderState().getRenderData(MORPH_RENDER);
        if (morph == null || camera == null) return;
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        ((EntityRenderer) morph.renderer()).submit(morph.state(), pose, event.getSubmitNodeCollector(), camera);
    }
}
