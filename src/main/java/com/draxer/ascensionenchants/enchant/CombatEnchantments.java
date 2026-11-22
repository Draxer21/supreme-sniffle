package com.draxer.ascensionenchants.enchant;

import com.draxer.ascensionenchants.AscensionEnchants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Encantamientos de armadura (Espinas de escarcha, Segundo aliento, Reflejos)
 * y de arco/ballesta (Flecha buscadora, Lluvia de flechas, Impacto explosivo).
 */
@EventBusSubscriber(modid = AscensionEnchants.MODID)
public final class CombatEnchantments {
    private static final String TAG_HOMING = "ae_homing_";
    private static final String TAG_EXPLOSIVE = "ae_explosive_";
    private static final String TAG_EXTRA = "ae_extra_arrow";
    private static final long SECOND_WIND_COOLDOWN = 20L * 60L * 5L; // 5 minutos

    /** Último uso de Segundo aliento por entidad (tick del mundo). */
    private static final Map<UUID, Long> SECOND_WIND = new HashMap<>();
    /** Flechas extra pendientes de Lluvia de flechas (no se pueden crear entidades dentro de EntityJoinLevelEvent). */
    private static final List<PendingRain> RAIN = new ArrayList<>();

    private record PendingRain(AbstractArrow original, int extra) {}

    private CombatEnchantments() {}

    // =====================================================================
    // Armadura
    // =====================================================================

    /** Reflejos: 5 % por nivel de esquivar por completo un golpe o proyectil (no caídas, fuego, etc.). */
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        DamageSource source = event.getSource();
        if (source.getEntity() == null) return; // solo ataques de algo/alguien
        int lvl = ModEnchantments.worn(victim, ModEnchantments.REFLEXES);
        if (lvl <= 0 || victim.getRandom().nextFloat() >= 0.05F * lvl) return;

        event.setCanceled(true);
        if (victim.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY(0.5D), victim.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.02D);
            server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0F, 1.6F);
        }
        if (victim instanceof Player player) player.sendOverlayMessage(Component.translatable("ascension_enchants.reflexes.dodged"));
    }

    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel server)) return;

        // Espinas de escarcha: el atacante cuerpo a cuerpo queda lento.
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim) {
            int lvl = ModEnchantments.worn(victim, ModEnchantments.FROST_THORNS);
            if (lvl > 0) {
                attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 + 20 * lvl, lvl - 1));
                server.sendParticles(ParticleTypes.SNOWFLAKE, attacker.getX(), attacker.getY(0.6D), attacker.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.02D);
            }
        }

        // Segundo aliento: al bajar del 30 % de vida, cura y da regeneración (recarga de 5 minutos).
        if (victim.isAlive() && victim.getHealth() <= victim.getMaxHealth() * 0.3F) {
            int lvl = ModEnchantments.worn(victim, ModEnchantments.SECOND_WIND);
            if (lvl <= 0) return;
            long now = server.getGameTime();
            Long last = SECOND_WIND.get(victim.getUUID());
            if (last != null && now - last < SECOND_WIND_COOLDOWN) return;
            SECOND_WIND.put(victim.getUUID(), now);

            victim.heal(2.0F + 3.0F * lvl);
            victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60 + 40 * lvl, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, lvl - 1));
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY(0.6D), victim.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.3D);
            server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.4F);
            if (victim instanceof Player player) player.sendOverlayMessage(Component.translatable("ascension_enchants.second_wind.triggered"));
        }
    }

    // =====================================================================
    // Arco y ballesta
    // =====================================================================

    @SubscribeEvent
    static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow) || arrow.entityTags().contains(TAG_EXTRA)) return;
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        Level level = event.getLevel();

        int homing = ModEnchantments.level(level, ModEnchantments.HOMING, weapon);
        if (homing > 0) arrow.addTag(TAG_HOMING + homing);
        int explosive = ModEnchantments.level(level, ModEnchantments.EXPLOSIVE_ARROWS, weapon);
        if (explosive > 0) arrow.addTag(TAG_EXPLOSIVE + explosive);
        int rain = ModEnchantments.level(level, ModEnchantments.ARROW_RAIN, weapon);
        if (rain > 0) {
            synchronized (RAIN) {
                RAIN.add(new PendingRain(arrow, 2 * rain));
            }
        }
    }

    /** Crea las flechas extra de Lluvia de flechas un tick después, abiertas en abanico. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<PendingRain> todo = new ArrayList<>();
        synchronized (RAIN) {
            RAIN.removeIf(p -> {
                if (p.original().level() != level) return false;
                todo.add(p);
                return true;
            });
        }
        for (PendingRain pending : todo) {
            AbstractArrow original = pending.original();
            Vec3 motion = original.getDeltaMovement();
            if (motion.lengthSqr() < 1.0E-4) continue;
            for (int i = 0; i < pending.extra(); i++) {
                Arrow extra = EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);
                if (extra == null) continue;
                // Abanico: alterna izquierda/derecha y aumenta el ángulo.
                double angle = Math.toRadians(6.0D * ((i / 2) + 1) * (i % 2 == 0 ? 1 : -1));
                Vec3 rotated = motion.yRot((float) angle);
                extra.setOwner(original.getOwner());
                extra.setPos(original.getX(), original.getY(), original.getZ());
                extra.setDeltaMovement(rotated);
                extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                extra.addTag(TAG_EXTRA);
                for (String tag : original.entityTags()) {
                    if (tag.startsWith(TAG_HOMING)) extra.addTag(tag);
                }
                level.addFreshEntity(extra);
            }
        }
    }

    /** Flecha buscadora: cada tick gira un poco hacia el enemigo más cercano que tenga por delante. */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof AbstractArrow arrow) || entity.level().isClientSide()) return;
        int lvl = tagLevel(arrow, TAG_HOMING);
        if (lvl <= 0) return;
        Vec3 motion = arrow.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.3D || arrow.tickCount > 100) return; // ya clavada o vieja

        Vec3 dir = motion.normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        AABB area = arrow.getBoundingBox().inflate(16.0D);
        for (LivingEntity target : arrow.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e.getType().getCategory() == MobCategory.MONSTER && e != arrow.getOwner())) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(arrow.position());
            double dist = to.length();
            if (dist < 1.0E-3 || dir.dot(to.scale(1.0D / dist)) < 0.5D) continue; // solo por delante (cono de 60°)
            if (dist < bestScore) {
                bestScore = dist;
                best = target;
            }
        }
        if (best == null) return;
        Vec3 wanted = best.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double turn = 0.12D * lvl;
        Vec3 steered = dir.scale(1.0D - turn).add(wanted.scale(turn)).normalize().scale(speed);
        arrow.setDeltaMovement(steered);
    }

    /** Impacto explosivo: la flecha explota al chocar, sin romper bloques. */
    @SubscribeEvent
    static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) return;
        int lvl = tagLevel(arrow, TAG_EXPLOSIVE);
        if (lvl <= 0) return;
        arrow.removeTag(TAG_EXPLOSIVE + lvl); // una sola explosión por flecha
        Vec3 at = event.getRayTraceResult().getLocation();
        level.explode(arrow, null, null, at, 1.0F + 0.6F * lvl, false, Level.ExplosionInteraction.NONE);
    }

    private static int tagLevel(Entity entity, String prefix) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                try {
                    return Integer.parseInt(tag.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
