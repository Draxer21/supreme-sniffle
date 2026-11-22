package com.draxer.ascension.client;

import com.draxer.ascension.UpgradeLogic;
import com.draxer.ascension.block.TableTrim;
import com.draxer.ascension.menu.AscensionMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Pantalla de la Mesa de Ascensión, con tema arcano (violeta oscuro y dorado).
 * Todo se dibuja con rectángulos (no usa textura),
 * y cada fila de la lista es un botón vanilla que envía su índice al servidor.
 */
public class AscensionScreen extends AbstractContainerScreen<AscensionMenu> {
    private static final int WIDTH = 222;
    private static final int HEIGHT = 186;

    private static final int LIST_X = 66;
    private static final int LIST_Y = 16;
    private static final int LIST_W = 136;
    private static final int ROW_H = 14;
    private static final int ROWS = 5;
    private static final int GIGA_W = 14;
    private static final int SMALL_W = 10;

    // Paleta arcana
    private static final int EDGE = 0xFF0E0718;
    private static final int GOLD = 0xFFC9A227;
    private static final int GOLD_LIGHT = 0xFFF0D890;
    private static final int PANEL = 0xFF2B1B45;
    private static final int PANEL_LIGHT = 0xFF3D2763;
    private static final int PANEL_DARK = 0xFF190F2A;
    private static final int LIST_BG = 0xFF160D26;
    private static final int SLOT_BORDER = 0xFF6E4FA8;
    private static final int SLOT_INNER = 0xFF120A1E;
    private static final int TEXT_COLOR = 0xFFD8C8F0;
    private static final int POINTS_COLOR = 0xFFFFD86B;
    private static final int ACCENT = 0xFF9B6BE0;

    /** Zona del texto del borde (arriba a la derecha), para el tooltip. */
    private int trimTextX = -1;
    private int trimTextW = 0;
    private int shelfTextX = -1;
    private int shelfTextW = 0;

    private final List<Button> rowButtons = new ArrayList<>();
    private final List<Button> gigaButtons = new ArrayList<>();
    private final List<Button> downButtons = new ArrayList<>();
    private final List<Button> extractButtons = new ArrayList<>();
    private Button applyButton;
    private final List<String> rowState = new ArrayList<>();
    private Button upButton;
    private Button downButton;
    private int scroll = 0;

    public AscensionScreen(AscensionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.inventoryLabelX = AscensionMenu.INV_X;
    }

