package com.draxer.draxmorph;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;

/**
 * Qué puede hacer cada forma. Los mobs vanilla tienen rasgos a mano; los de otros mods reciben
 * lo básico (tamaño y vida) y rasgos deducidos (inmune al fuego, acuático).
 */
public final class MorphForms {
    public enum Trait { FLY, SLOW_FALL, CLIMB, WATER, FIRE_IMMUNE, NIGHT_VISION, SPEED, FAST, JUMP, NO_FALL, STRONG, TOUGH }

    public enum Ability { NONE, TELEPORT, SMALL_FIREBALL, FIREBALL, EXPLODE, WIND_CHARGE, SONIC_BOOM, SNOWBALL, ARROW, LEVITATION_SHOT }

    private static final Map<String, Set<Trait>> TRAITS = new HashMap<>();
    private static final Map<String, Ability> ABILITIES = new HashMap<>();
    /** Formas que no se pueden absorber (jefes y cosas raras). */
    public static final Set<String> BLOCKED = Set.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:giant",
            "minecraft:armor_stand", "minecraft:player", "minecraft:mannequin");

    static {
        t("bat", Trait.FLY, Trait.NIGHT_VISION, Trait.NO_FALL);
        t("parrot", Trait.FLY, Trait.NO_FALL);
        t("bee", Trait.FLY, Trait.NO_FALL);
        t("allay", Trait.FLY, Trait.NO_FALL);
        t("vex", Trait.FLY, Trait.NO_FALL);
        t("phantom", Trait.FLY, Trait.NO_FALL, Trait.NIGHT_VISION);
        t("ghast", Trait.FLY, Trait.NO_FALL);
        t("happy_ghast", Trait.FLY, Trait.NO_FALL);
        t("blaze", Trait.FLY, Trait.NO_FALL);
        t("chicken", Trait.SLOW_FALL, Trait.NO_FALL);
        t("spider", Trait.CLIMB, Trait.NIGHT_VISION);
        t("cave_spider", Trait.CLIMB, Trait.NIGHT_VISION);
        for (String water : new String[] {"cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin",
                "axolotl", "guardian", "elder_guardian", "turtle", "drowned", "frog", "tadpole", "nautilus", "zombie_nautilus"}) {
            t(water, Trait.WATER);
        }
        t("cat", Trait.FAST, Trait.NO_FALL, Trait.NIGHT_VISION);
        t("ocelot", Trait.FAST, Trait.NO_FALL, Trait.NIGHT_VISION);
        t("fox", Trait.FAST, Trait.NIGHT_VISION, Trait.JUMP);
        t("wolf", Trait.FAST, Trait.STRONG);
        t("rabbit", Trait.FAST, Trait.JUMP, Trait.NO_FALL);
        t("goat", Trait.JUMP, Trait.NO_FALL);
        t("slime", Trait.JUMP, Trait.NO_FALL);
        t("magma_cube", Trait.JUMP, Trait.NO_FALL);
        t("frog", Trait.WATER, Trait.JUMP, Trait.NO_FALL);
        for (String horse : new String[] {"horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel"}) t(horse, Trait.SPEED, Trait.JUMP);
        t("iron_golem", Trait.STRONG, Trait.TOUGH);
        t("ravager", Trait.STRONG, Trait.TOUGH);
        t("warden", Trait.STRONG, Trait.TOUGH, Trait.NIGHT_VISION);
        t("hoglin", Trait.STRONG);
        t("zoglin", Trait.STRONG);
        t("polar_bear", Trait.STRONG);
        t("enderman", Trait.NIGHT_VISION);
        t("shulker", Trait.TOUGH);

        a("enderman", Ability.TELEPORT);
        a("endermite", Ability.TELEPORT);
        a("blaze", Ability.SMALL_FIREBALL);
        a("ghast", Ability.FIREBALL);
        a("creeper", Ability.EXPLODE);
        a("breeze", Ability.WIND_CHARGE);
        a("warden", Ability.SONIC_BOOM);
        a("snow_golem", Ability.SNOWBALL);
        a("skeleton", Ability.ARROW);
        a("stray", Ability.ARROW);
        a("bogged", Ability.ARROW);
        a("shulker", Ability.LEVITATION_SHOT);
    }

    private MorphForms() {}

    private static void t(String path, Trait... traits) {
        TRAITS.computeIfAbsent("minecraft:" + path, k -> EnumSet.noneOf(Trait.class)).addAll(Set.of(traits));
    }

    private static void a(String path, Ability ability) {
        ABILITIES.put("minecraft:" + path, ability);
    }

    public static Optional<EntityType<?>> type(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return Optional.empty();
        return Optional.of(BuiltInRegistries.ENTITY_TYPE.getValue(key));
    }

    public static String idOf(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    /** ¿Se puede absorber esta forma? Tiene que ser un ser vivo con atributos y no estar bloqueada. */
    @SuppressWarnings("unchecked")
    public static boolean canMorphInto(EntityType<?> type) {
        String id = idOf(type);
        if (BLOCKED.contains(id)) return false;
        try {
            return DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) type);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public static Set<Trait> traits(String id) {
        Set<Trait> set = EnumSet.noneOf(Trait.class);
        Set<Trait> known = TRAITS.get(id);
        if (known != null) set.addAll(known);
        type(id).ifPresent(type -> {
            if (type.fireImmune()) set.add(Trait.FIRE_IMMUNE);
            MobCategory category = type.getCategory();
            if (category == MobCategory.WATER_CREATURE || category == MobCategory.WATER_AMBIENT
                    || category == MobCategory.UNDERGROUND_WATER_CREATURE || category == MobCategory.AXOLOTLS) {
                set.add(Trait.WATER);
            }
        });
        return set;
    }

    public static boolean has(String id, Trait trait) {
        return !id.isEmpty() && traits(id).contains(trait);
    }

    public static Ability ability(String id) {
        return ABILITIES.getOrDefault(id, Ability.NONE);
    }

    /** Vida máxima por defecto del mob (20 si no se sabe). */
    @SuppressWarnings("unchecked")
    public static double maxHealth(String id) {
        return type(id).map(type -> {
            try {
                var supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
                return supplier.hasAttribute(Attributes.MAX_HEALTH) ? supplier.getValue(Attributes.MAX_HEALTH) : 20.0D;
            } catch (RuntimeException e) {
                return 20.0D;
            }
        }).orElse(20.0D);
    }

    public static MorphData data(Player player) {
        return player.getData(DraxMorph.MORPH);
    }

    public static String current(Player player) {
        return data(player).current();
    }
}
