package com.draxer.ascension.menu;

import com.draxer.ascension.AscensionConfig;
import com.draxer.ascension.AscensionMod;
import com.draxer.ascension.UpgradeLogic;
import com.draxer.ascension.block.AscensionTableBlock;
import com.draxer.ascension.block.AscensionTableBlockEntity;
import com.draxer.ascension.block.TableTrim;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Menú de la Mesa de Ascensión.
 * Slots: 0 = ítem a mejorar, 1-3 = materiales, 4 = libro, 5-31 = inventario, 32-40 = barra rápida.
 * Datos sincronizados: [0] = puntos disponibles (materiales + guardados), [1 + i] = costo del encantamiento i
 * (COST_MAXED si ya está al máximo), [1 + MAX_ENTRIES] = borde de la mesa (ordinal + 1; 0 = ninguno),
 * [2 + MAX_ENTRIES] = puntos guardados en la mesa, [3 + MAX_ENTRIES] = estanterías,
 * [4 + MAX_ENTRIES] = costo de aplicar el libro (-1 si no se puede).
 *
 * Botones: i = subir, 1000 + i = Giga, 2000 + i = bajar un nivel, 3000 + i = extraer a libro, 4000 = aplicar libro.
 * Estanterías alrededor (como la mesa vanilla): −1 % de costo cada una, hasta −15 %.
 *
 * Cada mineral del borde da una bonificación:
 * oro = costos −15 %, cobre = materiales valen +25 %, amatista = 20 % de no gastar materiales,
 * hierro = 15 % de subir 2 niveles, diamante = 25 % de no gastar la Estrella del Nether al convertir a Giga.
 */
public class AscensionMenu extends AbstractContainerMenu {
    public static final int TARGET_SLOT = 0;
    public static final int MATERIAL_SLOTS = 3;
    public static final int MAX_ENTRIES = 32;
    public static final int COST_MAXED = -1;
    /** Los botones de conversión a Giga envían 1000 + índice del encantamiento. */
    public static final int GIGA_BUTTON_OFFSET = 1000;
    public static final int DOWNGRADE_BUTTON_OFFSET = 2000;
    public static final int EXTRACT_BUTTON_OFFSET = 3000;
    public static final int APPLY_BOOK_BUTTON = 4000;
    public static final int BOOK_SLOT = 1 + 3; // después de los materiales
    public static final double REFUND_FRACTION = 0.5D;
    public static final double EXTRACT_FRACTION = 0.25D;
    public static final int MAX_SHELVES = 15;
    /** X del inventario del jugador (centrado en una ventana de 222 px de ancho). */
    public static final int INV_X = 31;
    private static final int TRIM_DATA = 1 + MAX_ENTRIES;
    private static final int CREDIT_DATA = 2 + MAX_ENTRIES;
    private static final int SHELF_DATA = 3 + MAX_ENTRIES;
    private static final int APPLY_DATA = 4 + MAX_ENTRIES;
    /** Máximo que se puede sincronizar con seguridad en un slot de datos (short). */
    public static final int SYNC_CAP = 32000;

    public static final double GOLD_COST_FACTOR = 0.85D;
    public static final double COPPER_POINTS_FACTOR = 1.25D;
    public static final float AMETHYST_SAVE_CHANCE = 0.20F;
    public static final float IRON_DOUBLE_CHANCE = 0.15F;
    public static final float DIAMOND_STAR_CHANCE = 0.25F;

    private static final int INV_START = 2 + MATERIAL_SLOTS;   // 5
    private static final int HOTBAR_START = INV_START + 27;     // 32
    private static final int HOTBAR_END = HOTBAR_START + 9;     // 41 (exclusivo)

