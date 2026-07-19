package com.ikunkk02afk.blindness.contact;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ContactRevealMath {
    public static final int MAX_REVEALS_PER_CONTACT = 8;
    public static final List<BlockPos> LOCAL_OFFSETS = List.of(
            BlockPos.ORIGIN,
            BlockPos.ORIGIN.up(),
            BlockPos.ORIGIN.down(),
            BlockPos.ORIGIN.north(),
            BlockPos.ORIGIN.south(),
            BlockPos.ORIGIN.east(),
            BlockPos.ORIGIN.west());

    private ContactRevealMath() {}

    public static boolean isDirectContactOffset(int dx, int dy, int dz) {
        return Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 1;
    }
}
