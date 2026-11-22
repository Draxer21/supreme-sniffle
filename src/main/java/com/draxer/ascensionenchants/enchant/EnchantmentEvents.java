package com.draxer.ascensionenchants.enchant;

import com.draxer.ascensionenchants.AscensionEnchants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Efectos de los encantamientos que no se pueden expresar solo con JSON. */
@EventBusSubscriber(modid = AscensionEnchants.MODID)
public final class EnchantmentEvents {
    private EnchantmentEvents() {}

    // =====================================================================
    // Robo de vida: cura una fracción del daño cuerpo a cuerpo infligido.
    // I = 10 %, II = 15 %, III = 18 % ... (crece cada vez menos; nunca llega al 30 %).
    // =====================================================================
    @SubscribeEvent
    static void onLivingDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (source.getDirectEntity() != attacker) return; // solo golpes directos, no flechas
        if (attacker.level().isClientSide()) return;

        int lvl = ModEnchantments.level(attacker.level(), ModEnchantments.LIFE_STEAL, attacker.getMainHandItem());
        if (lvl <= 0) return;

        float fraction = 0.3F * lvl / (lvl + 2.0F);
        float heal = event.getHealthDamage() * fraction;
        if (heal <= 0.0F) return;

        attacker.heal(heal);
        if (heal >= 1.0F && attacker.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() + 0.3D, attacker.getZ(),
                    1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }

