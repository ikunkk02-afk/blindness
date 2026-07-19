package com.ikunkk02afk.blindness.awareness;

public enum RevealSource {
    CANE_CENTER,
    CANE_ADJACENT,
    ENTITY_FOOTSTEP,
    ENTITY_AMBIENT,
    ENTITY_DANGER;

    public static RevealSource fromNetwork(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
