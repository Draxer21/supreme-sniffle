package com.draxer.ascensionenchants.parry;

import com.draxer.ascensionenchants.AscensionEnchants;
import com.draxer.ascensionenchants.enchant.ModEnchantments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Parry perfecto: si un golpe o proyectil llega de frente justo después de alzar la guardia
 * (espada o escudo), se anula todo el daño y además:
 *  - el atacante queda aturdido (lento + débil) y sale empujado;
 *  - tu siguiente golpe cuerpo a cuerpo en 3 segundos hace daño extra con efecto de crítico;
 *  - los proyectiles rebotan hacia quien los lanzó;
 *  - chispas y un "clang" metálico.
 *
 * Ventana base: 4 ticks (0,2 s) tras alzar la guardia (en el escudo se cuenta desde que empieza a bloquear).
 * El encantamiento Desvío la amplía. Alzar la guardia repetidamente (antes de 0,6 s desde la anterior) no cuenta como parry.
 */
@EventBusSubscriber(modid = AscensionEnchants.MODID)
public final class ParryHandler {
    private static final int BASE_WINDOW_TICKS = 4;
    private static final int SPAM_GUARD_TICKS = 12;
    private static final int COUNTER_TICKS = 60;

    /** Momento en que cada jugador alzó la guardia por última vez y si esa vez cuenta para parry. */
    private static final Map<UUID, long[]> GUARD = new HashMap<>();
    /** Hasta qué tick tiene cada jugador el contraataque cargado, y con qué multiplicador. */
    private static final Map<UUID, double[]> COUNTER = new HashMap<>();

    private ParryHandler() {}

