package com.draxer.draxgrave;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Guarda los ítems del jugador muerto y quién es el dueño. */
public class GraveBlockEntity extends BlockEntity {
    private final List<ItemStack> items = new ArrayList<>();
    private @Nullable UUID owner;
    private String ownerName = "?";
    private int experience = 0;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(DraxGrave.GRAVE_BE.get(), pos, state);
    }

    public void fill(Player player, List<ItemStack> stacks) {
        this.owner = player.getUUID();
        this.ownerName = player.getName().getString();
        this.items.clear();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) this.items.add(stack.copy());
        }
        this.setChanged();
    }

    public void setExperience(int points) {
        this.experience = Math.max(0, points);
        this.setChanged();
    }

    public int experience() {
        return this.experience;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public int itemCount() {
        return this.items.size();
    }

    /** Pueden abrirla el dueño, alguien en creativo o quien tenga la brújula de esta tumba (la "llave"). */
    public boolean canOpen(Player player) {
        if (player.isCreative() || this.owner == null || this.owner.equals(player.getUUID())) return true;
        Level level = this.getLevel();
        return level != null && GraveCompass.has(player, level.dimension(), this.getBlockPos());
    }

    /** Devuelve los ítems: la armadura vuelve puesta si ese espacio está libre; el resto al inventario o al suelo. */
    public void giveBack(Player player) {
        for (ItemStack stack : this.items) {
            ItemStack copy = stack.copy();
            EquipmentSlot slot = player.getEquipmentSlotForItem(copy);
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, copy);
                continue;
            }
            if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                player.drop(copy, false);
            }
        }
        int count = this.items.size();
        this.items.clear();
        int xp = this.experience;
        this.experience = 0;
        if (xp > 0) player.giveExperiencePoints(xp);
        this.setChanged();
        if (this.getLevel() != null) GraveCompass.removeFrom(player, this.getLevel().dimension(), this.getBlockPos());

        Level level = this.getLevel();
        if (level instanceof ServerLevel server) {
            BlockPos p = this.getBlockPos();
            server.playSound(null, p, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.7F);
            server.playSound(null, p, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7F, 0.8F);
            server.sendParticles(ParticleTypes.SOUL, p.getX() + 0.5D, p.getY() + 0.8D, p.getZ() + 0.5D, 20, 0.3D, 0.3D, 0.3D, 0.03D);
        }
        player.sendOverlayMessage(xp > 0
                ? Component.translatable("drax_grave.recovered_xp", count, xp)
                : Component.translatable("drax_grave.recovered", count));
    }

    /** Si la tumba desaparece por otro motivo (comandos, otro mod), los ítems caen al suelo en vez de perderse. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            for (ItemStack stack : this.items) {
                Block.popResource(level, pos, stack.copy());
            }
            this.items.clear();
            if (this.experience > 0 && level instanceof ServerLevel server) {
                ExperienceOrb.award(server, Vec3.atCenterOf(pos), this.experience);
                this.experience = 0;
            }
        }
    }

    // ---------------------------------------------------------------------
    // Guardado
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.CODEC.listOf(), List.copyOf(this.items));
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
        output.store("owner_name", Codec.STRING, this.ownerName);
        output.store("experience", Codec.INT, this.experience);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        this.items.addAll(input.read("items", ItemStack.CODEC.listOf()).orElse(List.of()));
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.read("owner_name", Codec.STRING).orElse("?");
        this.experience = input.read("experience", Codec.INT).orElse(0);
    }
}
