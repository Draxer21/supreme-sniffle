package com.draxer.draxsort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * "Buscar en cofres cercanos": el cliente envía el texto del buscador, el servidor revisa los
 * contenedores a {@link #RADIUS} bloques y devuelve las posiciones de los que tienen coincidencias.
 * El cliente las hace brillar unos segundos (ver {@link Highlights}).
 */
public final class NearbySearch {
    public static final int RADIUS = 16;

    private NearbySearch() {}

    /** Cliente -> servidor. */
    public record Query(String text) implements CustomPacketPayload {
        public static final Type<Query> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "search_query"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Query> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Query::text, Query::new);

        @Override
        public Type<Query> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente. */
    public record Result(List<BlockPos> positions) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(Identifier.fromNamespaceAndPath(DraxSort.MODID, "search_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), Result::positions, Result::new);

        @Override
        public Type<Result> type() {
            return TYPE;
        }
    }

    /** Posiciones a resaltar en el cliente. Sin clases de cliente: se puede cargar en el servidor sin problema. */
    public static final class Highlights {
        private static volatile List<BlockPos> positions = List.of();
        private static volatile long untilMillis = 0L;

        private Highlights() {}

        public static void set(List<BlockPos> found) {
            positions = List.copyOf(found);
            untilMillis = System.currentTimeMillis() + 10_000L; // 10 segundos
        }

        public static List<BlockPos> active() {
            return System.currentTimeMillis() < untilMillis ? positions : List.of();
        }
    }

    public static void search(ServerPlayer player, String rawText) {
        String query = rawText.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty() || query.length() > 50) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        List<BlockPos> found = new ArrayList<>();
        for (BlockEntity be : Depositor.nearbyContainers(player, level, RADIUS)) {
            Container container = (Container) be;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && matches(stack, query)) {
                    found.add(be.getBlockPos().immutable());
                    break;
                }
            }
        }
        PacketDistributor.sendToPlayer(player, new Result(found));
        player.sendOverlayMessage(found.isEmpty()
                ? Component.translatable("drax_sort.search.nearby_none", rawText.trim(), RADIUS)
                : Component.translatable("drax_sort.search.nearby_found", found.size(), rawText.trim()));
    }

    public static boolean matches(ItemStack stack, String query) {
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(query);
    }
}