    @Override
    protected void init() {
        super.init();
        this.rowButtons.clear();
        this.gigaButtons.clear();
        this.downButtons.clear();
        this.extractButtons.clear();
        this.rowState.clear();
        for (int r = 0; r < ROWS; r++) {
            final int row = r;
            Button button = Button.builder(Component.empty(), b -> this.onRowPressed(row))
                    .bounds(this.leftPos + LIST_X, this.topPos + LIST_Y + r * ROW_H, LIST_W, ROW_H - 1)
                    .build();
            this.rowButtons.add(this.addRenderableWidget(button));
            // Botón ★ para convertir el encantamiento a su versión Giga (si existe).
            Button giga = Button.builder(Component.literal("★"), b -> this.onGigaPressed(row))
                    .bounds(this.leftPos + LIST_X + LIST_W - GIGA_W, this.topPos + LIST_Y + r * ROW_H, GIGA_W, ROW_H - 1)
                    .build();
            giga.visible = false;
            this.gigaButtons.add(this.addRenderableWidget(giga));
            // − bajar un nivel (devuelve la mitad a la mesa) y ⇪ extraer a libro.
            Button down = Button.builder(Component.literal("−"), b -> this.send(AscensionMenu.DOWNGRADE_BUTTON_OFFSET + this.scroll + row))
                    .bounds(0, this.topPos + LIST_Y + r * ROW_H, SMALL_W, ROW_H - 1).build();
            down.visible = false;
            this.downButtons.add(this.addRenderableWidget(down));
            Button extract = Button.builder(Component.literal("⇪"), b -> this.send(AscensionMenu.EXTRACT_BUTTON_OFFSET + this.scroll + row))
                    .bounds(0, this.topPos + LIST_Y + r * ROW_H, SMALL_W, ROW_H - 1).build();
            extract.visible = false;
            this.extractButtons.add(this.addRenderableWidget(extract));
            this.rowState.add("");
        }
        this.upButton = this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> this.scrollBy(-1))
                .bounds(this.leftPos + LIST_X + LIST_W + 1, this.topPos + LIST_Y, 10, ROW_H - 1)
                .build());
        this.downButton = this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> this.scrollBy(1))
                .bounds(this.leftPos + LIST_X + LIST_W + 1, this.topPos + LIST_Y + (ROWS - 1) * ROW_H, 10, ROW_H - 1)
                .build());
        // ✚ aplicar el libro encantado al ítem (como el yunque, sin el "demasiado caro").
        this.applyButton = this.addRenderableWidget(Button.builder(Component.literal("✚"), b -> this.send(AscensionMenu.APPLY_BOOK_BUTTON))
                .bounds(this.leftPos + 44, this.topPos + 21, 18, 18).build());
        this.updateButtons();
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    private void scrollBy(int delta) {
        this.scroll += delta;
        this.updateButtons();
    }

    private void onRowPressed(int row) {
        int index = this.scroll + row;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
    }

    private void onGigaPressed(int row) {
        int index = this.scroll + row;
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, AscensionMenu.GIGA_BUTTON_OFFSET + index);
        }
    }

    private void updateButtons() {
        if (this.rowButtons.isEmpty()) return;

        ItemStack target = this.menu.getTarget();
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        int maxScroll = Math.max(0, Math.min(entries.size(), AscensionMenu.MAX_ENTRIES) - ROWS);
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
        int points = this.menu.getPoints();
        boolean plainBook = this.menu.getBook().is(net.minecraft.world.item.Items.BOOK);
        this.updateApplyButton(points);

        for (int r = 0; r < ROWS; r++) {
            Button button = this.rowButtons.get(r);
            int index = this.scroll + r;
            Button gigaButton = this.gigaButtons.get(r);
            if (index >= entries.size() || index >= AscensionMenu.MAX_ENTRIES) {
                button.visible = false;
                gigaButton.visible = false;
                this.downButtons.get(r).visible = false;
                this.extractButtons.get(r).visible = false;
                this.rowState.set(r, ""); // al reaparecer, la fila se vuelve a configurar completa
                continue;
            }
            button.visible = true;

            Holder<Enchantment> holder = entries.get(index);
            int level = UpgradeLogic.currentLevel(target, holder);
            int cost = this.menu.getCost(index);
            Optional<Holder.Reference<Enchantment>> giga = this.menu.gigaFor(holder);
            boolean catalyst = this.menu.hasGigaCatalyst();

            // Evita recrear textos y tooltips si nada cambió.
            String state = holder.getRegisteredName() + "|" + level + "|" + cost + "|" + points + "|" + giga.isPresent() + "|" + catalyst
                    + "|" + plainBook + "|" + this.menu.getShelves() + "|" + this.menu.getTrim();
            if (state.equals(this.rowState.get(r))) continue;
            this.rowState.set(r, state);

            // De derecha a izquierda: ★ (si hay Giga), ⇪ (si hay un libro normal), − y el botón principal.
            int right = this.leftPos + LIST_X + LIST_W;
            gigaButton.visible = giga.isPresent();
            if (giga.isPresent()) right -= GIGA_W + 1;
            Button extract = this.extractButtons.get(r);
            extract.visible = plainBook && level > 0;
            if (extract.visible) {
                right -= SMALL_W + 1;
                extract.setX(right + 1);
                int extractCost = this.menu.extractCostFor(level);
                extract.active = points >= extractCost;
                extract.setTooltip(Tooltip.create(Component.translatable("gui.ascension.extract.tooltip",
                        Enchantment.getFullname(holder, level), extractCost, points)));
            }
            Button down = this.downButtons.get(r);
            down.visible = level > 0;
            if (down.visible) {
                right -= SMALL_W + 1;
                down.setX(right + 1);
                down.setTooltip(Tooltip.create(Component.translatable(level > 1 ? "gui.ascension.refund.tooltip" : "gui.ascension.refund.tooltip_remove",
                        Enchantment.getFullname(holder, level), this.menu.refundFor(level))));
            }
            button.setWidth(right - (this.leftPos + LIST_X));
            if (giga.isPresent()) {
                boolean levelOk = level >= holder.value().getMaxLevel();
                gigaButton.active = levelOk && catalyst;
                MutableComponent tip = Component.translatable("gui.ascension.giga.title",
                        Enchantment.getFullname(giga.get(), level));
                tip.append(Component.literal("\n")).append(Component.translatable(levelOk
                        ? "gui.ascension.giga.level_ok" : "gui.ascension.giga.level_missing",
                        Enchantment.getFullname(holder, holder.value().getMaxLevel())));
                tip.append(Component.literal("\n")).append(Component.translatable(catalyst
                        ? "gui.ascension.giga.star_ok" : "gui.ascension.giga.star_missing"));
                gigaButton.setTooltip(Tooltip.create(tip));
            }

            if (cost == AscensionMenu.COST_MAXED) {
                button.setMessage(Enchantment.getFullname(holder, level).copy()
                        .append(Component.translatable("gui.ascension.maxed")));
                button.active = false;
                button.setTooltip(Tooltip.create(Component.translatable("gui.ascension.tooltip.maxed")));
            } else {
                MutableComponent message = Enchantment.getFullname(holder, level + 1).copy()
                        .append(Component.literal(" · " + cost));
                button.setMessage(message);
                button.active = points >= cost;
                MutableComponent tip = Enchantment.getFullname(holder, level).copy()
                        .append(Component.literal(" → "))
                        .append(Enchantment.getFullname(holder, level + 1))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.ascension.tooltip.cost", cost, points));
                if (points < cost) {
                    tip.append(Component.literal("\n")).append(Component.translatable("gui.ascension.tooltip.missing", cost - points));
                }
                button.setTooltip(Tooltip.create(tip));
            }
        }

        this.upButton.visible = maxScroll > 0;
        this.downButton.visible = maxScroll > 0;
        this.upButton.active = this.scroll > 0;
        this.downButton.active = this.scroll < maxScroll;
    }

    private void updateApplyButton(int points) {
        ItemStack book = this.menu.getBook();
        int cost = this.menu.getApplyCost();
        boolean enchantedBook = book.is(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        this.applyButton.visible = enchantedBook;
        if (!enchantedBook) return;
        this.applyButton.active = cost >= 0 && points >= cost;
        this.applyButton.setTooltip(Tooltip.create(cost < 0
                ? Component.translatable("gui.ascension.apply.nothing")
                : Component.translatable("gui.ascension.apply.tooltip", cost, points)));
    }

    /** Costo de lo que está bajo el mouse (fila, extraer, aplicar), para marcar qué materiales se gastarían. -1 = nada. */
    private int hoveredCost(int mouseX, int mouseY) {
        if (this.applyButton.visible && this.applyButton.active && this.applyButton.isMouseOver(mouseX, mouseY)) {
            return this.menu.getApplyCost();
        }
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(this.menu.getTarget());
        for (int r = 0; r < ROWS; r++) {
            int index = this.scroll + r;
            if (index >= entries.size()) break;
            Button main = this.rowButtons.get(r);
            if (main.visible && main.active && main.isMouseOver(mouseX, mouseY)) return this.menu.getCost(index);
            Button extract = this.extractButtons.get(r);
            if (extract.visible && extract.active && extract.isMouseOver(mouseX, mouseY)) {
                return this.menu.extractCostFor(UpgradeLogic.currentLevel(this.menu.getTarget(), entries.get(index)));
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------------
    // Dibujo
    // ---------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        int w = this.imageWidth;
        int h = this.imageHeight;

        // Panel arcano: borde oscuro, filete dorado y relieve violeta.
        graphics.fill(x, y, x + w, y + h, EDGE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, GOLD);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL);
        graphics.fill(x + 2, y + 2, x + w - 3, y + 3, PANEL_LIGHT);
        graphics.fill(x + 2, y + 2, x + 3, y + h - 3, PANEL_LIGHT);
        graphics.fill(x + 3, y + h - 3, x + w - 2, y + h - 2, PANEL_DARK);
        graphics.fill(x + w - 3, y + 3, x + w - 2, y + h - 2, PANEL_DARK);
        // Esquinas doradas
        for (int[] c : new int[][] {{x + 2, y + 2}, {x + w - 6, y + 2}, {x + 2, y + h - 6}, {x + w - 6, y + h - 6}}) {
            graphics.fill(c[0], c[1], c[0] + 4, c[1] + 4, GOLD_LIGHT);
            graphics.fill(c[0] + 1, c[1] + 1, c[0] + 3, c[1] + 3, GOLD);
        }

        // Línea dorada bajo el título y separador sobre el inventario
        graphics.fill(x + 7, y + 15, x + 62, y + 16, GOLD);
        graphics.fill(x + 8, y + 16, x + 62, y + 17, PANEL_DARK);
        graphics.fill(x + 8, y + 89, x + w - 8, y + 90, PANEL_DARK);

        // Fondo de la lista con marco dorado
        int lx0 = x + LIST_X - 2, ly0 = y + LIST_Y - 2, lx1 = x + LIST_X + LIST_W + 13, ly1 = y + LIST_Y + ROWS * ROW_H + 1;
        graphics.fill(lx0, ly0, lx1, ly1, GOLD);
        graphics.fill(lx0 + 1, ly0 + 1, lx1 - 1, ly1 - 1, LIST_BG);

        // Marcos de los slots
        for (Slot slot : this.menu.slots) {
            drawSlotFrame(graphics, x + slot.x, y + slot.y, slot.index == AscensionMenu.TARGET_SLOT);
        }
    }

    private static void drawSlotFrame(GuiGraphicsExtractor graphics, int sx, int sy, boolean highlight) {
        if (highlight) graphics.fill(sx - 2, sy - 2, sx + 18, sy + 18, GOLD);
        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, highlight ? GOLD_LIGHT : SLOT_BORDER);
        graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_INNER);
        graphics.fill(sx, sy + 15, sx + 16, sy + 16, 0xFF241640);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Título e inventario a mano, en colores claros (los de vanilla son gris oscuro y no se leen sobre violeta).
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, GOLD_LIGHT, true);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_COLOR, false);

        // Pistas en los slots vacíos: "objeto" y "libro".
        if (this.menu.getTarget().isEmpty()) graphics.text(this.font, Component.literal("⚔"), 12, 26, 0x60D8C8F0, false);
        if (this.menu.getBook().isEmpty()) graphics.text(this.font, Component.literal("▤"), 29, 26, 0x60D8C8F0, false);
        graphics.text(this.font, Component.translatable("gui.ascension.materials"), 8, 44, TEXT_COLOR, false);
        graphics.text(this.font, Component.translatable("gui.ascension.points", this.menu.getPoints() >= AscensionMenu.SYNC_CAP ? AscensionMenu.SYNC_CAP + "+" : String.valueOf(this.menu.getPoints())), 8, 76, POINTS_COLOR, false);

        // Bonificación del borde de la mesa, arriba a la derecha, y estanterías a su izquierda.
        TableTrim trim = this.menu.getTrim();
        int labelRight = this.imageWidth - 8;
        if (trim != null) {
            Component label = Component.translatable("gui.ascension.trim." + trim.getSerializedName());
            this.trimTextW = this.font.width(label);
            this.trimTextX = labelRight - this.trimTextW;
            graphics.text(this.font, label, this.trimTextX, this.titleLabelY, trimColor(trim), false);
            labelRight = this.trimTextX - 6;
        } else {
            this.trimTextX = -1;
        }
        int shelves = this.menu.getShelves();
        if (shelves > 0) {
            Component shelfLabel = Component.translatable("gui.ascension.shelves", Math.min(shelves, AscensionMenu.MAX_SHELVES));
            this.shelfTextW = this.font.width(shelfLabel);
            this.shelfTextX = labelRight - this.shelfTextW;
            graphics.text(this.font, shelfLabel, this.shelfTextX, this.titleLabelY, 0xFFC8A06A, false);
        } else {
            this.shelfTextX = -1;
        }

        ItemStack target = this.menu.getTarget();
        Component empty = null;
        if (target.isEmpty()) {
            empty = Component.translatable("gui.ascension.empty");
        } else if (UpgradeLogic.upgradeable(target).isEmpty()) {
            empty = Component.translatable("gui.ascension.nothing");
        }
        if (empty != null) {
            List<FormattedCharSequence> lines = this.font.split(empty, LIST_W - 6);
            int ty = LIST_Y + 20;
            for (FormattedCharSequence line : lines) {
                graphics.text(this.font, line, LIST_X + 4, ty, TEXT_COLOR, false);
                ty += 10;
            }
        }
    }

    private static int trimColor(TableTrim trim) {
        return switch (trim) {
            case DIAMOND -> 0xFF6FE8E0;
            case IRON -> 0xFFD8D8D8;
            case GOLD -> 0xFFFFD84A;
            case COPPER -> 0xFFE38D5A;
            case AMETHYST -> 0xFFC79BFF;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        // Tooltip con la descripción completa al pasar el mouse sobre la bonificación del borde.
        TableTrim trim = this.menu.getTrim();
        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;
        if (trim != null && this.trimTextX >= 0 && rx >= this.trimTextX && rx < this.trimTextX + this.trimTextW
                && ry >= this.titleLabelY - 1 && ry < this.titleLabelY + 9) {
            graphics.setTooltipForNextFrame(this.font,
                    this.font.split(Component.translatable("gui.ascension.trim." + trim.getSerializedName() + ".desc"), 180),
                    mouseX, mouseY);
        }
        if (this.shelfTextX >= 0 && rx >= this.shelfTextX && rx < this.shelfTextX + this.shelfTextW
                && ry >= this.titleLabelY - 1 && ry < this.titleLabelY + 9) {
            graphics.setTooltipForNextFrame(this.font,
                    this.font.split(Component.translatable("gui.ascension.shelves.desc", Math.min(this.menu.getShelves(), AscensionMenu.MAX_SHELVES)), 180),
                    mouseX, mouseY);
        }
        // Puntos: detalle de cuántos son puntos guardados en la mesa.
        if (rx >= 8 && rx < 62 && ry >= 75 && ry < 85 && this.menu.getCredit() > 0) {
            graphics.setTooltipForNextFrame(this.font,
                    this.font.split(Component.translatable("gui.ascension.credit", this.menu.getCredit()), 180), mouseX, mouseY);
        }

        // Materiales que se gastarían con el botón bajo el mouse: tinte rojo y cantidad.
        int cost = this.hoveredCost(mouseX, mouseY);
        if (cost > 0) {
            int[] take = this.menu.previewConsumption(cost);
            for (int i = 0; i < take.length; i++) {
                if (take[i] <= 0) continue;
                Slot slot = this.menu.slots.get(1 + i);
                int sx = this.leftPos + slot.x;
                int sy = this.topPos + slot.y;
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x80FF3030);
                String text = "-" + take[i];
                graphics.text(this.font, Component.literal(text), sx + 17 - this.font.width(text), sy - 7, 0xFFFF6060, true);
            }
        }
    }
}
