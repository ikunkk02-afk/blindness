package com.ikunkk02afk.blindness.accessibility;

import java.util.UUID;

/**
 * Accessor interface for the EyeOfEnderEntity mixin.
 * Allows other mixins to read/write the thrower UUID.
 */
public interface EyeOfEnderEntityAccessor {
    void blindness$setThrowerUuid(UUID uuid);
    UUID blindness$getThrowerUuid();
    boolean blindness$getDropsItem();
}
