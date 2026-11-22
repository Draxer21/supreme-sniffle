package com.draxer.draxwaypoints.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Lista de puntos de ruta del mundo actual: renombrar (escribiendo en el nombre), cambiar color,
 * mostrar/ocultar, borrar y crear uno nuevo en tu posición.
 */
public class WaypointScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int ROWS = 8;
    private static final int W = 320;

    private int scroll = 0;
    private final List<EditBox> nameBoxes = new ArrayList<>();

    public WaypointScreen() {
        super(Component.translatable("drax_waypoints.screen.title"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.nameBoxes.clear();
        List<Waypoint> list = WaypointStore.all();
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, list.size() - ROWS)));
        int left = (this.width - W) / 2;
        int top = 40;

        for (int r = 0; r < ROWS; r++) {
            int index = this.scroll + r;
            if (index >= list.size()) break;
            Waypoint w = list.get(index);
            int y = top + r * ROW_H;

            EditBox name = new EditBox(this.font, left, y, 120, 18, Component.literal(w.name));
            name.setMaxLength(32);
            name.setValue(w.name);
            name.setResponder(text -> {
                w.name = text;
                WaypointStore.save();
            });
            this.nameBoxes.add(this.addRenderableWidget(name));

            this.addRenderableWidget(Button.builder(Component.literal("●").withColor(w.color), b -> {
                w.nextColor();
                WaypointStore.save();
                this.init();
            }).bounds(left + 214, y, 20, 18).tooltip(Tooltip.create(Component.translatable("drax_waypoints.screen.color"))).build());

            this.addRenderableWidget(Button.builder(Component.literal(w.visible ? "◉" : "○"), b -> {
                w.visible = !w.visible;
                WaypointStore.save();
                this.init();
            }).bounds(left + 238, y, 20, 18).tooltip(Tooltip.create(Component.translatable(
                    w.visible ? "drax_waypoints.screen.hide" : "drax_waypoints.screen.show"))).build());

            this.addRenderableWidget(Button.builder(Component.literal("✖").withColor(0xFF5555), b -> {
                WaypointStore.remove(w);
                this.init();
            }).bounds(left + 262, y, 20, 18).tooltip(Tooltip.create(Component.translatable("drax_waypoints.screen.delete"))).build());
        }

        int bottom = top + ROWS * ROW_H + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("drax_waypoints.screen.add_here"), b -> {
            WaypointStore.add(WaypointHud.createHere(Component.translatable("drax_waypoints.default_name", WaypointStore.all().size() + 1).getString()));
            this.scroll = Math.max(0, WaypointStore.all().size() - ROWS);
            this.init();
        }).bounds(left, bottom, 120, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            this.scroll--;
            this.init();
        }).bounds(left + 286, top, 20, 18).build()).active = this.scroll > 0;
        this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            this.scroll++;
            this.init();
        }).bounds(left + 286, top + (ROWS - 1) * ROW_H, 20, 18).build()).active = this.scroll + ROWS < list.size();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(left + W - 120, bottom, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - W) / 2;
        int top = 40;
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 16, 0xFFFFFFFF, true);

        List<Waypoint> list = WaypointStore.all();
        if (list.isEmpty()) {
            Component empty = Component.translatable("drax_waypoints.screen.empty");
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, top + 20, 0xFFAAAAAA, false);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        for (int r = 0; r < ROWS; r++) {
            int index = this.scroll + r;
            if (index >= list.size()) break;
            Waypoint w = list.get(index);
            int y = top + r * ROW_H + 5;
            String coords = w.x + " " + w.y + " " + w.z;
            String dist = "";
            if (player != null && Minecraft.getInstance().level != null
                    && w.dimension.equals(WaypointHud.dimensionOf(Minecraft.getInstance().level))) {
                dist = (int) Math.sqrt(player.distanceToSqr(w.x + 0.5D, w.y, w.z + 0.5D)) + "m";
            } else {
                dist = w.dimension.replace("minecraft:", "");
            }
            graphics.text(this.font, Component.literal(coords), left + 126, y - 4, 0xFFDDDDDD, false);
            graphics.text(this.font, Component.literal(dist), left + 126, y + 5, 0xFF999999, false);
        }
    }

    @Override
    public void onClose() {
        WaypointStore.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
