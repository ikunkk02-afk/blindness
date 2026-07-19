package com.ikunkk02afk.blindness.network;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PayloadBoundsTest {
    @Test void contactResultConstructorTruncatesToEight() {
        var entries = new ArrayList<BlindnessPayloads.ContactEntry>();
        for (int i = 0; i < 20; i++) entries.add(new BlindnessPayloads.ContactEntry((byte) 0, (byte) 0, (byte) 0, (byte) 3));
        var payload = new BlindnessPayloads.ContactReveal(1, BlockPos.ORIGIN, entries);
        assertEquals(8, payload.entries().size());
    }

    @Test void contactEntryPreservesCenterFacesAndLocalPosition() {
        BlockPos center = new BlockPos(100, 64, -40);
        BlockPos neighbor = center.up();
        int faces = 1 << Direction.NORTH.getId();
        var encoded = BlindnessPayloads.ContactEntry.relativeTo(center, neighbor, false, faces);
        assertEquals(neighbor, encoded.resolve(center));
        assertEquals(faces, encoded.visibleFaces());
        assertFalse(encoded.isCenter());
        assertTrue(encoded.isValid());
    }

    @Test void contactEntryRejectsNonLocalCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> BlindnessPayloads.ContactEntry.relativeTo(
                BlockPos.ORIGIN, new BlockPos(3, 0, 0), false, 1));
    }
}
