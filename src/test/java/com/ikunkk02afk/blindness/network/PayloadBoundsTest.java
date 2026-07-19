package com.ikunkk02afk.blindness.network;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import com.ikunkk02afk.blindness.awareness.EntitySoundCategory;
import com.ikunkk02afk.blindness.awareness.SoundOcclusion;

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

    @Test void entitySoundEntriesAreLocalAndBounded() {
        BlockPos center = new BlockPos(10, 64, -5);
        var entry = BlindnessPayloads.SoundRevealEntry.relativeTo(center, center.down().east(), 0x3F);
        assertTrue(entry.isValid());
        assertEquals(center.down().east(), entry.resolve(center));
        assertThrows(IllegalArgumentException.class, () -> BlindnessPayloads.SoundRevealEntry.relativeTo(
                center, center.add(5, 0, 0), 1));
    }

    @Test void entitySoundPayloadCapsEntries() {
        var entries = new ArrayList<BlindnessPayloads.SoundRevealEntry>();
        for (int i = 0; i < 100; i++) entries.add(new BlindnessPayloads.SoundRevealEntry((byte) 0, (byte) -1, (byte) 0, (byte) 1));
        var payload = new BlindnessPayloads.EntitySoundEcho(
                new BlindnessPayloads.SoundEchoPosition(1.25, 64.5, -3.75),
                new BlindnessPayloads.SoundEchoMetadata((byte) EntitySoundCategory.AMBIENT.ordinal(),
                        0.72F, false, (byte) SoundOcclusion.CLEAR.ordinal(), 42L),
                BlockPos.ORIGIN, entries);
        assertEquals(80, payload.entries().size());
        assertTrue(payload.position().isValid());
        assertTrue(payload.metadata().isValid());
    }

    @Test void echoMetadataContainsNoEntityTrackingFields() {
        var names = java.util.Arrays.stream(BlindnessPayloads.EntitySoundEcho.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        assertEquals(java.util.List.of("position", "metadata", "blockCenter", "entries"), names);
        String joined = String.join(" ", names).toLowerCase();
        assertFalse(joined.contains("entity"));
        assertFalse(joined.contains("uuid"));
        assertFalse(joined.contains("type"));
    }
}
