package com.draxer.draxmagnet;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Anillo imán. El estado (activo o no) se guarda como el brillo de encantamiento del ítem:
 * brillando = activo. Así se ve de un vistazo en el inventario.
 */
public class MagnetItem extends Item {
    private final double radius;

    public MagnetItem(Properties properties, double radius) {
        super(properties);
        this.radius = radius;
    }

    public double radius() {
        return this.radius;
    }

    public static boolean isActive(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    public static void setActive(ItemStack stack, boolean active) {
        if (active) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
    }

    public static void announce(Player player, boolean active) {
        player.sendOverlayMessage(Component.translatable(active ? "drax_magnet.on" : "drax_magnet.off"));
        player.level().playSound(null, player.blockPosition(), active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean active = !isActive(stack);
            setActive(stack, active);
            announce(player, active);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("drax_magnet.tooltip.radius", (int) this.radius).withColor(0xA0A0A0));
        tooltip.accept(Component.translatable(isActive(stack) ? "drax_magnet.tooltip.on" : "drax_magnet.tooltip.off")
                .withColor(isActive(stack) ? 0x55FF55 : 0xFF7777));
    }
}
