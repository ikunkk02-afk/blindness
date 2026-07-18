package com.ikunkk02afk.blindness.client.animation;

import com.ikunkk02afk.blindness.BlindnessMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

public final class BlindnessAnimations {
    private static final Identifier LAYER = BlindnessMod.id("animation_layer");
    private static final String[] NAMES = {"fall_forward", "fall_backward", "get_up", "cane_tap", "cane_sweep"};

    private BlindnessAnimations() {}

    public static void register() {
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, stack) -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            stack.addAnimLayer(1000, layer);
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(LAYER, layer);
        });
    }

    @SuppressWarnings("unchecked")
    public static void play(int entityId, byte animationIndex) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || animationIndex < 0 || animationIndex >= NAMES.length) return;
        if (!(client.world.getEntityById(entityId) instanceof AbstractClientPlayerEntity player)) return;
        var stored = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
        if (!(stored instanceof ModifierLayer<?> rawLayer)) return;
        var playable = PlayerAnimationRegistry.getAnimation(BlindnessMod.id(NAMES[animationIndex]));
        if (playable == null) {
            BlindnessMod.LOGGER.warn("Missing player animation blindness:{}", NAMES[animationIndex]);
            return;
        }
        ((ModifierLayer<IAnimation>) rawLayer).setAnimation(playable.playAnimation());
    }
}
