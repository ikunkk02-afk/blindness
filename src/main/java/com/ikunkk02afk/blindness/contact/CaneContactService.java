package com.ikunkk02afk.blindness.contact;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.item.GuidanceCaneItem;
import com.ikunkk02afk.blindness.item.ModItems;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import com.ikunkk02afk.blindness.awareness.CliffDetectionService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CaneContactService {
    private static final double MAX_CONTACT_DISTANCE = 4.0;
    private static final double MAX_REVEAL_DISTANCE_SQUARED = 7.0 * 7.0;

    private CaneContactService() {}

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GuidanceCaneItem.removeSlowdown(handler.player);
            BlindnessRuntime.clear(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BlindnessRuntime.clearAll());
    }

    public static boolean performContact(ServerPlayerEntity player, float yawOffset, boolean sweepContact) {
        if ((!player.getMainHandStack().isOf(ModItems.GUIDANCE_CANE)
                && !player.getOffHandStack().isOf(ModItems.GUIDANCE_CANE))
                || player.getItemCooldownManager().isCoolingDown(ModItems.GUIDANCE_CANE)) return false;
        CliffDetectionService.detectAndWarn(player, yawOffset);
        Vec3d start = player.getEyePos();
        Vec3d direction = Vec3d.fromPolar(player.getPitch(), player.getYaw() + yawOffset);
        BlockHitResult hit = raycast(player, start, start.add(direction.multiply(MAX_CONTACT_DISTANCE)));
        if (hit.getType() == HitResult.Type.MISS) {
            if (!sweepContact) playMiss(player);
            return false;
        }

        BlockPos center = hit.getBlockPos().toImmutable();
        List<RevealCandidate> candidates = buildCandidates(player, center, hit.getSide());
        if (candidates.isEmpty()) {
            if (!sweepContact) playMiss(player);
            return false;
        }

        long now = player.getServerWorld().getTime();
        PlayerRuntimeState runtime = BlindnessRuntime.get(player);
        runtime.lastContactAt = now;
        int ttl = BlindnessMod.serverConfig().contactPathTtlTicks();
        int maxPathNodes = BlindnessMod.serverConfig().maxPathNodes();
        for (RevealCandidate candidate : candidates) {
            BlockPos pos = candidate.pos();
            if (ContactRevealMath.isDirectContactOffset(
                    pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ())) {
                BlindnessRuntime.addScanned(player, pos, now + ttl, maxPathNodes);
            }
        }

        var component = BlindnessComponents.PLAYER.get(player);
        component.incrementSuccessfulScans();
        component.setCaneProficiency(component.caneProficiency() + 1);
        int sequence = ++runtime.nextContactSequence;
        List<BlindnessPayloads.ContactEntry> entries = candidates.stream()
                .map(candidate -> BlindnessPayloads.ContactEntry.relativeTo(
                        center, candidate.pos(), candidate.center(), candidate.visibleFaces()))
                .toList();
        ServerPlayNetworking.send(player, new BlindnessPayloads.ContactReveal(sequence, center, entries));
        playMaterialFeedback(player, center, sweepContact ? 0.55F : 0.8F);
        return true;
    }

    public static void playTapAnimation(ServerPlayerEntity player) {
        com.ikunkk02afk.blindness.network.BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.Animation(player.getId(), (byte) 3, 10));
    }

    public static void playSweepAnimation(ServerPlayerEntity player) {
        com.ikunkk02afk.blindness.network.BlindnessNetworking.sendToTrackingAndSelf(player,
                new BlindnessPayloads.Animation(player.getId(), (byte) 4, 20));
    }

    private static List<RevealCandidate> buildCandidates(ServerPlayerEntity player, BlockPos center, Direction hitFace) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (BlockPos offset : ContactRevealMath.LOCAL_OFFSETS) positions.add(center.add(offset));

        BlockPos structureMate = null;
        for (BlockPos pos : List.copyOf(positions)) {
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
                structureMate = state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
                break;
            }
        }
        if (structureMate != null) positions.add(structureMate);

        List<RevealCandidate> result = new ArrayList<>(ContactRevealMath.MAX_REVEALS_PER_CONTACT);
        for (BlockPos pos : positions) {
            if (result.size() >= ContactRevealMath.MAX_REVEALS_PER_CONTACT) break;
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.isAir() || pos.getSquaredDistance(player.getEyePos()) > MAX_REVEAL_DISTANCE_SQUARED) continue;
            VoxelShape shape = revealShape(player, pos, state);
            if (shape.isEmpty()) continue;
            int visibleFaces = pos.equals(center) ? 1 << hitFace.getId() : visibleFaceMask(player, pos, shape);
            if (visibleFaces == 0) continue;
            result.add(new RevealCandidate(pos.toImmutable(), pos.equals(center), visibleFaces));
        }
        result.sort(Comparator.comparing(RevealCandidate::center).reversed()
                .thenComparingDouble(candidate -> candidate.pos().getSquaredDistance(player.getEyePos()))
                .thenComparingLong(candidate -> candidate.pos().asLong()));
        return result.size() <= ContactRevealMath.MAX_REVEALS_PER_CONTACT
                ? List.copyOf(result)
                : List.copyOf(result.subList(0, ContactRevealMath.MAX_REVEALS_PER_CONTACT));
    }

    private static VoxelShape revealShape(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getOutlineShape(player.getWorld(), pos, ShapeContext.of(player));
        if (shape.isEmpty() && !state.getFluidState().isEmpty()) {
            shape = state.getFluidState().getShape(player.getWorld(), pos);
        }
        return shape;
    }

    private static int visibleFaceMask(ServerPlayerEntity player, BlockPos pos, VoxelShape shape) {
        Box box = shape.getBoundingBox().offset(pos);
        Vec3d eye = player.getEyePos();
        int mask = 0;
        for (Direction direction : Direction.values()) {
            Vec3d sample = faceCenter(box, direction).add(
                    direction.getOffsetX() * -0.01,
                    direction.getOffsetY() * -0.01,
                    direction.getOffsetZ() * -0.01);
            BlockHitResult visibility = raycast(player, eye, sample);
            if (visibility.getType() == HitResult.Type.BLOCK && visibility.getBlockPos().equals(pos)) {
                mask |= 1 << direction.getId();
            }
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

    private static BlockHitResult raycast(ServerPlayerEntity player, Vec3d start, Vec3d end) {
        return player.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY, player));
    }

    private static void playMiss(ServerPlayerEntity player) {
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 0.25F, 1.35F);
    }

    private static void playMaterialFeedback(ServerPlayerEntity player, BlockPos pos, float volume) {
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
        player.getWorld().playSound(null, pos, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private record RevealCandidate(BlockPos pos, boolean center, int visibleFaces) {}
}