    private final ContainerLevelAccess access;
    private final Player player;
    private final ContainerData data;
    private final Container container = new SimpleContainer(2 + MATERIAL_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            AscensionMenu.this.slotsChanged(this);
        }
    };

    /** Constructor del cliente. */
    public AscensionMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    /** Constructor del servidor. */
    public AscensionMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(AscensionMod.ASCENSION_MENU.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        this.data = new SimpleContainerData(5 + MAX_ENTRIES);

        this.addSlot(new Slot(this.container, TARGET_SLOT, 8, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return UpgradeLogic.isUpgradeTarget(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            this.addSlot(new Slot(this.container, 1 + i, 8 + i * 18, 54) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return UpgradeLogic.pointsPerItem(stack) > 0;
                }
            });
        }

        // Libro: un libro normal (para extraer un encantamiento) o uno encantado (para aplicarlo al ítem).
        this.addSlot(new Slot(this.container, BOOK_SLOT, 26, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, INV_X + col * 18, 162));
        }

        this.addDataSlots(this.data);
        this.refresh();
    }

    // ---------------------------------------------------------------------
    // Datos para la pantalla (cliente)
    // ---------------------------------------------------------------------

    public ItemStack getTarget() {
        return this.slots.get(TARGET_SLOT).getItem();
    }

    public int getPoints() {
        return this.data.get(0);
    }

    public int getCost(int index) {
        if (index < 0 || index >= MAX_ENTRIES) return COST_MAXED;
        return this.data.get(1 + index);
    }

    /** Borde de la mesa abierta (sincronizado desde el servidor), o null si no se sabe. */
    public @Nullable TableTrim getTrim() {
        int value = this.data.get(TRIM_DATA);
        TableTrim[] trims = TableTrim.values();
        return value >= 1 && value <= trims.length ? trims[value - 1] : null;
    }

    public int getCredit() {
        return this.data.get(CREDIT_DATA);
    }

    public int getShelves() {
        return this.data.get(SHELF_DATA);
    }

    /** Costo de aplicar el libro encantado del slot de libro, o -1 si no se puede. */
    public int getApplyCost() {
        return this.data.get(APPLY_DATA);
    }

    public ItemStack getBook() {
        return this.slots.get(BOOK_SLOT).getItem();
    }

    /** Multiplicador de costo por borde de oro y estanterías. Igual en cliente y servidor. */
    public static double discountFactor(@Nullable TableTrim trim, int shelves) {
        double factor = trim == TableTrim.GOLD ? GOLD_COST_FACTOR : 1.0D;
        return factor * (1.0D - 0.01D * Math.min(MAX_SHELVES, Math.max(0, shelves)));
    }

    /** Costo con descuentos del nivel {@code level} (para mostrar devoluciones y extracciones en el cliente). */
    public int discountedCost(int level) {
        return Math.max(1, (int) Math.ceil(UpgradeLogic.costForLevel(level) * discountFactor(this.getTrim(), this.getShelves())));
    }

    public int refundFor(int level) {
        return (int) Math.floor(this.discountedCost(level) * REFUND_FRACTION);
    }

    public int extractCostFor(int level) {
        return Math.max(1, (int) Math.ceil(this.discountedCost(level) * EXTRACT_FRACTION));
    }

    /**
     * Vista previa (cliente): cuántas unidades de cada slot de material se gastarían para pagar {@code cost}.
     * Índice 0 = slot 1 ... Usa primero los puntos guardados, igual que el servidor.
     */
    public int[] previewConsumption(int cost) {
        TableTrim trim = this.getTrim();
        int remaining = Math.max(0, cost - this.getCredit());
        int[] counts = new int[MATERIAL_SLOTS];
        int[] values = new int[MATERIAL_SLOTS];
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = this.slots.get(1 + i).getItem();
            counts[i] = stack.getCount();
            values[i] = itemValue(stack, trim);
        }
        return planConsumption(counts, values, remaining);
    }

    /** ¿Hay al menos una Estrella del Nether en los slots de materiales? */
    public boolean hasGigaCatalyst() {
        for (int i = 1; i <= MATERIAL_SLOTS; i++) {
            if (this.slots.get(i).getItem().is(Items.NETHER_STAR)) return true;
        }
        return false;
    }

    /** Versión Giga del encantamiento, si el mod Giga Enchantments está instalado y la tiene. */
    public Optional<Holder.Reference<Enchantment>> gigaFor(Holder<Enchantment> enchantment) {
        return UpgradeLogic.gigaCounterpart(this.player.level().registryAccess(), enchantment);
    }

    // ---------------------------------------------------------------------
    // Lógica (servidor)
    // ---------------------------------------------------------------------

    private boolean isServer() {
        return !this.player.level().isClientSide();
    }

    /** Lee el borde directamente del bloque (solo servidor). */
    private @Nullable TableTrim readTrim() {
        // evaluate() envuelve el resultado con Optional.of, así que no puede devolver null: se anida otro Optional.
        return this.access.evaluate((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            return state.hasProperty(AscensionTableBlock.TRIM) ? Optional.of(state.getValue(AscensionTableBlock.TRIM)) : Optional.<TableTrim>empty();
        }).flatMap(o -> o).orElse(null);
    }

    /** Estanterías válidas alrededor de la mesa (mismo patrón que la mesa de encantamientos vanilla). Solo servidor. */
    private int readShelves() {
        return this.access.evaluate((level, pos) -> {
            int count = 0;
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) count++;
            }
            return count;
        }).orElse(0);
    }

    private int readCredit() {
        return this.access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof AscensionTableBlockEntity be ? be.credit() : 0)
                .orElse(0);
    }

    private void writeCredit(int credit) {
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof AscensionTableBlockEntity be) be.setCredit(credit);
        });
    }

    /** Costo del nivel {@code nextLevel} con los descuentos de borde de oro y estanterías. Solo servidor. */
    private int effectiveCost(int nextLevel, @Nullable TableTrim trim) {
        return Math.max(1, (int) Math.ceil(UpgradeLogic.costForLevel(nextLevel) * discountFactor(trim, readShelves())));
    }

    /** Puntos disponibles: materiales + puntos guardados en la mesa. */
    private long available(@Nullable TableTrim trim) {
        return (long) totalPoints(trim) + readCredit();
    }

    /** Paga {@code cost}: primero con los puntos guardados y lo que falte con materiales. */
    private void pay(int cost, @Nullable TableTrim trim) {
        int credit = readCredit();
        int fromCredit = Math.min(credit, cost);
        if (fromCredit > 0) writeCredit(credit - fromCredit);
        int rest = cost - fromCredit;
        if (rest > 0) consumeMaterials(rest, trim);
    }

    /** Puntos que vale una unidad del material, teniendo en cuenta el borde de cobre. */
    public static int itemValue(ItemStack stack, @Nullable TableTrim trim) {
        int value = UpgradeLogic.pointsPerItem(stack);
        if (value > 0 && trim == TableTrim.COPPER) value = (int) Math.round(value * COPPER_POINTS_FACTOR);
        return value;
    }

    private void announce(String key, @Nullable TableTrim trim) {
        this.player.sendOverlayMessage(Component.translatable(key).withColor(0xE0B84A));
        this.access.execute((lvl, pos) -> {
            lvl.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 1.5F);
            if (lvl instanceof ServerLevel server) {
                server.sendParticles(trim == TableTrim.AMETHYST ? ParticleTypes.WITCH : ParticleTypes.WAX_OFF,
                        pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D, 20, 0.45D, 0.1D, 0.45D, 0.05D);
            }
        });
    }

    @Override
    public void slotsChanged(Container changed) {
        super.slotsChanged(changed);
        if (changed == this.container) {
            this.refresh();
        }
    }

    /** Recalcula puntos y costos; solo en el servidor (el cliente los recibe sincronizados). */
    private void refresh() {
        if (!isServer()) return;

        TableTrim trim = readTrim();
        this.data.set(TRIM_DATA, trim == null ? 0 : trim.ordinal() + 1);
        this.data.set(SHELF_DATA, readShelves());
        this.data.set(CREDIT_DATA, Math.min(readCredit(), SYNC_CAP));
        // Los datos del menú viajan al cliente como short (máx. 32767): se topa el valor mostrado.
        // El servidor siempre usa el total real al subir niveles.
        this.data.set(0, (int) Math.min(available(trim), SYNC_CAP));
        int applyCost = applyBookCost(trim);
        this.data.set(APPLY_DATA, applyCost < 0 ? -1 : Math.min(applyCost, SYNC_CAP));

        ItemStack target = this.container.getItem(TARGET_SLOT);
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        int maxLevel = AscensionConfig.maxLevel();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            int cost = COST_MAXED;
            if (i < entries.size()) {
                int level = UpgradeLogic.currentLevel(target, entries.get(i));
                if (level < maxLevel) {
                    cost = effectiveCost(level + 1, trim);
                }
            }
            this.data.set(1 + i, cost == COST_MAXED ? COST_MAXED : Math.min(cost, SYNC_CAP));
        }
    }

    private int totalPoints(@Nullable TableTrim trim) {
        long total = 0;
        for (int i = 1; i <= MATERIAL_SLOTS; i++) {
            ItemStack stack = this.container.getItem(i);
            total += (long) itemValue(stack, trim) * stack.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Se llama cuando el cliente pulsa el botón del encantamiento con índice {@code id}. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == APPLY_BOOK_BUTTON) return this.applyBook();
        if (id >= EXTRACT_BUTTON_OFFSET) return this.extract(id - EXTRACT_BUTTON_OFFSET);
        if (id >= DOWNGRADE_BUTTON_OFFSET) return this.downgrade(id - DOWNGRADE_BUTTON_OFFSET);
        if (id >= GIGA_BUTTON_OFFSET) {
            return this.convertToGiga(id - GIGA_BUTTON_OFFSET);
        }
        ItemStack target = this.container.getItem(TARGET_SLOT);
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        if (id < 0 || id >= entries.size()) return false;

        Holder<Enchantment> enchantment = entries.get(id);
        int level = UpgradeLogic.currentLevel(target, enchantment);
        if (level >= AscensionConfig.maxLevel()) return false;

        if (!isServer()) {
            // El cliente no conoce el borde: usa el costo sincronizado.
            int synced = this.getCost(id);
            return synced != COST_MAXED && this.getPoints() >= synced;
        }

        TableTrim trim = readTrim();
        int cost = effectiveCost(level + 1, trim);
        if (available(trim) < cost) return false;

        {
            var random = this.player.getRandom();
            boolean saved = trim == TableTrim.AMETHYST && random.nextFloat() < AMETHYST_SAVE_CHANCE;
            if (!saved) pay(cost, trim);

            int gained = 1;
            if (trim == TableTrim.IRON && level + 2 <= AscensionConfig.maxLevel() && random.nextFloat() < IRON_DOUBLE_CHANCE) {
                gained = 2;
            }
            int newLevel = level + gained;

            ItemStack upgraded = target.copy();
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(upgraded));
            mutable.set(enchantment, newLevel);
            EnchantmentHelper.setEnchantments(upgraded, mutable.toImmutable());
            this.container.setItem(TARGET_SLOT, upgraded);
            if (saved) {
                this.container.setChanged();
                announce("gui.ascension.trim.proc.amethyst", trim);
            }
            if (gained == 2) announce("gui.ascension.trim.proc.iron", trim);

            boolean reachedMax = newLevel >= AscensionConfig.maxLevel();
            this.access.execute((lvl, pos) -> {
                lvl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
                lvl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 0.8F + lvl.getRandom().nextFloat() * 0.4F);
                if (reachedMax) {
                    lvl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                if (lvl instanceof ServerLevel server) {
                    double x = pos.getX() + 0.5D;
                    double y = pos.getY() + 1.0D;
                    double z = pos.getZ() + 0.5D;
                    // Estallido de glifos alrededor de la mesa y chispas que suben desde la gema.
                    server.sendParticles(ParticleTypes.ENCHANT, x, y + 0.3D, z, 60, 0.6D, 0.4D, 0.6D, 1.0D);
                    server.sendParticles(ParticleTypes.END_ROD, x, y, z, reachedMax ? 30 : 10, 0.15D, 0.1D, 0.15D, 0.08D);
                    if (reachedMax) {
                        server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.2D, z, 40, 0.3D, 0.3D, 0.3D, 0.4D);
                    }
                }
            });
            this.refresh();
        }
        return true;
    }

    /**
     * Convierte un encantamiento vanilla en su versión Giga, manteniendo el nivel.
     * Requisitos: que exista la versión Giga, que el encantamiento esté al menos en su nivel
     * máximo normal y que haya una Estrella del Nether en los materiales (se gasta una).
     */
    private boolean convertToGiga(int index) {
        ItemStack target = this.container.getItem(TARGET_SLOT);
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        if (index < 0 || index >= entries.size()) return false;

        Holder<Enchantment> vanilla = entries.get(index);
        Optional<Holder.Reference<Enchantment>> giga = this.gigaFor(vanilla);
        if (giga.isEmpty() || !UpgradeLogic.meetsGigaLevel(target, vanilla) || !this.hasGigaCatalyst()) return false;

        if (isServer()) {
            int level = UpgradeLogic.currentLevel(target, vanilla);

            // Gasta una Estrella del Nether (con borde de diamante, a veces no).
            TableTrim trim = readTrim();
            boolean keepStar = trim == TableTrim.DIAMOND && this.player.getRandom().nextFloat() < DIAMOND_STAR_CHANCE;
            if (keepStar) announce("gui.ascension.trim.proc.diamond", trim);
            for (int i = 1; i <= MATERIAL_SLOTS && !keepStar; i++) {
                ItemStack stack = this.container.getItem(i);
                if (stack.is(Items.NETHER_STAR)) {
                    stack.shrink(1);
                    if (stack.isEmpty()) this.container.setItem(i, ItemStack.EMPTY);
                    break;
                }
            }

            ItemStack converted = target.copy();
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(converted));
            mutable.set(vanilla, 0);            // quita el normal
            mutable.set(giga.get(), level);     // pone el Giga con el mismo nivel
            EnchantmentHelper.setEnchantments(converted, mutable.toImmutable());
            this.container.setItem(TARGET_SLOT, converted);
            this.container.setChanged();

            this.access.execute((lvl, pos) -> {
                lvl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.3F);
                lvl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
                if (lvl instanceof ServerLevel server) {
                    double x = pos.getX() + 0.5D;
                    double y = pos.getY() + 1.1D;
                    double z = pos.getZ() + 0.5D;
                    server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 40, 0.4D, 0.3D, 0.4D, 0.05D);
                    server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 60, 0.3D, 0.4D, 0.3D, 0.5D);
                    server.sendParticles(ParticleTypes.END_ROD, x, y, z, 25, 0.2D, 0.2D, 0.2D, 0.1D);
                }
            });
            this.refresh();
        }
        return true;
    }

    /**
     * Gasta materiales por al menos {@code cost} puntos, intentando desperdiciar lo menos posible:
     * primero usa los materiales más valiosos que caben completos en el costo y, si falta algo,
     * consume una unidad del material más barato que lo cubra. El sobrante no se devuelve.
     */
    private void consumeMaterials(int cost, @Nullable TableTrim trim) {
        int[] counts = new int[MATERIAL_SLOTS];
        int[] values = new int[MATERIAL_SLOTS];
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = this.container.getItem(1 + i);
            counts[i] = stack.getCount();
            values[i] = itemValue(stack, trim);
        }
        int[] take = planConsumption(counts, values, cost);
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            if (take[i] <= 0) continue;
            ItemStack stack = this.container.getItem(1 + i);
            stack.shrink(take[i]);
            if (stack.isEmpty()) this.container.setItem(1 + i, ItemStack.EMPTY);
        }
        this.container.setChanged();
    }

    /**
     * Decide cuántas unidades gastar de cada material para cubrir {@code cost}, desperdiciando lo menos posible:
     * primero los más valiosos que caben completos y, si falta algo, una unidad del más barato que lo cubra
     * (o de a una, de más barato a más caro). Lo usa el servidor para gastar y el cliente para la vista previa.
     */
    public static int[] planConsumption(int[] counts, int[] values, int cost) {
        int n = counts.length;
        int[] left = counts.clone();
        int[] take = new int[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;

        java.util.Arrays.sort(order, (a, b) -> Integer.compare(values[b], values[a])); // más caro primero
        int remaining = cost;
        for (int i : order) {
            if (remaining <= 0) break;
            if (values[i] <= 0) continue;
            int t = Math.min(left[i], remaining / values[i]);
            if (t > 0) {
                take[i] += t;
                left[i] -= t;
                remaining -= t * values[i];
            }
        }
        if (remaining > 0) {
            java.util.Arrays.sort(order, (a, b) -> Integer.compare(values[a], values[b])); // más barato primero
            for (int i : order) {
                if (values[i] > 0 && left[i] > 0 && values[i] >= remaining) {
                    take[i]++;
                    left[i]--;
                    remaining = 0;
                    break;
                }
            }
            for (int i : order) {
                while (remaining > 0 && values[i] > 0 && left[i] > 0) {
                    take[i]++;
                    left[i]--;
                    remaining -= values[i];
                }
            }
        }
        return take;
    }

    // ---------------------------------------------------------------------
    // Bajar nivel, extraer a libro y aplicar libro
    // ---------------------------------------------------------------------

    /** Baja un nivel (en I lo quita) y guarda en la mesa la mitad de lo que costaba ese nivel. */
    private boolean downgrade(int index) {
        ItemStack target = this.container.getItem(TARGET_SLOT);
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        if (index < 0 || index >= entries.size()) return false;
        if (!isServer()) return true;
        Holder<Enchantment> enchantment = entries.get(index);
        int level = UpgradeLogic.currentLevel(target, enchantment);
        if (level <= 0) return false;

        int refund = (int) Math.floor(effectiveCost(level, readTrim()) * REFUND_FRACTION);
        writeCredit((int) Math.min(Integer.MAX_VALUE, (long) readCredit() + refund));

        ItemStack changed = target.copy();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(changed));
        mutable.set(enchantment, level - 1);
        EnchantmentHelper.setEnchantments(changed, mutable.toImmutable());
        this.container.setItem(TARGET_SLOT, cleanBook(changed));
        this.player.sendOverlayMessage(Component.translatable("gui.ascension.refund.done", refund).withColor(0xE0B84A));
        this.access.execute((lvl, pos) -> lvl.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.2F));
        this.refresh();
        return true;
    }

    /** Saca el encantamiento del ítem y lo guarda en el libro normal del slot de libro. Cuesta el 25 % del nivel. */
    private boolean extract(int index) {
        ItemStack target = this.container.getItem(TARGET_SLOT);
        ItemStack book = this.container.getItem(BOOK_SLOT);
        List<Holder<Enchantment>> entries = UpgradeLogic.upgradeable(target);
        if (index < 0 || index >= entries.size() || !book.is(Items.BOOK) || book.getCount() != 1) return false;
        if (!isServer()) return true;
        Holder<Enchantment> enchantment = entries.get(index);
        int level = UpgradeLogic.currentLevel(target, enchantment);
        if (level <= 0) return false;

        TableTrim trim = readTrim();
        int cost = Math.max(1, (int) Math.ceil(effectiveCost(level, trim) * EXTRACT_FRACTION));
        if (available(trim) < cost) return false;
        pay(cost, trim);

        ItemStack stored = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable bookEnchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        bookEnchants.set(enchantment, level);
        stored.set(DataComponents.STORED_ENCHANTMENTS, bookEnchants.toImmutable());
        this.container.setItem(BOOK_SLOT, stored);

        ItemStack changed = target.copy();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(changed));
        mutable.set(enchantment, 0);
        EnchantmentHelper.setEnchantments(changed, mutable.toImmutable());
        this.container.setItem(TARGET_SLOT, cleanBook(changed));
        this.container.setChanged();
        this.access.execute((lvl, pos) -> {
            lvl.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
            lvl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 0.6F);
            if (lvl instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 40, 0.4D, 0.3D, 0.4D, 0.6D);
            }
        });
        this.refresh();
        return true;
    }

    /** Un libro encantado que se quedó sin encantamientos vuelve a ser un libro normal. */
    private static ItemStack cleanBook(ItemStack stack) {
        if (stack.is(Items.ENCHANTED_BOOK) && !EnchantmentHelper.hasAnyEnchantments(stack)) return new ItemStack(Items.BOOK);
        return stack;
    }

    /** Resultado de combinar el libro con el ítem: encantamientos que cambian y su nuevo nivel. */
    private Map<Holder<Enchantment>, Integer> planApply(ItemStack target, ItemStack book) {
        Map<Holder<Enchantment>, Integer> result = new LinkedHashMap<>();
        if (target.isEmpty() || !book.is(Items.ENCHANTED_BOOK)) return result;
        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(target);
        boolean targetIsBook = target.is(Items.ENCHANTED_BOOK) || target.is(Items.BOOK);
        for (var entry : stored.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int bookLevel = entry.getIntValue();
            if (!targetIsBook && !holder.value().canEnchant(target)) continue;
            int cur = current.getLevel(holder);
            if (cur == 0) {
                java.util.Set<Holder<Enchantment>> others = new java.util.HashSet<>(current.keySet());
                others.addAll(result.keySet());
                if (!EnchantmentHelper.isEnchantmentCompatible(others, holder)) continue;
            }
            // Como el yunque: dos iguales suben uno (sin pasar el máximo normal); si no, queda el mayor.
            int next = cur == bookLevel && bookLevel < holder.value().getMaxLevel() ? bookLevel + 1 : Math.max(cur, bookLevel);
            if (next > cur) result.put(holder, next);
        }
        return result;
    }

    /** Costo de aplicar el libro: un cuarto del costo de cada nivel resultante. -1 si no hay nada que aplicar. */
    private int applyBookCost(@Nullable TableTrim trim) {
        Map<Holder<Enchantment>, Integer> plan = planApply(this.container.getItem(TARGET_SLOT), this.container.getItem(BOOK_SLOT));
        if (plan.isEmpty()) return -1;
        long total = 0;
        for (int level : plan.values()) total += Math.max(1, (int) Math.ceil(effectiveCost(level, trim) * EXTRACT_FRACTION));
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private boolean applyBook() {
        if (!isServer()) return true;
        ItemStack target = this.container.getItem(TARGET_SLOT);
        Map<Holder<Enchantment>, Integer> plan = planApply(target, this.container.getItem(BOOK_SLOT));
        if (plan.isEmpty()) return false;
        TableTrim trim = readTrim();
        int cost = applyBookCost(trim);
        if (cost < 0 || available(trim) < cost) return false;
        pay(cost, trim);

        ItemStack changed = target.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : target.copy();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(changed));
        plan.forEach(mutable::set);
        EnchantmentHelper.setEnchantments(changed, mutable.toImmutable());
        this.container.setItem(TARGET_SLOT, changed);
        this.container.setItem(BOOK_SLOT, ItemStack.EMPTY);
        this.container.setChanged();
        this.access.execute((lvl, pos) -> {
            lvl.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.4F);
            lvl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
            if (lvl instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 60, 0.5D, 0.3D, 0.5D, 0.8D);
            }
        });
        this.refresh();
        return true;
    }

    // ---------------------------------------------------------------------
    // Comportamiento estándar de contenedor
    // ---------------------------------------------------------------------

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Como la mesa vanilla: los ítems vuelven al jugador al cerrar.
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, AscensionMod.ASCENSION_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack raw = slot.getItem();
        copy = raw.copy();

        if (index < INV_START) {
            // De la mesa al inventario
            if (!this.moveItemStackTo(raw, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (UpgradeLogic.isUpgradeTarget(raw) && !this.slots.get(TARGET_SLOT).hasItem()) {
                moved = this.moveItemStackTo(raw.split(1), TARGET_SLOT, TARGET_SLOT + 1, false);
                if (!moved) raw.grow(1);
            } else if ((raw.is(Items.BOOK) || raw.is(Items.ENCHANTED_BOOK)) && !this.slots.get(BOOK_SLOT).hasItem()) {
                moved = this.moveItemStackTo(raw.split(1), BOOK_SLOT, BOOK_SLOT + 1, false);
                if (!moved) raw.grow(1);
            } else if (UpgradeLogic.pointsPerItem(raw) > 0) {
                moved = this.moveItemStackTo(raw, 1, INV_START, false);
            }
            if (!moved) {
                if (index < HOTBAR_START) {
                    if (!this.moveItemStackTo(raw, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(raw, INV_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (raw.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (raw.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, raw);
        return copy;
    }
}
