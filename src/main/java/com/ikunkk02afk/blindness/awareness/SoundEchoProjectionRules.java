package com.ikunkk02afk.blindness.awareness;

public final class SoundEchoProjectionRules {
    private static final float MINIMUM_CLIP_W = 0.001F;

    private SoundEchoProjectionRules() {}

    public static ProjectionState classify(float clipX, float clipY, float clipZ, float clipW) {
        if (!Float.isFinite(clipX) || !Float.isFinite(clipY) || !Float.isFinite(clipZ)
                || !Float.isFinite(clipW)) return ProjectionState.REJECTED;
        if (clipW <= MINIMUM_CLIP_W) return ProjectionState.BEHIND_CAMERA;
        float ndcX = clipX / clipW;
        float ndcY = clipY / clipW;
        float ndcZ = clipZ / clipW;
        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY) || !Float.isFinite(ndcZ)) {
            return ProjectionState.REJECTED;
        }
        return ndcX >= -1F && ndcX <= 1F && ndcY >= -1F && ndcY <= 1F
                && ndcZ >= -1F && ndcZ <= 1F
                ? ProjectionState.ON_SCREEN : ProjectionState.OFF_SCREEN;
    }

    public enum ProjectionState {
        ON_SCREEN,
        OFF_SCREEN,
        BEHIND_CAMERA,
        REJECTED
    }
}
