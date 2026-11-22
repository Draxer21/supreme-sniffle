package com.draxer.draxmorph;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Absorber formas al matar, tamaño de la forma, rasgos que se aplican cada tick
 * y mobs que no atacan a los de su misma especie.
 */
@EventBusSubscriber(modid = DraxMorph.MODID)
public final class MorphEvents {
    private MorphEvents() {}

    /** Matar un mob te enseña su forma. */
    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !(victim instanceof Mob)) return;
        if (!MorphForms.canMorphInto(victim.getType())) return;
        String id = MorphForms.idOf(victim.getType());
        MorphData data = MorphForms.data(player);
        if (data.has(id)) return;
        player.setData(DraxMorph.MORPH, data.withUnlocked(id));
        player.sendSystemMessage(Component.translatable("drax_morph.unlocked", victim.getType().getDescription()).withColor(0xC080FF));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** El tamaño (hitbox y altura de los ojos) es el de la forma. */
    @SubscribeEvent
    static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getPose() == Pose.SLEEPING) return;
        String id;
        try {
            id = MorphForms.current(player);
        } catch (RuntimeException e) {
            return; // los datos aún no existen (el jugador se está construyendo)
        }
        if (id.isEmpty()) return;
        MorphForms.type(id).ifPresent(type -> {
            EntityDimensions dims = type.getDimensions();
            event.setNewSize(dims);
        });
    }

    /** Rasgos de la forma, cada tick (en ambos lados para que el movimiento se sienta bien). */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        String id = MorphForms.current(player);
        if (id.isEmpty()) return;
        Set<MorphForms.Trait> traits = MorphForms.traits(id);

        // Trepar paredes (araña)
        if (traits.contains(MorphForms.Trait.CLIMB) && player.horizontalCollision && !player.isShiftKeyDown()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.2D), motion.z);
            player.resetFallDistance();
        }

        if (player.level().isClientSide() || !(player instanceof ServerPlayer server)) return;

        // Volar
        if (traits.contains(MorphForms.Trait.FLY) && !server.getAbilities().mayfly) {
            server.getAbilities().mayfly = true;
            server.onUpdateAbilities();
        }
        if (server.tickCount % 20 == 0) {
            if (traits.contains(MorphForms.Trait.WATER)) {
                effect(server, MobEffects.WATER_BREATHING, 0);
                if (server.isInWater()) effect(server, MobEffects.DOLPHINS_GRACE, 0);
            }
            if (traits.contains(MorphForms.Trait.FIRE_IMMUNE)) effect(server, MobEffects.FIRE_RESISTANCE, 0);
            if (traits.contains(MorphForms.Trait.NIGHT_VISION)) nightVision(server);
            if (traits.contains(MorphForms.Trait.SPEED)) effect(server, MobEffects.SPEED, 1);
            else if (traits.contains(MorphForms.Trait.FAST)) effect(server, MobEffects.SPEED, 0);
            if (traits.contains(MorphForms.Trait.JUMP)) effect(server, MobEffects.JUMP_BOOST, 1);
            if (traits.contains(MorphForms.Trait.SLOW_FALL)) effect(server, MobEffects.SLOW_FALLING, 0);
            if (traits.contains(MorphForms.Trait.STRONG)) effect(server, MobEffects.STRENGTH, 1);
            if (traits.contains(MorphForms.Trait.TOUGH)) effect(server, MobEffects.RESISTANCE, 0);
            // Por si el modificador de vida se perdió (al reaparecer, cambiar de dimensión...)
            var health = server.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (health != null && health.getModifier(MorphActions.HEALTH_MODIFIER) == null) {
                MorphActions.applyHealth(server, id);
            }
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false));
        }
    }

    private static void nightVision(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        }
    }

    /** Sin daño por caída para las formas que no lo sufren. */
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getSource().is(DamageTypeTags.IS_FALL) && MorphForms.has(MorphForms.current(player), MorphForms.Trait.NO_FALL)) {
            event.setCanceled(true);
        }
    }

    /** Los mobs no atacan a un jugador transformado en su misma especie. */
    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        String id = MorphForms.current(player);
        if (!id.isEmpty() && id.equals(MorphForms.idOf(event.getEntity().getType()))) {
            event.setCanceled(true);
        }
    }

    /** Al entrar o reaparecer se reaplican tamaño y vida. */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MorphActions.onFormChanged(player, "", MorphForms.current(player));
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MorphActions.onFormChanged(player, "", MorphForms.current(player));
        }
    }
}
