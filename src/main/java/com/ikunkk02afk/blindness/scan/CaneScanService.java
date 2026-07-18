package com.ikunkk02afk.blindness.scan;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.item.GuidanceCaneItem;
import com.ikunkk02afk.blindness.network.BlindnessNetworking;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.registry.tag.BlockTags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CaneScanService {
    public static final byte MODE_TAP = 0;
    public static final byte MODE_SWEEP = 1;

    private CaneScanService() {}

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GuidanceCaneItem.removeSlowdown(handler.player);
            BlindnessRuntime.clear(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BlindnessRuntime.clearAll());
    }

    public static void performTap(ServerPlayerEntity player) {
        int range = BlindnessMod.serverConfig().tapRange();
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0F).multiply(range));
        BlockHitResult hit = raycast(player, start, end);
        if (hit.getType() == HitResult.Type.MISS) {
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_WOOD_HIT,
                    SoundCategory.PLAYERS, 0.35F, 0.65F);
            return;
        }
        List<ScanSurface> hits = List.of(new ScanSurface(hit.getBlockPos().toImmutable(), hit.getSide()));
        finishScan(player, hit.getBlockPos(), hits, MODE_TAP, range);
        playMaterialFeedback(player, hit.getBlockPos());
        BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.Animation(player.getId(), (byte) 3, 10));
    }

    public static void performSweep(ServerPlayerEntity player) {
        int range = BlindnessMod.serverConfig().sweepRange();
        int maxHits = Math.min(BlindnessPayloads.ScanResult.MAX_HITS, BlindnessMod.serverConfig().maxScanHits());
        Map<BlockPos, Direction> hitSet = new LinkedHashMap<>();
        Vec3d start = player.getEyePos();
        for (int yawStep = -6; yawStep <= 6 && hitSet.size() < maxHits; yawStep++) {
            float yaw = player.getYaw() + yawStep * 7.5F;
            for (float pitch : new float[]{-30F, -12F, 5F, 22F}) {
                Vec3d direction = Vec3d.fromPolar(pitch, yaw);
                BlockHitResult hit = raycast(player, start, start.add(direction.multiply(range)));
                if (hit.getType() != HitResult.Type.MISS) hitSet.putIfAbsent(hit.getBlockPos().toImmutable(), hit.getSide());
            }
        }
        for (int lane = -1; lane <= 1 && hitSet.size() < maxHits; lane++) {
            Vec3d forward = Vec3d.fromPolar(0, player.getYaw()).normalize();
            Vec3d side = new Vec3d(-forward.z, 0, forward.x).multiply(lane * 0.75);
            for (int step = 1; step <= range && hitSet.size() < maxHits; step++) {
                Vec3d probeStart = player.getPos().add(0, 1.2, 0).add(side).add(forward.multiply(step));
                BlockHitResult hit = raycast(player, probeStart, probeStart.add(0, -3.2, 0));
                if (hit.getType() != HitResult.Type.MISS) hitSet.putIfAbsent(hit.getBlockPos().toImmutable(), hit.getSide());
            }
        }
        if (hitSet.isEmpty()) return;
        List<ScanSurface> hits = hitSet.entrySet().stream()
                .map(entry -> new ScanSurface(entry.getKey(), entry.getValue()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (hits.size() > maxHits) hits = new ArrayList<>(hits.subList(0, maxHits));
        finishScan(player, player.getBlockPos(), hits, MODE_SWEEP, range);
        playMaterialFeedback(player, hits.getFirst().pos());
        BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.Animation(player.getId(), (byte) 4, 20));
    }

    private static void finishScan(ServerPlayerEntity player, BlockPos origin, List<ScanSurface> hits, byte mode, int radius) {
        PlayerRuntimeState runtime = BlindnessRuntime.get(player);
        long now = player.getServerWorld().getTime();
        runtime.lastScanAt = now;
        int ttl = BlindnessMod.serverConfig().scannedPathTtlTicks();
        for (ScanSurface hit : hits) {
            if (hit.pos().getSquaredDistance(player.getPos()) <= (radius + 2.0) * (radius + 2.0)) {
                BlindnessRuntime.addScanned(player, hit.pos(), now + ttl, BlindnessMod.serverConfig().maxPathNodes());
            }
        }
        var component = BlindnessComponents.PLAYER.get(player);
        component.incrementSuccessfulScans();
        component.setCaneProficiency(component.caneProficiency() + 1);
        int scanId = ++runtime.nextScanId;
        List<BlindnessPayloads.ScanHit> packetHits = hits.stream()
                .map(hit -> BlindnessPayloads.ScanHit.relativeTo(origin, hit.pos(), hit.face()))
                .toList();
        ServerPlayNetworking.send(player, new BlindnessPayloads.ScanResult(scanId, mode, origin, packetHits));
        ServerPlayNetworking.send(player, new BlindnessPayloads.ScanWave(scanId, origin, radius, 30, mode));
    }

    private static BlockHitResult raycast(PlayerEntity player, Vec3d start, Vec3d end) {
        return player.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.ANY, player));
    }

    private static void playMaterialFeedback(ServerPlayerEntity player, BlockPos pos) {
        BlockState state = player.getWorld().getBlockState(pos);
        BlockSoundGroup group = state.getSoundGroup();
        SoundEvent sound;
        float pitch;
        if (!state.getFluidState().isEmpty()) { sound = SoundEvents.ENTITY_GENERIC_SPLASH; pitch = 0.9F; }
        else if (group == BlockSoundGroup.GLASS) { sound = SoundEvents.BLOCK_GLASS_HIT; pitch = 1.45F; }
        else if (group == BlockSoundGroup.METAL || group == BlockSoundGroup.COPPER) { sound = SoundEvents.BLOCK_ANVIL_LAND; pitch = 1.65F; }
        else if (group == BlockSoundGroup.WOOD || group == BlockSoundGroup.BAMBOO_WOOD) { sound = SoundEvents.BLOCK_WOOD_HIT; pitch = 0.72F; }
        else if (state.isIn(BlockTags.LEAVES)) { sound = SoundEvents.BLOCK_GRASS_HIT; pitch = 1.15F; }
        else if (group == BlockSoundGroup.GRASS || group == BlockSoundGroup.SAND || group == BlockSoundGroup.GRAVEL) { sound = SoundEvents.BLOCK_GRAVEL_HIT; pitch = 0.62F; }
        else { sound = SoundEvents.BLOCK_STONE_HIT; pitch = 1.2F; }
        player.getWorld().playSound(null, pos, sound, SoundCategory.PLAYERS, 0.8F, pitch);
    }

    private record ScanSurface(BlockPos pos, Direction face) {}
}
