package com.ikunkk02afk.blindness.awareness;

public enum EntitySoundCategory {
    FOOTSTEP,
    AMBIENT,
    HURT,
    ATTACK,
    DEATH,
    MOVEMENT;

    public RevealSource revealSource() {
        return switch (this) {
            case FOOTSTEP, MOVEMENT -> RevealSource.ENTITY_FOOTSTEP;
            case AMBIENT -> RevealSource.ENTITY_AMBIENT;
            case HURT, ATTACK, DEATH -> RevealSource.ENTITY_DANGER;
        };
    }
}
