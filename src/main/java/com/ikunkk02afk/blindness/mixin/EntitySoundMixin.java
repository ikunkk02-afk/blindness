package com.ikunkk02afk.blindness.mixin;

import com.ikunkk02afk.blindness.awareness.EntitySoundRevealService;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"))
    private void blindness$captureEntitySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld() instanceof ServerWorld serverWorld) {
            EntitySoundRevealService.handleSound(serverWorld, self, sound, volume, pitch);
        }
    }
}
