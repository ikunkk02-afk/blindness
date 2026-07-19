package com.ikunkk02afk.blindness.awareness;

public enum CliffRisk {
    NONE,
    DROP,
    SEVERE_DROP,
    LAVA,
    VOID;

    public boolean isSevere() {
        return this == SEVERE_DROP || this == LAVA || this == VOID;
    }
}
