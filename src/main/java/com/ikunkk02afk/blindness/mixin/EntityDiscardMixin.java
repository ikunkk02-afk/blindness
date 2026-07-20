package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.accessibility.EnderEyeTracker;
import com.ikunkk02afk.blindness.accessibility.EyeOfEnderEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Intercepts {@link Entity#discard()} to detect when an
 * {@link EyeOfEnderEntity} finishes its flight.
 * <p>
 * {@code discard()} is {@code public final} on {@code Entity}. Injecting
 * at {@code HEAD} avoids bytecode-level {@code INVOKESPECIAL} vs
 * {@code INVOKEVIRTUAL} mismatches that break {@code @At(INVOKE)}
 * targets inside subclass methods.
 * <p>
 * The one-shot guard uses the thrower UUID itself: after notification the
 * UUID is cleared, so a second {@code discard()} on the same instance is
 * a no-op.
 */
@Mixin(Entity.class)
public abstract class EntityDiscardMixin {

    @Inject(method = "discard", at = @At("HEAD"))
    private void blindness$onDiscard(CallbackInfo ci) {
        if (!((Object) this instanceof EyeOfEnderEntity)) return;

        EyeOfEnderEntityAccessor accessor = (EyeOfEnderEntityAccessor) this;
        UUID throwerUuid = accessor.blindness$getThrowerUuid();
        if (throwerUuid == null) return;

        // One-shot: clear the UUID so we never fire twice for the same entity.
        accessor.blindness$setThrowerUuid(null);

        if (((Entity) (Object) this).getWorld() instanceof ServerWorld serverWorld) {
            EnderEyeTracker.onResult(
                    serverWorld,
                    (EyeOfEnderEntity) (Object) this,
                    throwerUuid,
                    accessor.blindness$getDropsItem()
            );
        }
    }
}