    // ---------------------------------------------------------------------
    // Alzar la guardia
    // ---------------------------------------------------------------------

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.has(DataComponents.BLOCKS_ATTACKS)) return;

        // La espada solo bloquea con la mano secundaria vacía: así comer, poner antorchas o usar
        // el escudo de la otra mano funciona igual que siempre.
        if (event.getHand() == InteractionHand.MAIN_HAND && stack.is(ItemTags.SWORDS)
                && !player.getOffhandItem().isEmpty()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.PASS);
            return;
        }

        if (!player.level().isClientSide()) {
            long now = player.level().getGameTime();
            long[] previous = GUARD.get(player.getUUID());
            boolean valid = previous == null || now - previous[0] >= SPAM_GUARD_TICKS;
            GUARD.put(player.getUUID(), new long[] {now, valid ? 1L : 0L});
        }
    }

    /** ¿El jugador está en la ventana de parry perfecto en este momento? Devuelve el nivel de Desvío + 1, o 0 si no. */
    private static int perfectParryLevel(Player player) {
        if (!player.isUsingItem()) return 0;
        ItemStack using = player.getUseItem();
        BlocksAttacks blocks = using.get(DataComponents.BLOCKS_ATTACKS);
        if (blocks == null) return 0;

        long[] guard = GUARD.get(player.getUUID());
        if (guard != null && guard[1] == 0L) return 0; // guardia "spameada"

        int parryLevel = ModEnchantments.level(player.level(), ModEnchantments.PARRY, using);
        int window = BASE_WINDOW_TICKS + Math.round(6.0F * parryLevel / (parryLevel + 2.0F));
        int delay = Math.round(blocks.blockDelaySeconds() * 20.0F);
        int t = player.getTicksUsingItem() - delay;
        return (t >= 0 && t <= window) ? parryLevel + 1 : 0;
    }

    /** ¿La posición está delante del jugador (dentro de 90° a cada lado de donde mira)? */
    private static boolean isInFront(Player player, Vec3 source) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = source.subtract(player.position());
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        Vec3 flatTo = new Vec3(to.x, 0.0D, to.z);
        if (flatLook.lengthSqr() < 1.0E-6 || flatTo.lengthSqr() < 1.0E-6) return true;
        return flatLook.normalize().dot(flatTo.normalize()) > 0.0D;
    }

    // ---------------------------------------------------------------------
    // Golpes cuerpo a cuerpo (y contraataque)
    // ---------------------------------------------------------------------

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // 1) Contraataque: el atacante es un jugador con el golpe cargado.
        if (source.getEntity() instanceof Player attacker && source.getDirectEntity() == attacker) {
            double[] counter = COUNTER.get(attacker.getUUID());
            if (counter != null) {
                COUNTER.remove(attacker.getUUID());
                if (attacker.level().getGameTime() <= (long) counter[0]) {
                    event.setAmount((float) (event.getAmount() * counter[1]));
                    LivingEntity target = event.getEntity();
                    if (target.level() instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(),
                                20, 0.3D, 0.4D, 0.3D, 0.4D);
                        server.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
                    }
                }
            }
        }

        // 2) Parry perfecto del jugador que recibe el golpe.
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        Vec3 from = source.getSourcePosition();
        if (from == null || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) return;
        if (!isInFront(player, from)) return;

        int parry = perfectParryLevel(player);
        if (parry <= 0) return;

        event.setCanceled(true);
        Entity attacker = source.getEntity();
        if (source.getDirectEntity() instanceof LivingEntity direct && direct == attacker) {
            stun(player, direct);
        }
        chargeCounter(player, parry - 1);
        parryEffects(player, from);
        flash(player, 1.0F);
    }

    // ---------------------------------------------------------------------
    // Proyectiles: rebotan hacia su dueño
    // ---------------------------------------------------------------------

    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        Projectile projectile = event.getProjectile();
        if (!isInFront(player, projectile.position())) return;

        int parry = perfectParryLevel(player);
        if (parry <= 0) return;

        event.setCanceled(true);

        Entity owner = projectile.getOwner();
        Vec3 direction = owner != null
                ? owner.getEyePosition().subtract(projectile.position()).normalize()
                : player.getViewVector(1.0F);
        double speed = Math.max(projectile.getDeltaMovement().length(), 1.0D) * 1.2D;
        projectile.setDeltaMovement(direction.scale(speed));
        projectile.setOwner(player);
        projectile.hurtMarked = true;

        chargeCounter(player, parry - 1);
        parryEffects(player, projectile.position());
        flash(player, 0.6F);
    }

    // ---------------------------------------------------------------------
    // Efectos
    // ---------------------------------------------------------------------

    private static void stun(Player player, LivingEntity attacker) {
        attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3));
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
        // En 26.2 el empuje recibe también el origen del daño y la cantidad (aquí 0: el parry no hace daño).
        attacker.knockback(0.9D, player.getX() - attacker.getX(), player.getZ() - attacker.getZ(),
                player.damageSources().playerAttack(player), 0.0F);
        attacker.hurtMarked = true;
    }

    private static void chargeCounter(Player player, int parryLevel) {
        double multiplier = 1.5D + 0.5D * parryLevel / (parryLevel + 2.0D); // 1,5x; con Desvío hasta casi 2x
        COUNTER.put(player.getUUID(), new double[] {player.level().getGameTime() + COUNTER_TICKS, multiplier});
    }

    /** Destello en la pantalla del jugador (solo si su cliente tiene el mod). */
    private static void flash(Player player, float strength) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection.hasChannel(ParryFlash.TYPE)) {
            PacketDistributor.sendToPlayer(serverPlayer, new ParryFlash(strength));
        }
    }

    private static void parryEffects(Player player, Vec3 impact) {
        if (!(player.level() instanceof ServerLevel server)) return;
        Vec3 eye = player.getEyePosition();
        Vec3 at = eye.add(impact.subtract(eye).normalize().scale(0.8D));

        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 18, 0.15D, 0.15D, 0.15D, 0.6D);
        server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.1D, 0.1D, 0.1D, 0.5D);
        server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 0.05D, 0.05D, 0.05D, 0.08D);
        if (player.getUseItem().is(Items.SHIELD)) {
            // Parry con escudo: golpe sordo de madera y una campanada, en vez del "clang" de la espada.
            server.sendParticles(ParticleTypes.WAX_OFF, at.x, at.y, at.z, 10, 0.25D, 0.25D, 0.25D, 0.2D);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8F, 1.6F);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, 0.8F);
        } else {
            server.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.8F);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }
}
