package com.ikunkk02afk.blindness.awareness;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public final class CliffDetectionService {
    public static final double MAX_DISTANCE = 4.0;
    private static final double SAMPLE_STEP = 0.5;
    private static final double STEP_TOLERANCE = 1.25;

    private CliffDetectionService() {}

    public static CliffRisk detectAndWarn(ServerPlayerEntity player, float yawOffset) {
        if (!BlindnessMod.serverConfig().cliffWarningsEnabled()
                || !BlindnessComponents.PLAYER.get(player).blindnessEnabled()) return CliffRisk.NONE;
        Detection detection = detect(player, yawOffset);
        if (detection.risk() == CliffRisk.NONE) return CliffRisk.NONE;

        PlayerRuntimeState state = BlindnessRuntime.get(player);
        long now = player.getServerWorld().getTime();
        int cooldown = Math.max(1, (int) Math.round(BlindnessMod.serverConfig().cliffWarningCooldownSeconds() * 20.0));
        int yawBucket = MathHelper.floorMod(Math.round((player.getYaw() + yawOffset) / 30F), 12);
        long fingerprint = detection.position().asLong() * 31L + yawBucket;
        boolean sameHazardCooling = state.cliffWarningFingerprint == fingerprint && now - state.lastCliffWarningAt < cooldown;
        boolean escalation = detection.risk().isSevere() && state.lastCliffRisk == CliffRisk.DROP.ordinal();
        if (!sameHazardCooling || escalation) {
            state.cliffWarningFingerprint = fingerprint;
            state.lastCliffWarningAt = now;
            state.lastCliffRisk = detection.risk().ordinal();
            ServerPlayNetworking.send(player, new BlindnessPayloads.CliffWarning((byte) detection.risk().ordinal()));
        }
        return detection.risk();
    }

    static Detection detect(ServerPlayerEntity player, float yawOffset) {
        Vec3d horizontal = Vec3d.fromPolar(0F, player.getYaw() + yawOffset).normalize();
        double unobstructed = unobstructedDistance(player, horizontal);
        Optional<Surface> starting = findSurface(player, player.getX(), player.getZ(), player.getY() + 1.0,
                BlindnessMod.serverConfig().severeCliffDropThreshold() + 3);
        if (starting.isEmpty()) return new Detection(CliffRisk.NONE, player.getBlockPos());
        double previousHeight = starting.get().height();
        BlockPos lastSafe = starting.get().pos();

        for (double distance = SAMPLE_STEP; distance <= unobstructed + 0.001; distance += SAMPLE_STEP) {
            double x = player.getX() + horizontal.x * distance;
            double z = player.getZ() + horizontal.z * distance;
            Optional<Surface> next = findSurface(player, x, z, previousHeight + 1.0,
                    BlindnessMod.serverConfig().severeCliffDropThreshold() + 3);
            if (next.isEmpty()) {
                BlockPos probe = BlockPos.ofFloored(x, previousHeight, z);
                CliffRisk risk = probe.getY() <= player.getWorld().getBottomY() + 2 ? CliffRisk.VOID : CliffRisk.SEVERE_DROP;
                return new Detection(risk, lastSafe);
            }
            Surface surface = next.get();
            if (surface.hazard()) return new Detection(CliffRisk.LAVA, surface.pos());
            double drop = previousHeight - surface.height();
            if (drop >= BlindnessMod.serverConfig().severeCliffDropThreshold() - 0.01) {
                return new Detection(CliffRisk.SEVERE_DROP, lastSafe);
            }
            if (drop >= BlindnessMod.serverConfig().cliffDropThreshold() - 0.01) {
                return new Detection(CliffRisk.DROP, lastSafe);
            }
            if (drop <= STEP_TOLERANCE) {
                previousHeight = surface.height();
                lastSafe = surface.pos();
            }
        }
        return new Detection(CliffRisk.NONE, lastSafe);
    }

    private static double unobstructedDistance(ServerPlayerEntity player, Vec3d direction) {
        double limit = MAX_DISTANCE;
        Vec3d[] starts = {
                player.getEyePos(),
                new Vec3d(player.getX(), player.getY() + 0.65, player.getZ())
        };
        for (Vec3d start : starts) {
            BlockHitResult hit = player.getWorld().raycast(new RaycastContext(start, start.add(direction.multiply(MAX_DISTANCE)),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) limit = Math.min(limit, Math.max(0, hit.getPos().distanceTo(start) - 0.15));
        }
        return limit;
    }

    private static Optional<Surface> findSurface(ServerPlayerEntity player, double x, double z,
                                                  double referenceHeight, int searchDepth) {
        int topY = MathHelper.floor(referenceHeight + 0.75);
        int bottomY = Math.max(player.getWorld().getBottomY(), MathHelper.floor(referenceHeight) - searchDepth);
        boolean hazard = false;
        for (int y = topY; y >= bottomY; y--) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            BlockState state = player.getWorld().getBlockState(pos);
            hazard |= state.getFluidState().isIn(FluidTags.LAVA) || state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE);
            VoxelShape shape = state.getCollisionShape(player.getWorld(), pos, ShapeContext.of(player));
            if (shape.isEmpty()) continue;
            double localX = x - pos.getX();
            double localZ = z - pos.getZ();
            double bestTop = Double.NEGATIVE_INFINITY;
            for (Box box : shape.getBoundingBoxes()) {
                if (localX >= box.minX - 0.001 && localX <= box.maxX + 0.001
                        && localZ >= box.minZ - 0.001 && localZ <= box.maxZ + 0.001) {
                    bestTop = Math.max(bestTop, pos.getY() + box.maxY);
                }
            }
            if (bestTop > Double.NEGATIVE_INFINITY) {
                BlockPos feet = BlockPos.ofFloored(x, bestTop + 0.01, z);
                BlockState feetState = player.getWorld().getBlockState(feet);
                hazard |= feetState.getFluidState().isIn(FluidTags.LAVA)
                        || feetState.isOf(Blocks.FIRE) || feetState.isOf(Blocks.SOUL_FIRE);
                return Optional.of(new Surface(pos.toImmutable(), bestTop, hazard));
            }
        }
        if (hazard) {
            return Optional.of(new Surface(BlockPos.ofFloored(x, referenceHeight - searchDepth, z),
                    referenceHeight - searchDepth, true));
        }
        return Optional.empty();
    }

    record Detection(CliffRisk risk, BlockPos position) {}
    private record Surface(BlockPos pos, double height, boolean hazard) {}
}
