package com.draxer.ascension.client;

import com.draxer.ascension.AscensionMod;
import com.draxer.ascension.block.AscensionTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * Dibuja el libro sobre la mesa como en la mesa de encantamientos vanilla:
 * se abre y mira al jugador cuando está cerca, pasa páginas, y se cierra y gira lento cuando se aleja.
 *
 * Piezas (modelos de ítem):
 *  - ascension:tome_half -> una mitad del libro (tapa + páginas) con la bisagra en el origen; se dibuja dos veces, en espejo.
 *  - ascension:tome_page -> la hoja que se pasa.
 */
public class FloatingTomeRenderer implements BlockEntityRenderer<AscensionTableBlockEntity, FloatingTomeRenderer.State> {
    private static final float CLOSED_ANGLE = (float) Math.toRadians(88.0D); // cada mitad, libro cerrado
    private static final float OPEN_ANGLE = (float) Math.toRadians(12.0D);   // cada mitad, libro abierto
    private static final float BOOK_TILT = (float) Math.toRadians(-80.0D);   // de pie, un poco reclinado
    private static final float SCALE = 0.8F;

    private final ItemModelResolver itemModelResolver;
    private @Nullable ItemStack half;
    private @Nullable ItemStack page;

    public FloatingTomeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AscensionTableBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumblingOverlay);

        state.time = be.time + partialTick;
        state.open = be.oOpen + (be.open - be.oOpen) * partialTick;
        state.rot = be.oRot + AscensionTableBlockEntity.wrap(be.rot - be.oRot) * partialTick;
        state.flip = be.oFlip + (be.flip - be.oFlip) * partialTick;

        if (this.half == null) {
            this.half = new ItemStack(AscensionMod.TOME_HALF.get());
            this.page = new ItemStack(AscensionMod.TOME_PAGE.get());
        }
        state.half = this.resolve(this.half, be);
        state.page = this.resolve(this.page, be);
    }

    private ItemStackRenderState resolve(ItemStack stack, AscensionTableBlockEntity be) {
        ItemStackRenderState item = new ItemStackRenderState();
        this.itemModelResolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, be.getLevel(), be, 0);
        return item;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.half == null) return;

        float bob = (float) Math.sin(state.time * 0.1F) * 0.03F;
        // El lomo queda vertical y las páginas miran hacia -Z; este giro apunta -Z hacia el jugador.
        float yaw = -state.rot - (float) (Math.PI / 2.0D);
        float halfAngle = CLOSED_ANGLE + (OPEN_ANGLE - CLOSED_ANGLE) * state.open;

        pose.pushPose();
        pose.translate(0.5F, 1.08F + bob, 0.5F);
        pose.mulPose(new Quaternionf().rotationY(yaw));
        pose.mulPose(new Quaternionf().rotationX(BOOK_TILT));
        pose.scale(SCALE, SCALE, SCALE);

        // Mitad derecha
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationZ(halfAngle));
        state.half.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // Mitad izquierda (espejo)
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationY((float) Math.PI));
        pose.mulPose(new Quaternionf().rotationZ(halfAngle));
        state.half.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();

        // Hoja que se pasa (solo con el libro abierto)
        if (state.page != null && state.open > 0.15F) {
            float f = state.flip + 0.25F;
            float frac = f - (float) Math.floor(f);
            float progress = Math.max(0.0F, Math.min(1.0F, frac * 1.6F - 0.3F));
            float pageAngle = halfAngle + progress * ((float) Math.PI - 2.0F * halfAngle);
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationZ(pageAngle));
            state.page.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        pose.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        float time;
        float open;
        float rot;
        float flip;
        @Nullable ItemStackRenderState half;
        @Nullable ItemStackRenderState page;
    }
}
