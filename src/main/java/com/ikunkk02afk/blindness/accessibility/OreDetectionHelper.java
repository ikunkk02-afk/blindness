package com.ikunkk02afk.blindness.accessibility;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side ore detection logic that runs during a cane contact scan.
 * <p>
 * Only blocks already present in the contact reveal set are checked —
 * this helper does NOT perform its own block iteration. The caller
 * (CaneContactService) passes the list of already-detected positions.
 */
public final class OreDetectionHelper {
    private OreDetectionHelper() {}

    /**
     * Examines a list of scanned positions and returns detected ore blocks.
     * Results are sorted by distance (nearest first) and capped at {@code maxResults}.
     *
     * @param player       the scanning player
     * @param scannedPositions the positions already identified by the cane contact
     * @param maxResults   maximum number of ore entries to return
     * @return sorted list of detected ore entries (may be empty)
     */
    public static List<OreEntry> detect(ServerPlayerEntity player, List<BlockPos> scannedPositions, int maxResults) {
        Vec3d eye = player.getEyePos();
        Map<OreType, List<OreEntry>> grouped = new LinkedHashMap<>();

        for (BlockPos pos : scannedPositions) {
            BlockState state = player.getServerWorld().getBlockState(pos);
            if (state.isAir()) continue;

            OreType type = OreType.classify(state);
            if (type == null) continue;

            double distSq = pos.getSquaredDistance(eye);
            grouped.computeIfAbsent(type, k -> new ArrayList<>())
                    .add(new OreEntry(type, pos.toImmutable(), state, distSq));
        }

        // Sort each group by distance, then flatten by nearest group.
        List<OreEntry> flat = new ArrayList<>();
        for (List<OreEntry> group : grouped.values()) {
            group.sort(Comparator.comparingDouble(OreEntry::distanceSq));
            flat.addAll(group);
        }
        flat.sort(Comparator.comparingDouble(OreEntry::distanceSq));

        if (flat.size() > maxResults) {
            flat = flat.subList(0, maxResults);
        }
        return List.copyOf(flat);
    }

    /**
     * Merge per-entry detection results into a grouped summary for the HUD.
     */
    public static List<GroupedOreResult> groupResults(List<OreEntry> entries, int maxGroups) {
        Map<OreType, GroupedOreResult> grouped = new LinkedHashMap<>();
        for (OreEntry entry : entries) {
            grouped.compute(entry.type(), (t, existing) -> {
                if (existing == null) return new GroupedOreResult(entry.type(), entry.blockState(),
                        1, entry.distanceSq(), entry.pos());
                return existing.withOneMore(entry.distanceSq(), entry.pos());
            });
        }
        return grouped.values().stream()
                .sorted(Comparator.comparingDouble(GroupedOreResult::nearestDistanceSq))
                .limit(Math.max(1, maxGroups))
                .toList();
    }

    public record OreEntry(OreType type, BlockPos pos, BlockState blockState, double distanceSq) {}

    public static final class GroupedOreResult {
        private final OreType type;
        private final BlockState representativeState;
        private final int count;
        private final double nearestDistanceSq;
        private final BlockPos nearestPos;

        GroupedOreResult(OreType type, BlockState state, int count, double distSq, BlockPos pos) {
            this.type = type;
            this.representativeState = state;
            this.count = count;
            this.nearestDistanceSq = distSq;
            this.nearestPos = pos;
        }

        public OreType type() { return type; }
        public BlockState representativeState() { return representativeState; }
        public int count() { return count; }
        public double nearestDistanceSq() { return nearestDistanceSq; }
        public BlockPos nearestPos() { return nearestPos; }

        GroupedOreResult withOneMore(double otherDistSq, BlockPos otherPos) {
            if (otherDistSq < nearestDistanceSq) {
                return new GroupedOreResult(type, representativeState, count + 1, otherDistSq, otherPos);
            }
            return new GroupedOreResult(type, representativeState, count + 1, nearestDistanceSq, nearestPos);
        }
    }
}
