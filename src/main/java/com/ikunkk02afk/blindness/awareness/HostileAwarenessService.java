package com.ikunkk02afk.blindness.awareness;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HostileAwarenessService {
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int MAX_PROCESSED_ENTITIES = 32;

    private HostileAwarenessService() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL_TICKS != 0 || !BlindnessMod.serverConfig().hostileAwarenessEnabled()) return;
            for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive()
                    && BlindnessComponents.PLAYER.get(player).blindnessEnabled())) check(world, player);
        });
    }

    private static void check(ServerWorld world, ServerPlayerEntity player) {
        double range = BlindnessMod.serverConfig().hostileAwarenessRange();
        Box area = player.getBoundingBox().expand(range);
        List<MobEntity> nearby = world.getEntitiesByClass(MobEntity.class, area,
                mob -> mob.isAlive() && mob.squaredDistanceTo(player) <= range * range
                        && (mob instanceof HostileEntity || mob.getTarget() == player));
        Set<UUID> current = new HashSet<>();
        Set<UUID> targeting = new HashSet<>();
        boolean newThreat = false;
        boolean beganTargeting = false;
        double nearestSquared = Double.MAX_VALUE;
        PlayerRuntimeState state = BlindnessRuntime.get(player);

        int processed = 0;
        for (MobEntity mob : nearby) {
            if (processed++ >= MAX_PROCESSED_ENTITIES) break;
            UUID id = mob.getUuid();
            current.add(id);
            newThreat |= !state.nearbyThreats.contains(id);
            if (mob.getTarget() == player) {
                targeting.add(id);
                beganTargeting |= !state.targetingThreats.contains(id);
            }
            nearestSquared = Math.min(nearestSquared, mob.squaredDistanceTo(player));
        }

        long now = world.getTime();
        int cooldown = Math.max(20, (int) Math.round(BlindnessMod.serverConfig().hostileWarningCooldownSeconds() * 20.0));
        boolean critical = beganTargeting || nearestSquared <= 9.0;
        int requiredCooldown = critical ? Math.max(20, cooldown / 2) : cooldown;
        if (!current.isEmpty() && (newThreat || beganTargeting || critical) && now - state.lastHostileWarningAt >= requiredCooldown) {
            int volume = nearestSquared == Double.MAX_VALUE ? 55
                    : (int) Math.clamp(100.0 - Math.sqrt(nearestSquared) / range * 50.0, 45.0, 100.0);
            ServerPlayNetworking.send(player, new BlindnessPayloads.HostileWarning((byte) volume, critical));
            state.lastHostileWarningAt = now;
        }
        state.nearbyThreats.clear();
        state.nearbyThreats.addAll(current);
        state.targetingThreats.clear();
        state.targetingThreats.addAll(targeting);
    }
}
