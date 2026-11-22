package com.draxer.draxmorph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Transformarse, volver a humano y usar habilidades (todo en el servidor). */
public final class MorphActions {
    public static final Identifier HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(DraxMorph.MODID, "morph_health");
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private MorphActions() {}

    public static void select(ServerPlayer player, String id) {
        MorphData data = MorphForms.data(player);
        if (!id.isEmpty() && !data.has(id)) return;           // no la tiene absorbida
        if (id.equals(data.current())) return;
        if (!id.isEmpty() && MorphForms.type(id).isEmpty()) return;

        String previous = data.current();
        player.setData(DraxMorph.MORPH, data.withCurrent(id));
        onFormChanged(player, previous, id);

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY(0.5D), player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.sendOverlayMessage(id.isEmpty()
                ? Component.translatable("drax_morph.human")
                : Component.translatable("drax_morph.morphed", MorphForms.type(id).get().getDescription()));
    }

    /** Ajusta tamaño, vida y vuelo al cambiar de forma. */
    public static void onFormChanged(ServerPlayer player, String previous, String current) {
        player.refreshDimensions();
        applyHealth(player, current);
        if (MorphForms.has(previous, MorphForms.Trait.FLY) && !MorphForms.has(current, MorphForms.Trait.FLY)
                && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void applyHealth(ServerPlayer player, String current) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(HEALTH_MODIFIER);
        if (!current.isEmpty()) {
            double target = Math.max(2.0D, Math.min(200.0D, MorphForms.maxHealth(current)));
            health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER, target - 20.0D, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    /** Habilidad de la forma actual (tecla V), con su tiempo de recarga. */
    public static void useAbility(ServerPlayer player) {
        String id = MorphForms.current(player);
        MorphForms.Ability ability = MorphForms.ability(id);
        if (ability == MorphForms.Ability.NONE) {
            player.sendOverlayMessage(Component.translatable("drax_morph.no_ability"));
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Long ready = COOLDOWN.get(player.getUUID());
        if (ready != null && now < ready) {
            player.sendOverlayMessage(Component.translatable("drax_morph.cooldown", (ready - now + 19) / 20));
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        int cooldown = switch (ability) {
            case TELEPORT -> teleport(player, level) ? 30 : 0;
            case SMALL_FIREBALL -> shoot(player, EntityTypes.SMALL_FIREBALL, 1.5F, SoundEvents.BLAZE_SHOOT) ? 8 : 0;
            case FIREBALL -> shoot(player, EntityTypes.FIREBALL, 1.2F, SoundEvents.GHAST_SHOOT) ? 40 : 0;
            case WIND_CHARGE -> shoot(player, EntityTypes.WIND_CHARGE, 1.5F, SoundEvents.BREEZE_SHOOT) ? 20 : 0;
            case SNOWBALL -> shoot(player, EntityTypes.SNOWBALL, 1.5F, SoundEvents.SNOW_GOLEM_SHOOT) ? 4 : 0;
            case ARROW -> shoot(player, EntityTypes.ARROW, 2.5F, SoundEvents.SKELETON_SHOOT) ? 20 : 0;
            case LEVITATION_SHOT -> levitate(player, level, eye, look) ? 40 : 0;
            case SONIC_BOOM -> sonicBoom(player, level, eye, look) ? 100 : 0;
            case EXPLODE -> {
                level.explode(player, player.getX(), player.getY(), player.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
                select(player, ""); // como el creeper: después de explotar se acaba la forma
                yield 60;
            }
            case NONE -> 0;
        };
        if (cooldown > 0) COOLDOWN.put(player.getUUID(), now + cooldown);
    }

    private static boolean shoot(ServerPlayer player, EntityType<? extends Entity> type, float speed, net.minecraft.sounds.SoundEvent sound) {
        Entity created = type.create(player.level(), EntitySpawnReason.TRIGGERED);
        if (!(created instanceof Projectile projectile)) return false;
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        projectile.setOwner(player);
        projectile.setPos(eye.x + look.x * 0.8D, eye.y - 0.1D + look.y * 0.8D, eye.z + look.z * 0.8D);
        projectile.shoot(look.x, look.y, look.z, speed, 0.5F);
        if (projectile instanceof AbstractArrow arrow) arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean teleport(ServerPlayer player, ServerLevel level) {
        HitResult hit = player.pick(32.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos target = blockHit.getBlockPos().relative(blockHit.getDirection());
        for (int up = 0; up < 3; up++) {
            BlockPos pos = target.above(up);
            Vec3 at = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            AABB box = player.getBoundingBox().move(at.subtract(player.position()));
            if (level.noCollision(player, box)) {
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.5D), player.getZ(), 40, 0.3D, 0.6D, 0.3D, 0.5D);
                player.teleportTo(at.x, at.y, at.z);
                player.resetFallDistance();
                level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    private static boolean levitate(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look) {
        LivingEntity target = firstInRay(player, level, eye, look, 20.0D);
        if (target == null) return false;
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 0));
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5D), target.getZ(), 20, 0.3D, 0.5D, 0.3D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean sonicBoom(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look) {
        for (int i = 1; i <= 15; i++) {
            Vec3 p = eye.add(look.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
        AABB area = player.getBoundingBox().expandTowards(look.scale(15.0D)).inflate(1.0D);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity target : hit) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > 16.0D || look.dot(to.normalize()) < 0.9D) continue;
            target.hurtServer(level, level.damageSources().sonicBoom(player), 10.0F);
        }
        return true;
    }

    private static LivingEntity firstInRay(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look, double range) {
        AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || look.dot(to.normalize()) < 0.95D) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }
}
