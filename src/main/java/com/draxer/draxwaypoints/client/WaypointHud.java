package com.draxer.draxwaypoints.client;

import com.draxer.draxwaypoints.DraxWaypoints;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Brújula arriba de la pantalla (N/E/S/O y los puntos de ruta con su distancia), coordenadas y
 * dirección, columnas de partículas sobre los puntos cercanos y punto automático al morir.
 * Teclas: B = nuevo punto aquí, N = lista de puntos, H = mostrar/ocultar la brújula.
 */
@EventBusSubscriber(modid = DraxWaypoints.MODID, value = Dist.CLIENT)
public final class WaypointHud {
    public static final KeyMapping ADD_KEY = new KeyMapping("key.drax_waypoints.add", InputConstants.KEY_B, KeyMapping.Category.MISC);
    public static final KeyMapping LIST_KEY = new KeyMapping("key.drax_waypoints.list", InputConstants.KEY_N, KeyMapping.Category.MISC);
    public static final KeyMapping HUD_KEY = new KeyMapping("key.drax_waypoints.hud", InputConstants.KEY_H, KeyMapping.Category.MISC);

    private static final int BAR_WIDTH = 200;
    private static final float BAR_FOV = 180.0F; // grados que abarca la barra
    private static boolean hudVisible = true;
    private static boolean wasDead = false;
    private static int ticks = 0;

    private WaypointHud() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ADD_KEY);
        event.register(LIST_KEY);
        event.register(HUD_KEY);
    }

    public static String dimensionOf(ClientLevel level) {
        return level.dimension().identifier().toString();
    }

    public static Waypoint createHere(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        BlockPos pos = player.blockPosition();
        int color = Waypoint.COLORS[WaypointStore.all().size() % Waypoint.COLORS.length];
        return new Waypoint(name, pos.getX(), pos.getY(), pos.getZ(), dimensionOf(minecraft.level), color);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            wasDead = false;
            return;
        }

        while (ADD_KEY.consumeClick()) {
            Waypoint waypoint = createHere(Component.translatable("drax_waypoints.default_name", WaypointStore.all().size() + 1).getString());
            WaypointStore.add(waypoint);
            player.sendOverlayMessage(Component.translatable("drax_waypoints.added", waypoint.name, waypoint.x, waypoint.y, waypoint.z));
        }
        while (LIST_KEY.consumeClick()) {
            minecraft.gui.setScreen(new WaypointScreen());
        }
        while (HUD_KEY.consumeClick()) {
            hudVisible = !hudVisible;
        }

        // Punto automático donde moriste.
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            Waypoint death = createHere(Component.translatable("drax_waypoints.death_name").getString());
            death.color = 0xAA3333;
            WaypointStore.all().removeIf(w -> w.name.equals(death.name)); // solo la última muerte
            WaypointStore.add(death);
        }
        wasDead = dead;

        // Columna de luz sobre los puntos a menos de 160 bloques.
        if (++ticks % 6 != 0) return;
        String dim = dimensionOf(level);
        for (Waypoint w : WaypointStore.all()) {
            if (!w.visible || !w.dimension.equals(dim)) continue;
            double dx = w.x + 0.5D - player.getX();
            double dz = w.z + 0.5D - player.getZ();
            if (dx * dx + dz * dz > 160.0D * 160.0D) continue;
            DustParticleOptions dust = new DustParticleOptions(w.color, 1.5F);
            for (int i = 0; i < 6; i++) {
                double y = w.y + 0.5D + level.getRandom().nextDouble() * 24.0D;
                level.addAlwaysVisibleParticle(dust, w.x + 0.5D, y, w.z + 0.5D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!hudVisible || player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (minecraft.gui.hud.isHidden()) return; // F1
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int left = (width - BAR_WIDTH) / 2;
        int top = 3;

        // Fondo de la barra
        g.fill(left - 2, top - 1, left + BAR_WIDTH + 2, top + 11, 0x80000000);
        float yaw = Mth.wrapDegrees(player.getYRot());

        // Puntos cardinales cada 45°
        String[] names = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};
        for (int i = 0; i < 8; i++) {
            float angle = i * 45.0F; // 0 = sur en Minecraft
            float diff = Mth.wrapDegrees(angle - yaw);
            if (Math.abs(diff) > BAR_FOV / 2.0F) continue;
            int x = left + BAR_WIDTH / 2 + Math.round(diff / BAR_FOV * BAR_WIDTH);
            String label = Component.translatable("drax_waypoints.dir." + names[i]).getString();
            int color = names[i].equals("N") ? 0xFFFF5555 : (names[i].length() == 1 ? 0xFFFFFFFF : 0xFFAAAAAA);
            g.text(font, Component.literal(label), x - font.width(label) / 2, top + 1, color, true);
        }
        g.fill(left + BAR_WIDTH / 2, top - 1, left + BAR_WIDTH / 2 + 1, top + 11, 0xFFFFD24A); // centro

        // Puntos de ruta
        String dim = dimensionOf(minecraft.level);
        List<Waypoint> list = WaypointStore.all();
        for (Waypoint w : list) {
            if (!w.visible || !w.dimension.equals(dim)) continue;
            double dx = w.x + 0.5D - player.getX();
            double dz = w.z + 0.5D - player.getZ();
            float angle = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            float diff = Mth.wrapDegrees(angle - yaw);
            if (Math.abs(diff) > BAR_FOV / 2.0F) continue;
            int x = left + BAR_WIDTH / 2 + Math.round(diff / BAR_FOV * BAR_WIDTH);
            int color = 0xFF000000 | w.color;
            g.fill(x - 2, top + 3, x + 3, top + 8, 0xFF000000);
            g.fill(x - 1, top + 4, x + 2, top + 7, color);
            // Nombre y distancia solo del que está cerca del centro (si no, se amontonan).
            if (Math.abs(diff) < 12.0F) {
                int dist = (int) Math.sqrt(dx * dx + dz * dz + Math.pow(w.y - player.getY(), 2));
                String text = w.name + " · " + dist + "m";
                g.text(font, Component.literal(text), x - font.width(text) / 2, top + 13, color, true);
            }
        }

        // Coordenadas, dirección y bioma debajo de la barra
        BlockPos pos = player.blockPosition();
        String facing = Component.translatable("drax_waypoints.facing." + player.getDirection().getName()).getString();
        String biome = minecraft.level.getBiome(pos).unwrapKey()
                .map(k -> Component.translatable("biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()).getString())
                .orElse("");
        String info = pos.getX() + " " + pos.getY() + " " + pos.getZ() + "  ·  " + facing + (biome.isEmpty() ? "" : "  ·  " + biome);
        g.text(font, Component.literal(info), (width - font.width(info)) / 2, top + 24, 0xFFE0E0E0, true);
    }
}
