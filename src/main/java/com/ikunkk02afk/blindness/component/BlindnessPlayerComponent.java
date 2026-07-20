package com.ikunkk02afk.blindness.component;

import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public interface BlindnessPlayerComponent extends AutoSyncedComponent {
    boolean blindnessEnabled();
    String visualMode();
    int caneProficiency();
    int totalFalls();
    int totalSuccessfulScans();
    boolean tutorialCompleted();
    int listeningChunkRadius();
    int entitySoundBlockRevealRadius();

    void setBlindnessEnabled(boolean value);
    void setVisualMode(String value);
    void setCaneProficiency(int value);
    void incrementFalls();
    void incrementSuccessfulScans();
    void setTutorialCompleted(boolean value);
    void setSoundAwarenessSettings(int listeningChunkRadius, int blockRevealRadius);

    boolean starterCaneGranted();
    void setStarterCaneGranted(boolean value);

    void reset();
}
