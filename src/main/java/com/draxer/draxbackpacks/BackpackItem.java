package com.draxer.draxbackpacks;

import java.util.function.Consumer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Mochila. Clic derecho: abrirla. Agachado + clic derecho: activar/desactivar la recolección
 * automática (los ítems que recoges y que ya hay en la mochila entran directo en ella).
 */
public class BackpackItem extends Item {
    private final int rows;

    public BackpackItem(Properties properties, int rows) {
        super(properties);
        this.rows = rows;
    }

    public int rows() {
        return this.rows;
    }

    public int size() {
        return this.rows * 9;
    }

    public static boolean autoPickup(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            boolean on = !autoPickup(stack);
            if (on) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            player.sendOverlayMessage(Component.translatable(on ? "drax_backpacks.pickup.on" : "drax_backpacks.pickup.off"));
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new BackpackMenu(id, inventory, stack, hand, this.rows),
                stack.getHoverName()));
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(this.size(), ItemStack.EMPTY);
        contents.copyInto(items);
        int used = 0;
        for (ItemStack item : items) if (!item.isEmpty()) used++;
        tooltip.accept(Component.translatable("drax_backpacks.tooltip.slots", used, this.size()).withColor(0xA0A0A0));
        tooltip.accept(Component.translatable(autoPickup(stack) ? "drax_backpacks.tooltip.pickup_on" : "drax_backpacks.tooltip.pickup_off")
                .withColor(autoPickup(stack) ? 0x55FF55 : 0x909090));
    }
}
