package com.ikunkk02afk.blindness.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlayerBlindnessComponent implements BlindnessPlayerComponent {
    private static final String DEFAULT_VISUAL_MODE = "standard";
    private final PlayerEntity owner;
    private boolean blindnessEnabled = true;
    private String visualMode = DEFAULT_VISUAL_MODE;
    private int caneProficiency;
    private int totalFalls;
    private int totalSuccessfulScans;
    private boolean tutorialCompleted;
    private int listeningChunkRadius = 1;
    private int entitySoundBlockRevealRadius = 2;
    private boolean starterCaneGranted;

    public PlayerBlindnessComponent(PlayerEntity owner) {
        this.owner = owner;
    }

    @Override public boolean blindnessEnabled() { return blindnessEnabled; }
    @Override public String visualMode() { return visualMode; }
    @Override public int caneProficiency() { return caneProficiency; }
    @Override public int totalFalls() { return totalFalls; }
    @Override public int totalSuccessfulScans() { return totalSuccessfulScans; }
    @Override public boolean tutorialCompleted() { return tutorialCompleted; }
    @Override public int listeningChunkRadius() { return listeningChunkRadius; }
    @Override public int entitySoundBlockRevealRadius() { return entitySoundBlockRevealRadius; }
    @Override public boolean starterCaneGranted() { return starterCaneGranted; }

    @Override public void setBlindnessEnabled(boolean value) { blindnessEnabled = value; sync(); }
    @Override public void setVisualMode(String value) { visualMode = DEFAULT_VISUAL_MODE.equals(value) ? value : DEFAULT_VISUAL_MODE; sync(); }
    @Override public void setCaneProficiency(int value) { caneProficiency = Math.clamp(value, 0, 100); sync(); }
    @Override public void incrementFalls() { totalFalls = saturatingIncrement(totalFalls); sync(); }
    @Override public void incrementSuccessfulScans() { totalSuccessfulScans = saturatingIncrement(totalSuccessfulScans); sync(); }
    @Override public void setTutorialCompleted(boolean value) { tutorialCompleted = value; sync(); }
    @Override public void setSoundAwarenessSettings(int listeningRadius, int blockRadius) {
        listeningChunkRadius = Math.clamp(listeningRadius, 0, 2);
        entitySoundBlockRevealRadius = Math.clamp(blockRadius, 0, 4);
        sync();
    }
    @Override public void setStarterCaneGranted(boolean value) { starterCaneGranted = value; sync(); }

    @Override
    public void reset() {
        blindnessEnabled = true;
        visualMode = DEFAULT_VISUAL_MODE;
        caneProficiency = 0;
        totalFalls = 0;
        totalSuccessfulScans = 0;
        tutorialCompleted = false;
        listeningChunkRadius = 1;
        entitySoundBlockRevealRadius = 2;
        starterCaneGranted = false;
        sync();
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        blindnessEnabled = !tag.contains("BlindnessEnabled") || tag.getBoolean("BlindnessEnabled");
        setVisualModeWithoutSync(tag.getString("VisualMode"));
        caneProficiency = Math.clamp(tag.getInt("CaneProficiency"), 0, 100);
        totalFalls = Math.max(0, tag.getInt("TotalFalls"));
        totalSuccessfulScans = Math.max(0, tag.getInt("TotalSuccessfulScans"));
        tutorialCompleted = tag.getBoolean("TutorialCompleted");
        listeningChunkRadius = tag.contains("ListeningChunkRadius")
                ? Math.clamp(tag.getInt("ListeningChunkRadius"), 0, 2) : 1;
        entitySoundBlockRevealRadius = tag.contains("EntitySoundBlockRevealRadius")
                ? Math.clamp(tag.getInt("EntitySoundBlockRevealRadius"), 0, 4) : 2;
        starterCaneGranted = tag.getBoolean("StarterCaneGranted");
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
        tag.putBoolean("BlindnessEnabled", blindnessEnabled);
        tag.putString("VisualMode", visualMode);
        tag.putInt("CaneProficiency", caneProficiency);
        tag.putInt("TotalFalls", totalFalls);
        tag.putInt("TotalSuccessfulScans", totalSuccessfulScans);
        tag.putBoolean("TutorialCompleted", tutorialCompleted);
        tag.putInt("ListeningChunkRadius", listeningChunkRadius);
        tag.putInt("EntitySoundBlockRevealRadius", entitySoundBlockRevealRadius);
        tag.putBoolean("StarterCaneGranted", starterCaneGranted);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == owner;
    }

    private void setVisualModeWithoutSync(String value) {
        visualMode = DEFAULT_VISUAL_MODE.equals(value) ? value : DEFAULT_VISUAL_MODE;
    }

    private void sync() {
        if (!owner.getWorld().isClient && owner instanceof ServerPlayerEntity) {
            BlindnessComponents.PLAYER.sync(owner);
        }
    }

    private static int saturatingIncrement(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }
}
