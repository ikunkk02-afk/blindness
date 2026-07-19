package com.ikunkk02afk.blindness.awareness;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.component.BlindnessComponents;
import com.ikunkk02afk.blindness.component.BlindnessPlayerComponent;
import com.ikunkk02afk.blindness.network.BlindnessPayloads;
import com.ikunkk02afk.blindness.runtime.BlindnessRuntime;
import com.ikunkk02afk.blindness.runtime.PlayerRuntimeState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class EntitySoundRevealService {
    public static final int MAX_SOUND_REVEALS = 80;
    private static final int FOOTSTEP_MIN_INTERVAL_TICKS = 5;
    private static final Map<Entity, Long> LAST_FOOTSTEP = new WeakHashMap<>();
    private static final Map<SoundEventKey, Long> RECENT_EVENTS = new HashMap<>();
    private static final Set<String> REPORTED_UNREGISTERED_SOUNDS = new HashSet<>();

    private EntitySoundRevealService() {}

    public static void handleSound(ServerWorld world, Entity source, SoundEvent sound, float volume, float pitch) {
        if (world == null || source == null || sound == null) return;
        if (!BlindnessMod.serverConfig().entitySoundRevealEnabled() || !(source instanceof LivingEntity)
                || source instanceof PlayerEntity || source.isRemoved() || source.isSilent()
                || source.getWorld() != world || volume <= 0.02F) return;
        EntitySoundCategory category = classify(sound);
        long gameTime = world.getTime();
        if (category == null || isDuplicate(world, source, sound, gameTime)
                || !acceptEntityInterval(source, category, gameTime)) return;

        Vec3d soundPosition = soundPosition(source, category);
        ChunkPos sourceChunk = source.getChunkPos();
        if (!world.isChunkLoaded(sourceChunk.toLong())) return;
        boolean hostile = source instanceof HostileEntity || category.revealSource() == RevealSource.ENTITY_DANGER;
        SoundOcclusion occlusion;

        for (ServerPlayerEntity player : world.getPlayers(player -> player.isAlive()
                && BlindnessComponents.PLAYER.get(player).blindnessEnabled())) {
            BlindnessPlayerComponent component = BlindnessComponents.PLAYER.get(player);
            int listeningRadius = Math.min(component.listeningChunkRadius(),
                    BlindnessMod.serverConfig().maximumListeningChunkRadius());
            ChunkPos playerChunk = player.getChunkPos();
            if (!SoundAwarenessRules.isChunkInRange(playerChunk.x, playerChunk.z,
                    sourceChunk.x, sourceChunk.z, listeningRadius)) continue;
            if (!acceptPlayerRate(player, hostile, world.getTime())) continue;

            occlusion = estimateOcclusion(player, soundPosition, source.getBoundingBox());
            int revealRadius = Math.min(component.entitySoundBlockRevealRadius(),
                    BlindnessMod.serverConfig().maximumEntitySoundBlockRevealRadius());
            BlockPos blockCenter = BlockPos.ofFloored(source.getX(), source.getBoundingBox().minY - 0.05, source.getZ());
            List<BlindnessPayloads.SoundRevealEntry> entries = collectVisibleBlocks(
                    player, source, blockCenter, revealRadius);
            float strength = soundStrength(category, volume);
            ServerPlayNetworking.send(player, new BlindnessPayloads.EntitySoundEcho(
                    new BlindnessPayloads.SoundEchoPosition(soundPosition.x, soundPosition.y, soundPosition.z),
                    new BlindnessPayloads.SoundEchoMetadata((byte) category.ordinal(), strength, hostile,
                            (byte) occlusion.ordinal(), world.getTime()),
                    blockCenter, entries));
        }
    }

    private static boolean isDuplicate(ServerWorld world, Entity source, SoundEvent sound, long tick) {
        Vec3d pos = source.getPos();
        Identifier soundId = Registries.SOUND_EVENT.getId(sound);
        Identifier keySoundId = soundId != null ? soundId : Identifier.of("blindness", "unregistered");
        SoundEventKey key = new SoundEventKey(world.getRegistryKey().getValue(), source.getId(),
                keySoundId,
                (int) Math.floor(pos.x * 4.0), (int) Math.floor(pos.y * 4.0),
                (int) Math.floor(pos.z * 4.0));
        RECENT_EVENTS.entrySet().removeIf(entry -> Math.abs(tick - entry.getValue()) > 2L);
        Long previous = RECENT_EVENTS.put(key, tick);
        return previous != null && tick - previous <= 2L;
    }

    private static boolean acceptEntityInterval(Entity source, EntitySoundCategory category, long tick) {
        if (category != EntitySoundCategory.FOOTSTEP && category != EntitySoundCategory.MOVEMENT) return true;
        Long previous = LAST_FOOTSTEP.get(source);
        if (previous != null && tick - previous < FOOTSTEP_MIN_INTERVAL_TICKS) return false;
        LAST_FOOTSTEP.put(source, tick);
        return true;
    }

    private static boolean acceptPlayerRate(ServerPlayerEntity player, boolean danger, long tick) {
        PlayerRuntimeState state = BlindnessRuntime.get(player);
        int window = (int) (tick / 20L);
        if (state.soundRateWindow != window) {
            state.soundRateWindow = window;
            state.normalSoundEventsInWindow = 0;
            state.dangerSoundEventsInWindow = 0;
        }
        if (danger) {
            if (state.dangerSoundEventsInWindow >= BlindnessMod.serverConfig()
                    .entitySoundMaximumDangerEventsPerSecond()) return false;
            state.dangerSoundEventsInWindow++;
        } else {
            if (state.normalSoundEventsInWindow >= BlindnessMod.serverConfig()
                    .entitySoundMaximumNormalEventsPerSecond()) return false;
            state.normalSoundEventsInWindow++;
        }
        return true;
    }

    private static Vec3d soundPosition(Entity source, EntitySoundCategory category) {
        double y = source.getY() + source.getHeight() * echoHeightFraction(category);
        return new Vec3d(source.getX(), y, source.getZ());
    }

    static double echoHeightFraction(EntitySoundCategory category) {
        return switch (category) {
            case FOOTSTEP, MOVEMENT -> 0.35;
            case AMBIENT, HURT, ATTACK, DEATH -> 0.65;
        };
    }

    private static float soundStrength(EntitySoundCategory category, float volume) {
        float categoryScale = switch (category) {
            case FOOTSTEP, MOVEMENT -> 0.58F;
            case AMBIENT -> 0.72F;
            case HURT -> 0.88F;
            case ATTACK, DEATH -> 1.0F;
        };
        return Math.clamp(categoryScale * Math.clamp(volume, 0.25F, 1.25F), 0.2F, 1.0F);
    }

    private static SoundOcclusion estimateOcclusion(ServerPlayerEntity player, Vec3d position, Box sourceBox) {
        Vec3d eye = player.getEyePos();
        double[] offsets = {-sourceBox.getLengthY() * 0.25, 0.0, sourceBox.getLengthY() * 0.25};
        int clear = 0;
        for (double offset : offsets) {
            Vec3d sample = position.add(0.0, offset, 0.0);
            BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, sample,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) clear++;
        }
        if (clear >= 2) return SoundOcclusion.CLEAR;
        if (clear == 1) return SoundOcclusion.PARTIAL;
        return SoundOcclusion.OCCLUDED;
    }

    private static List<BlindnessPayloads.SoundRevealEntry> collectVisibleBlocks(
            ServerPlayerEntity player, Entity source, BlockPos center, int radius) {
        int maximum = SoundAwarenessRules.blockLimit(radius,
                BlindnessMod.serverConfig().maximumEntitySoundRevealBlocks());
        if (maximum == 0) return List.of();
        int vertical = Math.min(radius, 2);
        List<Candidate> candidates = new ArrayList<>();
        for (BlockPos mutable : BlockPos.iterate(center.add(-radius, -vertical, -radius),
                center.add(radius, vertical, radius))) {
            int dx = mutable.getX() - center.getX();
            int dz = mutable.getZ() - center.getZ();
            if (dx * dx + dz * dz > radius * radius) continue;
            BlockPos pos = mutable.toImmutable();
            if (!player.getServerWorld().isChunkLoaded(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) continue;
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            VoxelShape shape = state.getOutlineShape(player.getWorld(), pos, ShapeContext.of(player));
            if (shape.isEmpty()) continue;
            int faces = visibleFaceMask(player, pos, shape);
            if (faces == 0) continue;
            double distance = pos.getSquaredDistance(source.getPos());
            int heightPenalty = Math.abs(pos.getY() - center.getY());
            candidates.add(new Candidate(pos, faces, distance + heightPenalty * 2.0));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score));
        return candidates.stream().limit(maximum)
                .map(candidate -> BlindnessPayloads.SoundRevealEntry.relativeTo(center,
                        candidate.pos(), candidate.faces())).toList();
    }

    private static int visibleFaceMask(ServerPlayerEntity player, BlockPos pos, VoxelShape shape) {
        Box box = shape.getBoundingBox().offset(pos);
        Vec3d eye = player.getEyePos();
        int mask = 0;
        for (Direction direction : Direction.values()) {
            Vec3d sample = faceCenter(box, direction).add(direction.getOffsetX() * -0.01,
                    direction.getOffsetY() * -0.01, direction.getOffsetZ() * -0.01);
            BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, sample,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) mask |= 1 << direction.getId();
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
        if (sound == null) return null;
        Identifier id = Registries.SOUND_EVENT.getId(sound);
        if (id == null) {
            if (REPORTED_UNREGISTERED_SOUNDS.add(sound.toString())) {
                BlindnessMod.LOGGER.debug("EntitySoundRevealService: ignoring unregistered SoundEvent: {}", sound);
            }
            return null;
        }
        return classifyPath(id.getPath());
    }

    static EntitySoundCategory classifyPath(String path) {
        if (path == null) return null;
        String[] parts = path.split("\\.");
        if (parts.length < 3 || !parts[0].equals("entity")) return null;
        String action = parts[parts.length - 1];
        if (action.equals("death")) return EntitySoundCategory.DEATH;
        if (action.equals("hurt")) return EntitySoundCategory.HURT;
        if (isOneOf(action, "attack", "shoot", "sting", "primed", "explode", "charge", "bite")) {
            return EntitySoundCategory.ATTACK;
        }
        if (action.equals("step")) return EntitySoundCategory.FOOTSTEP;
        if (isOneOf(action, "swim", "splash", "flap", "fly", "jump", "land")) {
            return EntitySoundCategory.MOVEMENT;
        }
        if (isOneOf(action, "ambient", "idle", "breathe", "growl", "hiss", "warn", "eat", "drink",
                "roar", "sniff", "celebrate", "lay_egg", "purr", "beg", "howl", "scream")) {
            return EntitySoundCategory.AMBIENT;
        }
        return null;
    }

    private static boolean isOneOf(String value, String... allowed) {
        for (String candidate : allowed) if (candidate.equals(value)) return true;
        return false;
    }

    private record Candidate(BlockPos pos, int faces, double score) {}
    private record SoundEventKey(Identifier dimension, int entityId, Identifier soundId,
                                 int xQuarter, int yQuarter, int zQuarter) {}
}
