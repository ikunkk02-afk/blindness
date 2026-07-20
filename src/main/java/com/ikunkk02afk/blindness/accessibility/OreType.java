package com.ikunkk02afk.blindness.accessibility;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Fine-grained ore classification using Fabric conventional block tags.
 * <p>
 * Each constant corresponds to a conventional ore tag group.
 * {@link #MODDED_OR_UNKNOWN} covers everything in {@code c:ores} that
 * doesn't match a more specific tag — its display name is read from the
 * block's own {@link net.minecraft.block.Block#getName()}.
 */
public enum OreType {
    COAL,
    IRON,
    COPPER,
    GOLD,
    REDSTONE,
    LAPIS,
    DIAMOND,
    EMERALD,
    QUARTZ,
    ANCIENT_DEBRIS,
    MODDED_OR_UNKNOWN;

    public String translationKey() {
        return "ore.blindness." + name().toLowerCase();
    }

    /**
     * Classifies a block state into an ore type using Fabric conventional
     * block tags. Falls back to {@link #MODDED_OR_UNKNOWN} for any block
     * that is in {@code c:ores} but doesn't match a specific tag.
     * <p>
     * Returns {@code null} if the state is not an ore at all.
     */
    public static OreType classify(BlockState state) {
        RegistryEntry<net.minecraft.block.Block> entry = state.getRegistryEntry();

        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.COAL_ORES)) return COAL;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.IRON_ORES)) return IRON;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.COPPER_ORES)) return COPPER;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.GOLD_ORES)) return GOLD;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.REDSTONE_ORES)) return REDSTONE;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.LAPIS_ORES)) return LAPIS;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.DIAMOND_ORES)) return DIAMOND;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.EMERALD_ORES)) return EMERALD;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.QUARTZ_ORES)) return QUARTZ;
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.NETHERITE_SCRAP_ORES)) return ANCIENT_DEBRIS;

        // Generic ore tag catch-all for modded ores.
        if (entry.isIn(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.ORES)) {
            return MODDED_OR_UNKNOWN;
        }
        return null;
    }

    /**
     * Returns the display name for an ore type.
     * For {@link #MODDED_OR_UNKNOWN}, uses the block's own localised name.
     */
    public static Text displayName(OreType type, BlockState state) {
        if (type == MODDED_OR_UNKNOWN) {
            return state.getBlock().getName();
        }
        return Text.translatable(type.translationKey());
    }

    /**
     * Whether this ore type is considered "rare" (diamond, emerald, ancient debris).
     */
    public boolean isRare() {
        return this == DIAMOND || this == EMERALD || this == ANCIENT_DEBRIS;
    }
}
