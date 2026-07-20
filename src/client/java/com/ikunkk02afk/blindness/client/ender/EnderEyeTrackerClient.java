package com.ikunkk02afk.blindness.client.ender;

import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/**
 * Client-side tracker for a single player-thrown Ender Eye.
 * <p>
 * Manages the lifecycle: flying → dropped (search → track → position fallback) → cleared.
 * Only tracks the LAST thrown eye. A new throw replaces the old one.
 * <p>
 * Does NOT render anything — that's {@link EnderEyeTrackingRenderer}'s job.
 */
public final class EnderEyeTrackerClient {

    // ── Constants ──

    /** Ticks to search for the dropped ItemEntity near the result position. */
    private static final int DROPPED_SEARCH_TICKS = 40;  // 2 seconds
    /** Default marker duration for dropped items (ticks). */
    private static final int DEFAULT_DROPPED_DURATION_TICKS = 180;  // 9 seconds
    /** Search radius for finding the dropped item entity. */
    private static final double DROPPED_ITEM_SEARCH_RADIUS = 4.0;
    /** How many ticks before the eye entity is considered MIA before giving up. */
    private static final int ENTITY_MIA_GRACE_TICKS = 60;  // 3 seconds

    // ── State ──

    private static int trackedEntityId = -1;
    private static UUID trackedEntityUuid;
    private static Phase phase = Phase.INACTIVE;

    /** The drop position sent by the server (for DROPPED phases). */
    private static Vec3d dropPosition;

    /** The item entity ID found during DROPPED_SEARCHING. */
    private static int droppedItemEntityId = -1;

    /** Tick counter for timeouts. */
    private static int phaseTicks;

    /** The client tick when the last sound was played. */
    private static long lastSoundTick = -1;

    /** Position from the previous tick, for interpolation. */
    private static Vec3d prevTrackedPosition;
    /** Position from the current tick, for interpolation. */
    private static Vec3d currentTrackedPosition;

    public enum Phase {
        INACTIVE,
        FLYING,
        DROPPED_SEARCHING,   // looking for the ItemEntity near drop position
        DROPPED_TRACKING,     // following the found ItemEntity
        DROPPED_POSITION,     // ItemEntity not found, using fixed position (fallback)
        SHATTERED             // eye shattered, about to clear
    }

    private EnderEyeTrackerClient() {}

    // ── Public API ──

    /** Called when the server sends StartEnderEyeTracking. */
    public static void startTracking(int entityId, UUID entityUuid) {
        if (!BlindnessClient.CONFIG.enableEnderEyeTrackingMarker()) return;
        clearSilently();
        trackedEntityId = entityId;
        trackedEntityUuid = entityUuid;
        phase = Phase.FLYING;
        phaseTicks = 0;
        lastSoundTick = -1;
        prevTrackedPosition = null;
        currentTrackedPosition = null;
        dropPosition = null;
        droppedItemEntityId = -1;
    }

    /** Called when the server sends EnderEyeResult (dropped or shattered). */
    public static void onResult(int resultType, double x, double y, double z) {
        if (phase == Phase.INACTIVE) return;

        Vec3d pos = new Vec3d(x, y, z);

        if (resultType == 0) {
            // Dropped as item
            phase = Phase.DROPPED_SEARCHING;
            phaseTicks = 0;
            dropPosition = pos;
            droppedItemEntityId = -1;
            prevTrackedPosition = null;
            currentTrackedPosition = null;
        } else {
            // Shattered
            phase = Phase.SHATTERED;
            phaseTicks = 0;
        }
    }

    /** Called every client tick. Updates positions and handles timeouts. */
    public static void tick() {
        if (phase == Phase.INACTIVE) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            clear();
            return;
        }

        long tick = client.world.getTime();
        phaseTicks++;

