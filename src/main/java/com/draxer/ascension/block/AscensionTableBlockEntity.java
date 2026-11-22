package com.draxer.ascension.block;

import com.draxer.ascension.AscensionMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Block entity: guarda los puntos de devolución de la mesa y lleva el estado de la animación del libro (solo en el cliente).
 * La lógica imita la de la mesa de encantamientos vanilla: el libro se abre y mira al jugador
 * cercano, pasa páginas, y se cierra y gira despacio cuando no hay nadie.
 */
public class AscensionTableBlockEntity extends BlockEntity implements ItemOwner {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float TWO_PI = (float) (Math.PI * 2.0D);

    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;

    /** Puntos guardados en la mesa (devoluciones al bajar niveles). Se gastan antes que los materiales. */
    private int credit = 0;

    public AscensionTableBlockEntity(BlockPos pos, BlockState state) {
        super(AscensionMod.ASCENSION_TABLE_BE.get(), pos, state);
    }

    public int credit() {
        return this.credit;
    }

    public void setCredit(int credit) {
        this.credit = Math.max(0, credit);
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("credit", Codec.INT, this.credit);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.credit = input.read("credit", Codec.INT).orElse(0);
    }

    /** Se llama cada tick, solo en el cliente. */
    public void clientTick() {
        Level level = this.getLevel();
        if (level == null) return;

        this.oOpen = this.open;
        this.oRot = this.rot;

        BlockPos pos = this.getBlockPos();
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        Player player = level.getNearestPlayer(cx, cy, cz, 3.0D, false);

        if (player != null) {
            this.tRot = (float) Math.atan2(player.getZ() - cz, player.getX() - cx);
            this.open += 0.1F;
            if (this.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float previous = this.flipT;
                do {
                    this.flipT += RANDOM.nextInt(4) - RANDOM.nextInt(4);
                } while (previous == this.flipT);
            }
        } else {
            this.tRot += 0.02F;
            this.open -= 0.1F;
        }

        this.rot = wrap(this.rot);
        this.tRot = wrap(this.tRot);
        this.rot += wrap(this.tRot - this.rot) * 0.4F;

        this.open = Math.max(0.0F, Math.min(1.0F, this.open));
        this.time++;

        this.oFlip = this.flip;
        float step = (this.flipT - this.flip) * 0.4F;
        step = Math.max(-0.2F, Math.min(0.2F, step));
        this.flipA += (step - this.flipA) * 0.9F;
        this.flip += this.flipA;
    }

    /** Lleva un ángulo al rango [-PI, PI). */
    public static float wrap(float angle) {
        while (angle >= Math.PI) angle -= TWO_PI;
        while (angle < -Math.PI) angle += TWO_PI;
        return angle;
    }

    // --- ItemOwner: el libro se dibuja con modelos de ítem ---

    @Override
    public Level level() {
        return this.getLevel();
    }

    @Override
    public Vec3 position() {
        return Vec3.atCenterOf(this.getBlockPos());
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return 0.0F;
    }
}
