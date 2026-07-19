package com.ikunkk02afk.blindness.awareness;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EntitySoundRevealService {
    public static final int MAX_SOUND_REVEALS = 12;

    private EntitySoundRevealService() {}

    public static void handleSound(ServerWorld world, Entity source, SoundEvent sound, float volume, float pitch) {
        if (!BlindnessMod.serverConfig().entitySoundRevealEnabled() || !(source instanceof LivingEntity)
                || source instanceof PlayerEntity || source.isSilent()) return;
        EntitySoundCategory category = classify(sound);
        if (category == null) return;
        double configuredRange = BlindnessMod.serverConfig().entitySoundRevealMaximumDistance();
        double audibleRange = Math.min(configuredRange, Math.max(3.0, 8.0 * Math.max(0.25, volume)));
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive()
                && BlindnessComponents.PLAYER.get(p).blindnessEnabled()
                && p.squaredDistanceTo(source) <= audibleRange * audibleRange)) {
            if (!acceptRate(player, category, world.getTime())) continue;
            List<BlindnessPayloads.SoundRevealEntry> entries = collectVisibleBlocks(player, source, category);
            if (!entries.isEmpty()) {
                ServerPlayNetworking.send(player, new BlindnessPayloads.EntitySoundReveal(
                        source.getBlockPos(), (byte) category.revealSource().ordinal(), entries));
            }
        }
    }

    private static boolean acceptRate(ServerPlayerEntity player, EntitySoundCategory category, long tick) {
        PlayerRuntimeState state = BlindnessRuntime.get(player);
        int window = (int) (tick / 20L);
        if (state.soundRateWindow != window) {
            state.soundRateWindow = window;
            state.soundEventsInWindow = 0;
        }
        int maximum = BlindnessMod.serverConfig().entitySoundMaximumEventsPerSecond();
        if (state.soundEventsInWindow >= maximum && category.revealSource() != RevealSource.ENTITY_DANGER) return false;
        if (state.soundEventsInWindow >= maximum + 2) return false;
        state.soundEventsInWindow++;
        return true;
    }

    private static List<BlindnessPayloads.SoundRevealEntry> collectVisibleBlocks(
            ServerPlayerEntity player, Entity source, EntitySoundCategory category) {
        BlockPos center = source.getBlockPos();
        int maximum = switch (category.revealSource()) {
            case ENTITY_FOOTSTEP -> 7;
            case ENTITY_AMBIENT -> 10;
            case ENTITY_DANGER -> 12;
            default -> 0;
        };
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(center.down());
        for (Direction direction : Direction.Type.HORIZONTAL) positions.add(center.down().offset(direction));
        if (category.revealSource() != RevealSource.ENTITY_FOOTSTEP) {
            positions.add(center);
            positions.add(center.up());
            for (Direction direction : Direction.Type.HORIZONTAL) positions.add(center.offset(direction));
        }
        if (category.revealSource() == RevealSource.ENTITY_DANGER) {
            for (Direction direction : Direction.Type.HORIZONTAL) positions.add(center.up().offset(direction));
        }

        List<Candidate> visible = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (visible.size() >= MAX_SOUND_REVEALS
                    || !player.getServerWorld().isChunkLoaded(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) continue;
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getOutlineShape(player.getWorld(), pos, ShapeContext.of(player));
            if (shape.isEmpty() && !state.getFluidState().isEmpty()) shape = state.getFluidState().getShape(player.getWorld(), pos);
            if (shape.isEmpty()) continue;
            int faces = visibleFaceMask(player, pos, shape);
            if (faces != 0) visible.add(new Candidate(pos.toImmutable(), faces));
        }
        visible.sort(Comparator.comparingDouble(candidate -> candidate.pos().getSquaredDistance(source.getPos())));
        return visible.stream().limit(maximum)
                .map(candidate -> BlindnessPayloads.SoundRevealEntry.relativeTo(center, candidate.pos(), candidate.faces()))
                .toList();
    }

    private static int visibleFaceMask(ServerPlayerEntity player, BlockPos pos, VoxelShape shape) {
        Box box = shape.getBoundingBox().offset(pos);
        Vec3d eye = player.getEyePos();
        int mask = 0;
        for (Direction direction : Direction.values()) {
            Vec3d sample = faceCenter(box, direction).add(direction.getOffsetX() * -0.01,
                    direction.getOffsetY() * -0.01, direction.getOffsetZ() * -0.01);
            BlockHitResult result = player.getWorld().raycast(new RaycastContext(eye, sample,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, player));
            if (result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) mask |= 1 << direction.getId();
        }
        return mask;
    }

    private static Vec3d faceCenter(Box box, Direction direction) {
        double x = (box.minX + box.maxX) * 0.5;
        double y = (box.minY + box.maxY) * 0.5;
        double z = (box.minZ + box.maxZ) * 0.5;
        return switch (direction) {
            case DOWN -> new Vec3d(x, box.minY, z);
            case UP -> new Vec3d(x, box.maxY, z);
            case NORTH -> new Vec3d(x, y, box.minZ);
            case SOUTH -> new Vec3d(x, y, box.maxZ);
            case WEST -> new Vec3d(box.minX, y, z);
            case EAST -> new Vec3d(box.maxX, y, z);
        };
    }

    static EntitySoundCategory classify(SoundEvent sound) {
        Identifier id = Registries.SOUND_EVENT.getId(sound);
        String path = id.getPath();
        if (path.contains("death")) return EntitySoundCategory.DEATH;
        if (path.contains("hurt")) return EntitySoundCategory.HURT;
        if (path.contains("attack") || path.contains("shoot") || path.contains("sting")) return EntitySoundCategory.ATTACK;
        if (path.contains("step")) return EntitySoundCategory.FOOTSTEP;
        if (path.contains("swim") || path.contains("splash") || path.contains("flap") || path.contains("fly")) {
            return EntitySoundCategory.MOVEMENT;
        }
        if (path.contains("ambient") || path.contains("idle") || path.contains("breathe")
                || path.contains("growl") || path.contains("hiss") || path.contains("warn")) {
            return EntitySoundCategory.AMBIENT;
        }
        return null;
    }

    private record Candidate(BlockPos pos, int faces) {}
}
