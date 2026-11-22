package com.draxer.draxmorph;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Drax Morph: mata un mob para absorber su forma; luego transfórmate en él (tecla G)
 * y usa su habilidad (tecla V).
 */
@Mod(DraxMorph.MODID)
public class DraxMorph {
    public static final String MODID = "drax_morph";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final Supplier<AttachmentType<MorphData>> MORPH = ATTACHMENTS.register("morph",
            () -> AttachmentType.builder(() -> MorphData.EMPTY)
                    .serialize(MorphData.CODEC)
                    .copyOnDeath()
                    .sync(MorphData.STREAM_CODEC)
                    .build());

    public DraxMorph(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(DraxMorph::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(MorphPayloads.Select.TYPE, MorphPayloads.Select.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) MorphActions.select(player, payload.id());
                        }))
                .playToServer(MorphPayloads.Ability.TYPE, MorphPayloads.Ability.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) MorphActions.useAbility(player);
                        }));
    }
}