        switch (phase) {
            case FLYING -> tickFlying(client, tick);
            case DROPPED_SEARCHING -> tickDroppedSearching(client, tick);
            case DROPPED_TRACKING -> tickDroppedTracking(client, tick);
            case DROPPED_POSITION -> tickDroppedPosition(client, tick);
            case SHATTERED -> tickShattered();
        }
    }

    private static void tickFlying(MinecraftClient client, long tick) {
        Entity entity = client.world.getEntityById(trackedEntityId);

        if (entity instanceof EyeOfEnderEntity eye && eye.isAlive()) {
            // Update positions for interpolation
            prevTrackedPosition = currentTrackedPosition;
            currentTrackedPosition = eye.getPos();
            if (prevTrackedPosition == null) {
                prevTrackedPosition = currentTrackedPosition;
            }

            // Tracking sound
            if (BlindnessClient.CONFIG.enableEnderEyeTrackingSound() && client.player != null) {
                int interval = BlindnessClient.CONFIG.enderEyeTrackingSoundIntervalTicks();
                if (lastSoundTick < 0 || tick - lastSoundTick >= interval) {
                    lastSoundTick = tick;
                    client.player.playSound(
                            net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                            0.25F, 1.4F);
                }
            }
        } else if (entity == null) {
            // Entity disappeared — give grace period then clear.
            if (phaseTicks > ENTITY_MIA_GRACE_TICKS) {
                clear();
            }
        } else {
            // Entity exists but is not an EyeOfEnderEntity — mismatched.
            if (phaseTicks > ENTITY_MIA_GRACE_TICKS) {
                clear();
            }
        }
    }

    private static void tickDroppedSearching(MinecraftClient client, long tick) {
        // Search for ItemEntity near drop position.
        if (client.world != null && dropPosition != null) {
            List<ItemEntity> items = client.world.getEntitiesByClass(
                    ItemEntity.class,
                    new net.minecraft.util.math.Box(
                            dropPosition.x - DROPPED_ITEM_SEARCH_RADIUS,
                            dropPosition.y - DROPPED_ITEM_SEARCH_RADIUS,
                            dropPosition.z - DROPPED_ITEM_SEARCH_RADIUS,
                            dropPosition.x + DROPPED_ITEM_SEARCH_RADIUS,
                            dropPosition.y + DROPPED_ITEM_SEARCH_RADIUS,
                            dropPosition.z + DROPPED_ITEM_SEARCH_RADIUS
                    ),
                    item -> item.getStack().isOf(Items.ENDER_EYE)
            );

            if (!items.isEmpty()) {
                // Found! Track the nearest one.
                ItemEntity nearest = items.get(0);
                double bestDist = nearest.getPos().squaredDistanceTo(dropPosition);
                for (int i = 1; i < items.size(); i++) {
                    double dist = items.get(i).getPos().squaredDistanceTo(dropPosition);
                    if (dist < bestDist) {
                        nearest = items.get(i);
                        bestDist = dist;
                    }
                }
                droppedItemEntityId = nearest.getId();
                phase = Phase.DROPPED_TRACKING;
                phaseTicks = 0;
                prevTrackedPosition = null;
                currentTrackedPosition = nearest.getPos();
                return;
            }
        }

        // Use the fixed drop position while searching.
        currentTrackedPosition = dropPosition;
        prevTrackedPosition = dropPosition;

        if (phaseTicks > DROPPED_SEARCH_TICKS) {
            // Give up searching, use fixed position.
            phase = Phase.DROPPED_POSITION;
            phaseTicks = 0;
        }
    }

    private static void tickDroppedTracking(MinecraftClient client, long tick) {
        Entity entity = client.world.getEntityById(droppedItemEntityId);
        int durationTicks = BlindnessClient.CONFIG.droppedEnderEyeMarkerDurationTicks();

        if (entity instanceof ItemEntity item && item.isAlive()) {
            prevTrackedPosition = currentTrackedPosition;
            currentTrackedPosition = item.getPos();
            if (prevTrackedPosition == null) {
                prevTrackedPosition = currentTrackedPosition;
            }
        } else {
            // Item picked up or removed — clear marker.
            clear();
            return;
        }

        if (phaseTicks > durationTicks) {
            clear();
        }
    }

    private static void tickDroppedPosition(MinecraftClient client, long tick) {
        int durationTicks = BlindnessClient.CONFIG.droppedEnderEyeMarkerDurationTicks();
        if (phaseTicks > durationTicks) {
            clear();
        }
    }

    private static void tickShattered() {
        // Brief hold for the shattered animation/message, then clear.
        if (phaseTicks > 2) {
            clear();
        }
    }

    /** Clear tracking entirely. */
    public static void clear() {
        trackedEntityId = -1;
        trackedEntityUuid = null;
        phase = Phase.INACTIVE;
        phaseTicks = 0;
        lastSoundTick = -1;
        prevTrackedPosition = null;
        currentTrackedPosition = null;
        dropPosition = null;
        droppedItemEntityId = -1;
    }

    /** Clear without affecting the result handler's action bar message. */
    private static void clearSilently() {
        trackedEntityId = -1;
        trackedEntityUuid = null;
        phase = Phase.INACTIVE;
        phaseTicks = 0;
        lastSoundTick = -1;
        prevTrackedPosition = null;
        currentTrackedPosition = null;
        dropPosition = null;
        droppedItemEntityId = -1;
    }

    // ── Getters for the renderer ──

    public static boolean isActive() {
        return phase != Phase.INACTIVE && phase != Phase.SHATTERED;
    }

    public static Phase phase() {
        return phase;
    }

    /**
     * Returns the interpolated position of the tracked entity.
     */
    public static Vec3d getInterpolatedPosition(float tickDelta) {
        if (currentTrackedPosition == null) return null;
        if (prevTrackedPosition == null) return currentTrackedPosition;
        return prevTrackedPosition.lerp(currentTrackedPosition, tickDelta);
    }

    /**
     * Returns the distance from the player to the tracked entity (in blocks).
     */
    public static int getDistance(MinecraftClient client) {
        Vec3d pos = currentTrackedPosition;
        if (pos == null || client.player == null) return 0;
        return (int) Math.round(client.player.getEyePos().distanceTo(pos));
    }

    /**
     * Returns the UUID of the currently tracked entity (for identity checks).
     */
    public static UUID getTrackedUuid() {
        return trackedEntityUuid;
    }

    /**
     * Returns the world tick for sound scheduling.
     */
    public static long getLastSoundTick() {
        return lastSoundTick;
    }

    public static void setLastSoundTick(long tick) {
        lastSoundTick = tick;
    }

    /**
     * Returns which phase we're in for marker labeling.
     */
    public static boolean isDroppedPhase() {
        return phase == Phase.DROPPED_SEARCHING
                || phase == Phase.DROPPED_TRACKING
                || phase == Phase.DROPPED_POSITION;
    }
}