    // =====================================================================
    // Telequinesis: lo que sueltan bloques y mobs va directo al inventario.
    // Si el inventario está lleno, lo que no cabe cae al suelo como siempre.
    // =====================================================================
    @SubscribeEvent(priority = EventPriority.LOW) // después de Fundición y de la Fortuna Giga
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return;
        if (ModEnchantments.level(event.getLevel(), ModEnchantments.TELEKINESIS, event.getTool()) <= 0) return;
        pullIntoInventory(player, event.getDrops().iterator());
    }

    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (ModEnchantments.level(player.level(), ModEnchantments.TELEKINESIS, player.getMainHandItem()) <= 0) return;
        pullIntoInventory(player, event.getDrops().iterator());
    }

    // =====================================================================
    // Fundición: lo que cae sale ya fundido (con la Fortuna ya aplicada).
    // =====================================================================
    @SubscribeEvent
    static void onSmeltDrops(BlockDropsEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (ModEnchantments.level(level, ModEnchantments.SMELTING, event.getTool()) <= 0) return;
        boolean any = false;
        for (ItemEntity entity : event.getDrops()) {
            ItemStack stack = entity.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) continue;
            result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * stack.getCount()));
            entity.setItem(result);
            any = true;
        }
        if (any) {
            BlockPos pos = event.getPos();
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    private static void pullIntoInventory(Player player, Iterator<ItemEntity> drops) {
        while (drops.hasNext()) {
            ItemEntity entity = drops.next();
            ItemStack stack = entity.getItem();
            player.getInventory().add(stack); // descuenta del stack lo que entró
            if (stack.isEmpty()) {
                drops.remove();
            } else {
                entity.setItem(stack);
            }
        }
    }

    // =====================================================================
    // Vena minera: al romper un mineral, rompe también los iguales conectados
    // (incluidas diagonales). Máximo 8 bloques extra por nivel. Agachado: desactivado.
    // =====================================================================
    /** true mientras se rompen bloques extra (para que esos no vuelvan a disparar los encantamientos). */
    private static boolean breakingExtra = false;

    @SubscribeEvent
    static void onBreakBlock(BreakBlockEvent event) {
        if (breakingExtra || event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isShiftKeyDown()) return;
        if (!(event.getLevel() instanceof Level level)) return;

        BlockState origin = event.getState();
        ItemStack tool = player.getMainHandItem();
        List<BlockPos> extra = List.of();

        // Vena minera
        if (origin.is(Tags.Blocks.ORES) && tool.isCorrectToolForDrops(origin)) {
            int lvl = ModEnchantments.level(level, ModEnchantments.VEIN_MINER, tool);
            if (lvl > 0) extra = findVein(level, event.getPos(), origin.getBlock(), 8 * lvl);
        }
        // Talador: solo árboles naturales (troncos conectados que tocan hojas), para no desarmar casas de madera.
        if (extra.isEmpty() && origin.is(BlockTags.LOGS)) {
            int lvl = ModEnchantments.level(level, ModEnchantments.TIMBER, tool);
            if (lvl > 0) {
                List<BlockPos> tree = findVein(level, event.getPos(), origin.getBlock(), 32 * lvl);
                if (touchesLeaves(level, event.getPos(), tree)) extra = tree;
            }
        }
        // Excavación 3×3
        if (extra.isEmpty() && tool.isCorrectToolForDrops(origin)
                && ModEnchantments.level(level, ModEnchantments.EXCAVATION, tool) > 0) {
            extra = area3x3(level, player, tool, event.getPos(), origin);
        }
        if (extra.isEmpty()) return;

        breakingExtra = true;
        try {
            for (BlockPos pos : extra) {
                ItemStack current = player.getMainHandItem();
                if (current.isEmpty() || current != tool) break;               // la herramienta cambió o se rompió
                if (current.isDamageableItem() && current.getMaxDamage() - current.getDamageValue() <= 2) break; // no romperla
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            breakingExtra = false;
        }
    }

    private static boolean touchesLeaves(Level level, BlockPos origin, List<BlockPos> logs) {
        List<BlockPos> all = new ArrayList<>(logs);
        all.add(origin);
        for (BlockPos log : all) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                if (level.getBlockState(log.relative(d)).is(BlockTags.LEAVES)) return true;
            }
        }
        return false;
    }

    /**
     * Los 8 bloques alrededor del roto, en el plano que mira el jugador: si mira muy arriba o abajo,
     * plano horizontal; si no, plano vertical de frente. Solo bloques que la herramienta rompe bien
     * y que no sean mucho más duros que el original (no rompe obsidiana al picar piedra).
     */
    private static List<BlockPos> area3x3(Level level, Player player, ItemStack tool, BlockPos center, BlockState origin) {
        float pitch = player.getXRot();
        net.minecraft.core.Direction.Axis normal = Math.abs(pitch) > 50.0F
                ? net.minecraft.core.Direction.Axis.Y
                : player.getDirection().getAxis();
        float originHardness = origin.getDestroySpeed(level, center);
        List<BlockPos> result = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos pos = switch (normal) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.hasBlockEntity()) continue;
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0.0F || hardness > originHardness + 2.0F) continue;
                if (!tool.isCorrectToolForDrops(state)) continue;
                result.add(pos);
            }
        }
        return result;
    }

    /** Búsqueda en anchura de bloques iguales conectados, sin incluir el bloque de origen. */
    private static List<BlockPos> findVein(Level level, BlockPos start, Block block, int limit) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos next = current.offset(dx, dy, dz);
                        if (!visited.add(next)) continue;
                        if (level.getBlockState(next).is(block)) {
                            result.add(next);
                            queue.add(next);
                            if (result.size() >= limit) return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    // =====================================================================
    // Paso etéreo: con Shift en el aire, caída lenta y sin daño por caída.
    // I = caída moderada, II = caída muy lenta. Corre en cliente y servidor
    // (el movimiento del jugador lo calcula el cliente; el daño, el servidor).
    // =====================================================================
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.onGround() || !player.isShiftKeyDown() || player.getAbilities().flying || player.isInWater()) return;

        int lvl = ModEnchantments.level(player.level(), ModEnchantments.ETHEREAL_STEP, player.getItemBySlot(EquipmentSlot.FEET));
        if (lvl <= 0) return;

        double minFallSpeed = lvl >= 2 ? -0.08D : -0.18D;
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < minFallSpeed) {
            player.setDeltaMovement(motion.x, minFallSpeed, motion.z);
        }
        player.resetFallDistance();

        if (player.level().isClientSide() && player.getRandom().nextInt(3) == 0) {
            player.level().addParticle(ParticleTypes.CLOUD,
                    player.getX() + (player.getRandom().nextDouble() - 0.5D) * 0.6D,
                    player.getY(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 0.6D,
                    0.0D, -0.02D, 0.0D);
        }
    }
}
