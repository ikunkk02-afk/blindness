package com.ikunkk02afk.blindness.client.compat;

import com.ikunkk02afk.blindness.BlindnessMod;
import com.ikunkk02afk.blindness.client.BlindnessClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BlockedInformationMods {
    private static final Set<String> RECIPE_VIEWERS = Set.of(
            "jei", "roughlyenoughitems", "roughlyenoughitems-api", "emi");
    private static final Map<String, Category> DEFAULTS = defaults();
    private static List<BlockedMod> startupSnapshot = List.of();
    private static boolean startupBlockingEnabled;

    private BlockedInformationMods() {}

    public static void initializeSnapshot() {
        startupBlockingEnabled = BlindnessClient.CONFIG.blockInformationMods();
        startupSnapshot = detect();
        if (!startupSnapshot.isEmpty()) {
            BlindnessMod.LOGGER.warn("Blocked information mods detected: {}",
                    startupSnapshot.stream().map(BlockedMod::id).toList());
        }
    }

    public static boolean shouldBlockWorldEntry() {
        return startupBlockingEnabled && !startupSnapshot.isEmpty();
    }

    public static List<BlockedMod> detected() { return startupSnapshot; }

    public static String report() {
        StringBuilder result = new StringBuilder("Blindness incompatible information mods\n");
        for (BlockedMod mod : startupSnapshot) {
            result.append(mod.name()).append(" [").append(mod.id()).append("] ")
                    .append(mod.version()).append(" - ").append(mod.category().translationKey()).append('\n');
        }
        return result.toString();
    }

    private static List<BlockedMod> detect() {
        if (!BlindnessClient.CONFIG.blockInformationMods()) return List.of();
        Set<String> ignored = BlindnessClient.CONFIG.ignoredBlockedModIds().stream()
                .map(BlockedInformationMods::normalize).collect(java.util.stream.Collectors.toSet());
        Map<String, Category> candidates = new LinkedHashMap<>();
        DEFAULTS.forEach((id, category) -> {
            boolean categoryEnabled = switch (category) {
                case MAP -> BlindnessClient.CONFIG.blockMapMods();
                case INFO_HUD -> BlindnessClient.CONFIG.blockWorldInfoHudMods();
                case XRAY -> BlindnessClient.CONFIG.blockXrayMods();
            };
            if (categoryEnabled) candidates.put(id, category);
        });
        for (String configured : BlindnessClient.CONFIG.additionalBlockedModIds()) {
            String id = normalize(configured);
            if (!id.isBlank()) candidates.putIfAbsent(id, Category.XRAY);
        }

        List<BlockedMod> result = new ArrayList<>();
        for (Map.Entry<String, Category> entry : candidates.entrySet()) {
            String id = entry.getKey();
            if (RECIPE_VIEWERS.contains(id) || ignored.contains(id)) continue;
            FabricLoader.getInstance().getModContainer(id).ifPresent(container -> result.add(toBlocked(container, entry.getValue())));
        }
        return List.copyOf(result);
    }

    private static BlockedMod toBlocked(ModContainer container, Category category) {
        return new BlockedMod(container.getMetadata().getName(), container.getMetadata().getId(),
                container.getMetadata().getVersion().getFriendlyString(), category);
    }

    private static String normalize(String id) { return id == null ? "" : id.trim().toLowerCase(Locale.ROOT); }

    private static Map<String, Category> defaults() {
        Map<String, Category> result = new LinkedHashMap<>();
        for (String id : List.of("xaerominimap", "xaeroworldmap", "journeymap", "voxelmap", "ftbchunks",
                "antiqueatlas", "antique_atlas", "antique_atlas_4", "map_atlases", "mapatlases")) {
            result.put(id, Category.MAP);
        }
        for (String id : List.of("jade", "wthit", "hwyla", "waila", "theoneprobe")) {
            result.put(id, Category.INFO_HUD);
        }
        return Map.copyOf(result);
    }

    public enum Category {
        MAP("screen.blindness.blocked.category.map"),
        INFO_HUD("screen.blindness.blocked.category.info_hud"),
        XRAY("screen.blindness.blocked.category.xray");

        private final String translationKey;
        Category(String translationKey) { this.translationKey = translationKey; }
        public String translationKey() { return translationKey; }
    }

    public record BlockedMod(String name, String id, String version, Category category) {}
}
