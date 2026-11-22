package com.draxer.ascensionenchants;

import com.draxer.ascensionenchants.parry.ParryFlash;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Ascension Enchantments: encantamientos nuevos y sistema de parry.
 * Los encantamientos se definen en data/ascension_enchants/enchantment/*.json; la lógica que
 * necesita código está en los paquetes enchant y parry (se registra sola con @EventBusSubscriber).
 */
@Mod(AscensionEnchants.MODID)
public class AscensionEnchants {
    public static final String MODID = "ascension_enchants";

    public AscensionEnchants(IEventBus modEventBus) {
        modEventBus.addListener(AscensionEnchants::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): un cliente sin el mod (o con una versión vieja) puede seguir entrando a un servidor con el mod.
        event.registrar("1").optional()
                .playToClient(ParryFlash.TYPE, ParryFlash.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ParryFlash.trigger(payload.strength())));
    }
}
