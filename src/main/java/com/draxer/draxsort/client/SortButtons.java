package com.draxer.draxsort.client;

import com.draxer.draxsort.DepositPayload;
import com.draxer.draxsort.DraxSort;
import com.draxer.draxsort.NearbySearch;
import com.draxer.draxsort.QuickMovePayload;
import com.draxer.draxsort.Restocker;
import com.draxer.draxsort.SortConfig;
import com.draxer.draxsort.SortMode;
import com.draxer.draxsort.SortPayload;
import com.draxer.draxsort.Sorter;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Todo lo del lado del cliente:
 *  - Botones al costado de las pantallas con inventario: ⇅ ordenar, ⇩ depositar, ◆/A/# criterio, ≡/‖ filas/columnas.
 *  - Barra de búsqueda sobre el panel: resalta los slots que coinciden y oscurece el resto.
 *  - Alt + clic en un slot del inventario: lo bloquea (candado rojo). Los bloqueados no se ordenan, no se
 *    depositan, y no se mueven con Shift + clic ni se tiran con Q.
 *  - Clic central sobre un slot: ordena su sección (fuera de creativo).
 *  - Tecla Z (configurable): depositar en cofres cercanos, dentro o fuera de un inventario.
 *  - Tecla R (configurable): rellenar la barra rápida desde cofres cercanos.
 *  - Rueda sobre un slot: mandar / traer pilas. Shift + arrastrar: mover varias pilas.
 *  - ▣ plantilla: guarda dónde va cada ítem del inventario y el orden lo respeta.
 */
@EventBusSubscriber(modid = DraxSort.MODID, value = Dist.CLIENT)
public final class SortButtons {
    private static final int SIZE = 12;
    private static final int GAP = 13;
    private static final int KEY_ESCAPE = 256;

    public static final KeyMapping DEPOSIT_KEY = new KeyMapping("key.drax_sort.deposit", InputConstants.KEY_Z, KeyMapping.Category.MISC);
    public static final KeyMapping RESTOCK_KEY = new KeyMapping("key.drax_sort.restock", InputConstants.KEY_R, KeyMapping.Category.MISC);

    /**
     * Botón colocado. Si {@code inside} es true, offsetX/offsetY son coordenadas dentro del panel
     * (inventario del jugador, a la derecha del libro de recetas); si no, va al costado derecho junto a {@code anchor}.
     */
    private record Placed(Button button, Slot anchor, int offsetX, int offsetY, boolean inside) {}

    /** Posición de la fila de botones dentro del inventario del jugador (a la derecha del libro de recetas). */
    private static final int INSIDE_X = 125;
    private static final int INSIDE_Y = 64;
    private static final int INSIDE_STEP = 12;

    private static Screen currentScreen;
    private static final List<Placed> PLACED = new ArrayList<>();
    private static EditBox searchBox;
    /** El texto de búsqueda se mantiene entre pantallas durante la sesión. */
    private static String searchText = "";

    private SortButtons() {}

