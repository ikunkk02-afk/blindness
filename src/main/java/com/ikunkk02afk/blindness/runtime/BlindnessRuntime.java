package com.ikunkk02afk.blindness.runtime;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlindnessRuntime {
    private static final Map<UUID, PlayerRuntimeState> STATES = new ConcurrentHashMap<>();

    private BlindnessRuntime() {}

    public static PlayerRuntimeState get(ServerPlayerEntity player) {
        PlayerRuntimeState state = STATES.computeIfAbsent(player.getUuid(), ignored -> new PlayerRuntimeState());
        if (state.worldKey == null || !state.worldKey.equals(player.getWorld().getRegistryKey())) {
            state.resetTransient(player.getWorld().getRegistryKey());
        }
        return state;
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public static void addScanned(ServerPlayerEntity player, BlockPos pos, long expiryTick, int maxNodes) {
        PlayerRuntimeState state = get(player);
        prune(state, player.getServerWorld().getTime());
        state.scannedPath.put(pos.asLong(), expiryTick, maxNodes);
    }

    public static boolean isScanned(ServerPlayerEntity player, BlockPos pos, long tick) {
        PlayerRuntimeState state = get(player);
        prune(state, tick);
        for (int dy = -1; dy <= 1; dy++) {
            if (state.scannedPath.contains(pos.add(0, dy, 0).asLong())) return true;
        }
        return false;
    }

    public static void prune(PlayerRuntimeState state, long tick) {
        state.scannedPath.prune(tick);
    }
}
