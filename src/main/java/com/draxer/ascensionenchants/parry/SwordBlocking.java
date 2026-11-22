package com.draxer.ascensionenchants.parry;

import com.draxer.ascensionenchants.AscensionEnchants;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Da a todas las espadas (vanilla y de otros mods cuyo id termine en "_sword") la capacidad de
 * bloquear con clic derecho, con la pose clásica de la 1.8. Un bloqueo normal con espada solo
 * absorbe la mitad del daño frontal; el parry perfecto lo maneja {@link ParryHandler}.
 */
@EventBusSubscriber(modid = AscensionEnchants.MODID)
public final class SwordBlocking {
    private SwordBlocking() {}

    @SubscribeEvent
    static void modifyComponents(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(
                (item, components) -> !components.has(DataComponents.BLOCKS_ATTACKS)
                        && BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_sword"),
                (components, context, item) -> components.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                        0.0F,                                   // sin demora: la guardia sube al instante
                        1.0F,                                   // las hachas la pueden desactivar como a un escudo
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 0.5F)), // 50 % del daño frontal
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 0.5F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK),
                        Optional.of(SoundEvents.SHIELD_BREAK))));
    }
}
