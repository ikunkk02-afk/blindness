package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.accessibility.EyeOfEnderEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Persists the thrower UUID in NBT and exposes {@link #dropsItem} via the
 * {@link EyeOfEnderEntityAccessor} interface.
 * <p>
 * End-of-flight notification is handled by {@link EntityDiscardMixin} which
 * intercepts {@link Entity#discard()} (declared {@code final} on the parent)
 * — that avoids the bytecode-level {@code INVOKESPECIAL} vs {@code INVOKEVIRTUAL}
 * mismatch that would break a {@code @At(INVOKE)} target inside {@code tick()}.
 */
@Mixin(EyeOfEnderEntity.class)
public abstract class EyeOfEnderEntityMixin extends Entity implements EyeOfEnderEntityAccessor {

    @Unique private UUID throwerUuid;
    @Shadow private boolean dropsItem;

    public EyeOfEnderEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void blindness$setThrowerUuid(UUID uuid) {
        this.throwerUuid = uuid;
    }

    @Override
    public UUID blindness$getThrowerUuid() {
        return throwerUuid;
    }

    @Override
    public boolean blindness$getDropsItem() {
        return dropsItem;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void blindness$readThrowerUuid(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.containsUuid("BlindnessThrower")) {
            throwerUuid = nbt.getUuid("BlindnessThrower");
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void blindness$writeThrowerUuid(NbtCompound nbt, CallbackInfo ci) {
        if (throwerUuid != null) {
            nbt.putUuid("BlindnessThrower", throwerUuid);
        }
    }
}
