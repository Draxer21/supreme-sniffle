package com.draxer.draxwaypoints.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Guarda los puntos de cada mundo (o servidor) en config/drax_waypoints/&lt;mundo&gt;.json.
 */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Waypoint> waypoints = new ArrayList<>();
    private static String loadedKey = null;

    private WaypointStore() {}

    /** Nombre del mundo (un jugador) o dirección del servidor (multijugador), como nombre de archivo seguro. */
    private static String worldKey() {
        String key;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            key = "sp_" + server.getWorldData().getLevelName();
        } else {
            var data = Minecraft.getInstance().getCurrentServer();
            key = "mp_" + (data != null ? data.ip : "unknown");
        }
        return key.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static Path file(String key) {
        return FMLPaths.CONFIGDIR.get().resolve("drax_waypoints").resolve(key + ".json");
    }

    /** Lista del mundo actual (la carga si cambiaste de mundo). */
    public static List<Waypoint> all() {
        if (Minecraft.getInstance().level == null) return List.of();
        String key = worldKey();
        if (!key.equals(loadedKey)) load(key);
        return waypoints;
    }

    private static void load(String key) {
        loadedKey = key;
        waypoints = new ArrayList<>();
        Path path = file(key);
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<Waypoint> read = GSON.fromJson(reader, new TypeToken<List<Waypoint>>() {}.getType());
            if (read != null) waypoints = new ArrayList<>(read);
        } catch (IOException | RuntimeException e) {
            waypoints = new ArrayList<>();
        }
    }

    public static void save() {
        if (loadedKey == null) return;
        Path path = file(loadedKey);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(waypoints, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void add(Waypoint waypoint) {
        all().add(waypoint);
        save();
    }

    public static void remove(Waypoint waypoint) {
        all().remove(waypoint);
        save();
    }
}
