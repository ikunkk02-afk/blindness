package com.ikunkk02afk.blindness.client.mixin;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubtitlesHud.class)
public abstract class SubtitlesHudMixin {
    @Unique private static final ThreadLocal<SoundCategory> BLINDNESS_CATEGORY = new ThreadLocal<>();

    @Inject(method = "onSoundPlayed", at = @At("HEAD"))
    private void blindness$rememberCategory(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        BLINDNESS_CATEGORY.set(sound.getCategory());
    }

    @ModifyArg(method = "onSoundPlayed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/SubtitlesHud$SubtitleEntry;<init>(Lnet/minecraft/text/Text;FLnet/minecraft/util/math/Vec3d;)V"),
            index = 0)
    private Text blindness$blurEntitySubtitle(Text original) {
        if (!blindness$blurEnabled()) return original;
        SoundCategory category = BLINDNESS_CATEGORY.get();
        if (category == SoundCategory.HOSTILE) return Text.translatable("subtitles.blindness.hostile");
        if (category == SoundCategory.NEUTRAL) return Text.translatable("subtitles.blindness.creature");
        return original;
    }

    @ModifyArg(method = "onSoundPlayed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/SubtitlesHud$SubtitleEntry;<init>(Lnet/minecraft/text/Text;FLnet/minecraft/util/math/Vec3d;)V"),
            index = 2)
    private Vec3d blindness$blurHostileDirection(Vec3d original) {
        if (!blindness$blurEnabled() || BLINDNESS_CATEGORY.get() != SoundCategory.HOSTILE) return original;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return original;
        Vec3d relative = original.subtract(client.player.getPos());
        double angle = Math.toRadians(25.0 + Math.floorMod(original.hashCode(), 21));
        if ((original.hashCode() & 1) == 0) angle = -angle;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return client.player.getPos().add(relative.x * cos - relative.z * sin, relative.y,
                relative.x * sin + relative.z * cos);
    }

    @Inject(method = "onSoundPlayed", at = @At("RETURN"))
    private void blindness$clearCategory(SoundInstance sound, WeightedSoundSet soundSet, float range, CallbackInfo ci) {
        BLINDNESS_CATEGORY.remove();
    }

    @Unique
    private static boolean blindness$blurEnabled() {
        MinecraftClient client = MinecraftClient.getInstance();
        return BlindnessClient.CONFIG.blurEntitySubtitles() && client.player != null
                && BlindnessComponents.PLAYER.maybeGet(client.player)
                .map(component -> component.blindnessEnabled()).orElse(false);
    }
}