    // =====================================================================
    // Registro de la tecla
    // =====================================================================

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DEPOSIT_KEY);
        event.register(RESTOCK_KEY);
    }

    /** Tecla de depósito fuera de pantallas (en el mundo). */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (DEPOSIT_KEY.consumeClick()) {
            requestDeposit();
        }
        while (RESTOCK_KEY.consumeClick()) {
            requestRestock();
        }
        // Cofres cercanos encontrados por la búsqueda: brillan en dorado unos segundos.
        if (++highlightTicks % 3 == 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                for (BlockPos pos : NearbySearch.Highlights.active()) {
                    for (int i = 0; i < 4; i++) {
                        double x = pos.getX() + minecraft.level.getRandom().nextDouble();
                        double y = pos.getY() + 0.2D + minecraft.level.getRandom().nextDouble() * 1.0D;
                        double z = pos.getZ() + minecraft.level.getRandom().nextDouble();
                        minecraft.level.addParticle(HIGHLIGHT, x, y, z, 0.0D, 0.02D, 0.0D);
                    }
                }
            }
        }
    }

    private static final DustParticleOptions HIGHLIGHT = new DustParticleOptions(0xFFC94A, 1.0F);
    private static int highlightTicks = 0;

    // =====================================================================
    // Construcción de la interfaz
    // =====================================================================

    private static boolean isSortableScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen);
    }

    @SubscribeEvent
    static void onScreenInit(ScreenEvent.Init.Post event) {
        PLACED.clear();
        currentScreen = null;
        searchBox = null;
        if (!isSortableScreen(event.getScreen())) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        AbstractContainerMenu menu = screen.getMenu();

        Slot containerSlot = Sorter.firstContainerSlot(menu);
        Slot playerSlot = Sorter.firstPlayerSlot(menu);
        if (containerSlot == null && playerSlot == null) return;
        currentScreen = screen;

        Slot settingsAnchor = containerSlot != null ? containerSlot : playerSlot;

        // Inventario del jugador (tecla E): los cuatro botones en fila dentro del panel, junto al libro de recetas.
        if (screen instanceof InventoryScreen && playerSlot != null) {
            Button deposit = Button.builder(Component.literal("⇩"), b -> requestDeposit())
                    .bounds(0, 0, SIZE, SIZE)
                    .tooltip(Tooltip.create(Component.translatable("drax_sort.button.deposit", DEPOSIT_KEY.getTranslatedKeyMessage())))
                    .build();
            Button[] row = {
                    sortButton(screen, playerSlot, "drax_sort.button.sort_inventory"),
                    deposit, modeButton(), orientationButton()};
            for (int i = 0; i < row.length; i++) {
                placeInside(event, screen, row[i], playerSlot, INSIDE_X + i * INSIDE_STEP, INSIDE_Y);
            }
            // Segunda fila, encima de la última parte de la primera: rellenar y plantilla.
            placeInside(event, screen, restockButton(), playerSlot, INSIDE_X + 2 * INSIDE_STEP, INSIDE_Y - INSIDE_STEP - 1);
            placeInside(event, screen, templateButton(), playerSlot, INSIDE_X + 3 * INSIDE_STEP, INSIDE_Y - INSIDE_STEP - 1);
            addSearchBox(event, screen);
            return;
        }

        if (containerSlot != null) {
            place(event, screen, sortButton(screen, containerSlot, "drax_sort.button.sort_container"), containerSlot, 0, 0);
        }
        if (playerSlot != null) {
            place(event, screen, sortButton(screen, playerSlot, "drax_sort.button.sort_inventory"), playerSlot, 0, 0);
            Button deposit = Button.builder(Component.literal("⇩"), b -> requestDeposit())
                    .bounds(0, 0, SIZE, SIZE)
                    .tooltip(Tooltip.create(Component.translatable("drax_sort.button.deposit", DEPOSIT_KEY.getTranslatedKeyMessage())))
                    .build();
            place(event, screen, deposit, playerSlot, GAP, 0);
            place(event, screen, restockButton(), playerSlot, GAP, GAP);
            place(event, screen, templateButton(), playerSlot, GAP, 2 * GAP);
        }

        place(event, screen, modeButton(), settingsAnchor, 0, GAP);
        place(event, screen, orientationButton(), settingsAnchor, 0, 2 * GAP);
        addSearchBox(event, screen);
    }

    private static Button restockButton() {
        return Button.builder(Component.literal("⇧"), b -> requestRestock())
                .bounds(0, 0, SIZE, SIZE)
                .tooltip(Tooltip.create(Component.translatable("drax_sort.button.restock", RESTOCK_KEY.getTranslatedKeyMessage())))
                .build();
    }

    /** 📌 Plantilla: clic = guardar cómo está ordenado el inventario ahora; Shift + clic = borrarla. */
    private static Button templateButton() {
        return Button.builder(Component.literal("▣"), b -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (minecraft.hasShiftDown()) {
                SortConfig.setTemplate(List.of());
                minecraft.player.sendOverlayMessage(Component.translatable("drax_sort.template.cleared"));
            } else {
                List<String> entries = new ArrayList<>();
                Inventory inventory = minecraft.player.getInventory();
                for (int i = 9; i < 36; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) entries.add(i + "=" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
                SortConfig.setTemplate(entries);
                minecraft.player.sendOverlayMessage(Component.translatable("drax_sort.template.saved", entries.size()));
            }
            b.setTooltip(templateTooltip());
        }).bounds(0, 0, SIZE, SIZE).tooltip(templateTooltip()).build();
    }

    private static Tooltip templateTooltip() {
        int size = SortConfig.template().size();
        return Tooltip.create(size == 0
                ? Component.translatable("drax_sort.button.template.empty")
                : Component.translatable("drax_sort.button.template.active", size));
    }

    private static Button modeButton() {
        return Button.builder(modeLabel(), b -> {
            SortConfig.setMode(SortConfig.mode().next());
            b.setMessage(modeLabel());
            b.setTooltip(modeTooltip());
        }).bounds(0, 0, SIZE, SIZE).tooltip(modeTooltip()).build();
    }

    private static Button orientationButton() {
        return Button.builder(orientationLabel(), b -> {
            SortConfig.setColumns(!SortConfig.columns());
            b.setMessage(orientationLabel());
            b.setTooltip(orientationTooltip());
        }).bounds(0, 0, SIZE, SIZE).tooltip(orientationTooltip()).build();
    }

    /** Barra de búsqueda encima del panel. */
    private static void addSearchBox(ScreenEvent.Init.Post event, AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        searchBox = new EditBox(minecraft.font, 0, 0, Math.min(120, screen.getImageWidth()), 12, Component.translatable("drax_sort.search"));
        searchBox.setMaxLength(50);
        searchBox.setHint(Component.translatable("drax_sort.search.hint"));
        searchBox.setValue(searchText);
        searchBox.setResponder(text -> searchText = text);
        repositionSearch(screen);
        event.addListener(searchBox);
    }

    private static Button sortButton(AbstractContainerScreen<?> screen, Slot anchor, String tooltipKey) {
        return Button.builder(Component.literal("⇅"), b -> requestSort(screen.getMenu(), anchor))
                .bounds(0, 0, SIZE, SIZE)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                .build();
    }

    private static void place(ScreenEvent.Init.Post event, AbstractContainerScreen<?> screen, Button button, Slot anchor, int offsetX, int offsetY) {
        Placed placed = new Placed(button, anchor, offsetX, offsetY, false);
        PLACED.add(placed);
        reposition(screen, placed);
        event.addListener(button);
    }

    private static void placeInside(ScreenEvent.Init.Post event, AbstractContainerScreen<?> screen, Button button, Slot anchor, int x, int y) {
        Placed placed = new Placed(button, anchor, x, y, true);
        PLACED.add(placed);
        reposition(screen, placed);
        event.addListener(button);
    }

    private static void reposition(AbstractContainerScreen<?> screen, Placed placed) {
        if (placed.inside()) {
            // Se recalcula cada frame: al abrir el libro de recetas el panel se corre y los botones lo siguen.
            placed.button().setX(screen.getLeftPos() + placed.offsetX());
            placed.button().setY(screen.getTopPos() + placed.offsetY());
            return;
        }
        placed.button().setX(screen.getLeftPos() + screen.getImageWidth() + 2 + placed.offsetX());
        placed.button().setY(screen.getTopPos() + placed.anchor().y - 1 + placed.offsetY());
    }

    private static void repositionSearch(AbstractContainerScreen<?> screen) {
        if (searchBox == null) return;
        searchBox.setX(screen.getLeftPos());
        searchBox.setY(screen.getTopPos() - 14);
    }

    // =====================================================================
    // Dibujo: candados y resaltado de búsqueda
    // =====================================================================

    @SubscribeEvent
    static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() != currentScreen || !(currentScreen instanceof AbstractContainerScreen<?> screen)) return;
        for (Placed placed : PLACED) reposition(screen, placed);
        repositionSearch(screen);
    }

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() != currentScreen || !(currentScreen instanceof AbstractContainerScreen<?> screen)) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int left = screen.getLeftPos();
        int top = screen.getTopPos();
        String query = searchText.trim().toLowerCase(Locale.ROOT);

        for (Slot slot : screen.getMenu().slots) {
            int x = left + slot.x;
            int y = top + slot.y;

            if (!query.isEmpty()) {
                if (slot.hasItem() && matches(slot.getItem(), query)) {
                    // Marco dorado
                    graphics.fill(x - 1, y - 1, x + 17, y, 0xFFFFC94A);
                    graphics.fill(x - 1, y + 16, x + 17, y + 17, 0xFFFFC94A);
                    graphics.fill(x - 1, y, x, y + 16, 0xFFFFC94A);
                    graphics.fill(x + 16, y, x + 17, y + 16, 0xFFFFC94A);
                } else {
                    graphics.fill(x, y, x + 16, y + 16, 0xB0101010);
                }
            }

            if (isLockedSlot(slot)) {
                graphics.fill(x, y, x + 16, y + 16, 0x40FF3030);
                // Candado pequeño en la esquina
                graphics.fill(x + 11, y + 1, x + 15, y + 2, 0xFFB0B0B0);
                graphics.fill(x + 11, y + 1, x + 12, y + 4, 0xFFB0B0B0);
                graphics.fill(x + 14, y + 1, x + 15, y + 4, 0xFFB0B0B0);
                graphics.fill(x + 10, y + 4, x + 16, y + 8, 0xFFE0B030);
            }
        }
    }

    private static boolean matches(ItemStack stack, String query) {
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(query);
    }

    private static boolean isLockedSlot(Slot slot) {
        return slot.container instanceof Inventory && SortConfig.isLocked(slot.getContainerSlot());
    }

    // =====================================================================
    // Entrada: clics y teclas
    // =====================================================================

    @SubscribeEvent
    static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isSortableScreen(event.getScreen())) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();

        // Clic fuera de la barra de búsqueda: le quita el foco para que E vuelva a cerrar el inventario.
        if (searchBox != null && searchBox.isFocused()
                && !searchBox.isMouseOver(event.getMouseX(), event.getMouseY())) {
            searchBox.setFocused(false);
            screen.setFocused(null);
        }

        Slot hovered = screen.getHoveredSlot();
        if (hovered == null) return;
        int button = event.getButton();

        // Alt + clic izquierdo: bloquear / desbloquear slot del inventario.
        if (button == 0 && event.getMouseButtonEvent().hasAltDown() && hovered.container instanceof Inventory
                && hovered.getContainerSlot() < 36) {
            SortConfig.toggleLocked(hovered.getContainerSlot());
            event.setCanceled(true);
            return;
        }

        // Shift + clic sobre un slot bloqueado: no se mueve.
        if (isLockedSlot(hovered) && event.getMouseButtonEvent().hasShiftDown()) {
            event.setCanceled(true);
            return;
        }

        // Clic central: ordenar la sección.
        if (button == 2 && SortConfig.middleClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.player.isCreative()) return; // en creativo el clic central copia ítems
            if (Sorter.sectionOf(screen.getMenu(), hovered).isEmpty()) return;
            requestSort(screen.getMenu(), hovered);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!isSortableScreen(event.getScreen())) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        int key = event.getKeyCode();

        // Mientras se escribe en la búsqueda, las teclas van a la barra (E no cierra el inventario).
        if (searchBox != null && searchBox.isFocused()) {
            if (key == KEY_ESCAPE) return;
            // Enter: buscar también en los cofres cercanos.
            if ((key == 257 || key == 335) && !searchText.isBlank()) {
                ClientPacketDistributor.sendToServer(new NearbySearch.Query(searchText.trim()));
                event.setCanceled(true);
                return;
            }
            searchBox.keyPressed(event.getKeyEvent());
            event.setCanceled(true);
            return;
        }

        // Q sobre un slot bloqueado: no se tira.
        Slot hovered = screen.getHoveredSlot();
        Minecraft minecraft = Minecraft.getInstance();
        if (hovered != null && isLockedSlot(hovered) && key == minecraft.options.keyDrop.getKey().getValue()) {
            event.setCanceled(true);
            return;
        }

        // Tecla de rellenar dentro de un inventario.
        if (key == RESTOCK_KEY.getKey().getValue()) {
            requestRestock();
            event.setCanceled(true);
            return;
        }

        // Tecla de depósito dentro de un inventario.
        if (key == DEPOSIT_KEY.getKey().getValue()) {
            requestDeposit();
            event.setCanceled(true);
        }
    }

    // =====================================================================
    // Paquetes
    // =====================================================================

    private static void requestSort(AbstractContainerMenu menu, Slot anchor) {
        // La plantilla solo se aplica al ordenar el inventario del jugador.
        List<String> template = Sorter.isPlayerMainSlot(anchor) ? SortConfig.template() : List.of();
        ClientPacketDistributor.sendToServer(new SortPayload(
                menu.containerId, anchor.index, SortConfig.mode().ordinal(), SortConfig.columns(), SortConfig.lockedMask(), template));
    }

    private static void requestRestock() {
        if (Minecraft.getInstance().player == null) return;
        ClientPacketDistributor.sendToServer(Restocker.Request.INSTANCE);
    }

    // =====================================================================
    // Rueda del mouse y Shift + arrastrar
    // =====================================================================

    /** Slots ya movidos durante el arrastre actual con Shift. */
    private static final java.util.Set<Integer> DRAGGED = new java.util.HashSet<>();

    /**
     * Rueda sobre un slot: hacia abajo manda esa pila al otro lado (como Shift + clic);
     * hacia arriba trae una pila igual desde el otro lado.
     */
    @SubscribeEvent
    static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!isSortableScreen(event.getScreen()) || !SortConfig.scrollMove()) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        if (searchBox != null && searchBox.isMouseOver(event.getMouseX(), event.getMouseY())) return;
        Slot hovered = screen.getHoveredSlot();
        if (hovered == null || !hovered.hasItem()) return;
        double delta = event.getScrollDeltaY();
        if (delta == 0.0D) return;

        if (delta < 0) {
            if (!isLockedSlot(hovered) && isMovable(screen.getMenu(), hovered)) quickMove(screen, hovered);
        } else {
            Slot source = findOtherSide(screen.getMenu(), hovered);
            if (source != null) quickMove(screen, source);
        }
        event.setCanceled(true);
    }

    /** Shift + arrastrar con clic izquierdo: mueve cada pila por la que pasas, como si hicieras Shift + clic en cada una. */
    @SubscribeEvent
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!isSortableScreen(event.getScreen()) || event.getMouseButton() != 0) return;
        if (!event.getMouseButtonEvent().hasShiftDown()) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        if (!screen.getMenu().getCarried().isEmpty()) return;
        Slot hovered = screen.getHoveredSlot();
        if (hovered == null || !hovered.hasItem() || isLockedSlot(hovered) || !isMovable(screen.getMenu(), hovered)) return;
        if (DRAGGED.add(hovered.index)) quickMove(screen, hovered);
    }

    @SubscribeEvent
    static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        DRAGGED.clear();
    }

    @SubscribeEvent
    static void onMousePressedForDrag(ScreenEvent.MouseButtonPressed.Post event) {
        // El primer slot ya lo movió el Shift + clic normal: que el arrastre no lo repita.
        DRAGGED.clear();
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen && screen.getHoveredSlot() != null) {
            DRAGGED.add(screen.getHoveredSlot().index);
        }
    }

    private static void quickMove(AbstractContainerScreen<?> screen, Slot slot) {
        if (Minecraft.getInstance().player == null) return;
        ClientPacketDistributor.sendToServer(new QuickMovePayload(screen.getMenu().containerId, slot.index));
    }

    /** Solo el inventario del jugador (barra o principal) y contenedores ordenables: nada de casillas de resultado. */
    private static boolean isMovable(AbstractContainerMenu menu, Slot slot) {
        if (slot.container instanceof Inventory) return slot.getContainerSlot() < 36;
        return !Sorter.sectionOf(menu, slot).isEmpty();
    }

    private static int side(Slot slot) {
        if (slot.container instanceof Inventory) return slot.getContainerSlot() < 9 ? 1 : 2; // 1 barra, 2 principal
        return 3 + System.identityHashCode(slot.container);
    }

    private static Slot findOtherSide(AbstractContainerMenu menu, Slot hovered) {
        int mySide = side(hovered);
        boolean hoveredIsPlayer = hovered.container instanceof Inventory;
        Slot fallback = null;
        for (Slot slot : menu.slots) {
            if (!slot.hasItem() || !isMovable(menu, slot) || isLockedSlot(slot)) continue;
            if (side(slot) == mySide || !ItemStack.isSameItemSameComponents(slot.getItem(), hovered.getItem())) continue;
            boolean slotIsPlayer = slot.container instanceof Inventory;
            // Preferir el otro "lado" de verdad: cofre <-> jugador antes que barra <-> principal.
            if (slotIsPlayer != hoveredIsPlayer) return slot;
            if (fallback == null) fallback = slot;
        }
        return fallback;
    }

    private static void requestDeposit() {
        if (Minecraft.getInstance().player == null) return;
        ClientPacketDistributor.sendToServer(new DepositPayload(SortConfig.lockedMask()));
    }

    // =====================================================================
    // Textos
    // =====================================================================

    private static Component modeLabel() {
        return Component.literal(switch (SortConfig.mode()) {
            case CATEGORY -> "◆";
            case NAME -> "A";
            case COUNT -> "#";
        });
    }

    private static Tooltip modeTooltip() {
        SortMode mode = SortConfig.mode();
        return Tooltip.create(Component.translatable("drax_sort.button.mode",
                Component.translatable("drax_sort.mode." + mode.name().toLowerCase(Locale.ROOT))));
    }

    private static Component orientationLabel() {
        return Component.literal(SortConfig.columns() ? "‖" : "≡");
    }

    private static Tooltip orientationTooltip() {
        return Tooltip.create(Component.translatable(SortConfig.columns()
                ? "drax_sort.button.columns" : "drax_sort.button.rows"));
    }
}
