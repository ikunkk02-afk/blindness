package com.ikunkk02afk.blindness.component;

import com.ikunkk02afk.blindness.BlindnessMod;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class BlindnessComponents implements EntityComponentInitializer {
    public static final ComponentKey<BlindnessPlayerComponent> PLAYER = ComponentRegistry.getOrCreate(
            BlindnessMod.id("player"), BlindnessPlayerComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PLAYER, PlayerBlindnessComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}
