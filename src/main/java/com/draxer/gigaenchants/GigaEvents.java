package com.draxer.gigaenchants;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Fortuna y Botín vanilla viven en las tablas de botín, no en la definición del encantamiento,
 * así que sus versiones Giga se aplican aquí:
 *  - Giga Fortuna N: vuelve a calcular lo que suelta el bloque como si la herramienta tuviera Fortuna 2N.
 *  - Giga Botín N: cada ítem apilable que suelta el mob recibe entre 0 y 2N unidades extra.
 * Prioridad alta para que otros mods (por ejemplo Telequinesis) vean ya el botín final.
 */
@EventBusSubscriber(modid = GigaEnchants.MODID)
public final class GigaEvents {
    public static final ResourceKey<Enchantment> GIGA_FORTUNE = key("fortune");
    public static final ResourceKey<Enchantment> GIGA_LOOTING = key("looting");

    private GigaEvents() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(GigaEnchants.MODID, name));
    }

    private static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player)) return;
        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        int giga = level(level, GIGA_FORTUNE, tool);
        if (giga <= 0) return;

        Holder<Enchantment> fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        ItemStack boosted = tool.copy();
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(boosted));
        enchantments.set(fortune, giga * 2);
        EnchantmentHelper.setEnchantments(boosted, enchantments.toImmutable());

        BlockPos pos = event.getPos();
        List<ItemStack> drops = Block.getDrops(event.getState(), level, pos, event.getBlockEntity(), event.getBreaker(), boosted);

        event.getDrops().clear();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
            entity.setDefaultPickUpDelay();
            event.getDrops().add(entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        int giga = level(player.level(), GIGA_LOOTING, player.getMainHandItem());
        if (giga <= 0) return;

        RandomSource random = player.getRandom();
        for (ItemEntity entity : event.getDrops()) {
            ItemStack stack = entity.getItem();
            if (!stack.isStackable()) continue; // armas, armaduras y objetos únicos no se multiplican
            int extra = random.nextInt(2 * giga + 1);
            if (extra > 0) {
                stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() + extra));
                entity.setItem(stack);
            }
        }
    }
}
