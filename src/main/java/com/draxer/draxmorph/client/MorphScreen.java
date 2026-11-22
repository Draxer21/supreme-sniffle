package com.draxer.draxmorph.client;

import com.draxer.draxmorph.MorphForms;
import com.draxer.draxmorph.MorphPayloads;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Menú de formas (tecla G): una cuadrícula con las formas absorbidas, buscador y "Humano". */
public class MorphScreen extends Screen {
    private static final int COLS = 3;
    private static final int ROWS = 7;
    private static final int BTN_W = 110;
    private static final int BTN_H = 20;

    private int page = 0;
    private String filter = "";
    private EditBox search;

    public MorphScreen() {
        super(Component.translatable("drax_morph.screen.title"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        String current = MorphForms.current(player);
        int gridW = COLS * (BTN_W + 4) - 4;
        int left = (this.width - gridW) / 2;
        int top = 46;

        this.search = new EditBox(this.font, left, 24, gridW, 16, Component.translatable("drax_morph.screen.search"));
        this.search.setHint(Component.translatable("drax_morph.screen.search"));
        this.search.setValue(this.filter);
        this.search.setResponder(text -> {
            if (!text.equals(this.filter)) {
                this.filter = text;
                this.page = 0;
                this.init();
                this.setFocused(this.search);
                this.search.setFocused(true);
            }
        });
        this.addRenderableWidget(this.search);

        List<String> forms = new ArrayList<>();
        String query = this.filter.toLowerCase(Locale.ROOT);
        for (String id : MorphForms.data(player).unlocked()) {
            String name = MorphForms.type(id).map(t -> t.getDescription().getString()).orElse(id);
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query) || id.contains(query)) forms.add(id);
        }
        forms.sort((a, b) -> name(a).compareToIgnoreCase(name(b)));

        int perPage = COLS * ROWS;
        int pages = Math.max(1, (forms.size() + perPage - 1) / perPage);
        this.page = Math.max(0, Math.min(this.page, pages - 1));

        for (int i = 0; i < perPage; i++) {
            int index = this.page * perPage + i;
            if (index >= forms.size()) break;
            String id = forms.get(index);
            int x = left + (i % COLS) * (BTN_W + 4);
            int y = top + (i / COLS) * (BTN_H + 3);
            MutableComponent label = Component.literal(name(id));
            if (id.equals(current)) label = label.withColor(0x55FF55);
            Button button = Button.builder(label, b -> this.choose(id)).bounds(x, y, BTN_W, BTN_H)
                    .tooltip(Tooltip.create(describe(id))).build();
            button.active = !id.equals(current);
            this.addRenderableWidget(button);
        }

        int bottom = top + ROWS * (BTN_H + 3) + 6;
        Button human = Button.builder(Component.translatable("drax_morph.screen.human"), b -> this.choose(""))
                .bounds(left, bottom, BTN_W, BTN_H).build();
        human.active = !current.isEmpty();
        this.addRenderableWidget(human);
        Button prev = Button.builder(Component.literal("◀"), b -> {
            this.page--;
            this.init();
        }).bounds(left + BTN_W + 4, bottom, 20, BTN_H).build();
        prev.active = this.page > 0;
        this.addRenderableWidget(prev);
        Button next = Button.builder(Component.literal("▶"), b -> {
            this.page++;
            this.init();
        }).bounds(left + BTN_W + 28, bottom, 20, BTN_H).build();
        next.active = this.page < pages - 1;
        this.addRenderableWidget(next);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(left + gridW - BTN_W, bottom, BTN_W, BTN_H).build());
    }

    private static String name(String id) {
        return MorphForms.type(id).map(t -> t.getDescription().getString()).orElse(id);
    }

    /** Tooltip: vida y rasgos de la forma. */
    private static Component describe(String id) {
        MutableComponent text = Component.translatable("drax_morph.screen.health", (int) MorphForms.maxHealth(id));
        for (MorphForms.Trait trait : MorphForms.traits(id)) {
            text.append(Component.literal("\n• ")).append(Component.translatable("drax_morph.trait." + trait.name().toLowerCase(Locale.ROOT)));
        }
        MorphForms.Ability ability = MorphForms.ability(id);
        if (ability != MorphForms.Ability.NONE) {
            text.append(Component.literal("\n")).append(Component.translatable("drax_morph.screen.ability",
                    Component.translatable("drax_morph.ability." + ability.name().toLowerCase(Locale.ROOT))).withColor(0xFFD24A));
        }
        return text;
    }

    private void choose(String id) {
        ClientPacketDistributor.sendToServer(new MorphPayloads.Select(id));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 10, 0xFFFFFFFF, true);
        Player player = Minecraft.getInstance().player;
        if (player != null && MorphForms.data(player).unlocked().isEmpty()) {
            Component empty = Component.translatable("drax_morph.screen.empty");
            graphics.text(this.font, empty, (this.width - this.font.width(empty)) / 2, 80, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
