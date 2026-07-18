package com.ikunkk02afk.blindness.network;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadBoundsTest {
    @Test void scanResultConstructorTruncatesBeforeNetworking() {
        var hits = new ArrayList<BlindnessPayloads.ScanHit>();
        for (int i = 0; i < 200; i++) hits.add(new BlindnessPayloads.ScanHit((byte) i, (byte) 0, (byte) 0, (byte) 1));
        var payload = new BlindnessPayloads.ScanResult(1, (byte) 0, BlockPos.ORIGIN, hits);
        assertEquals(BlindnessPayloads.ScanResult.MAX_HITS, payload.hits().size());
    }

    @Test void scanHitUsesBoundedRelativeCoordinatesAndFace() {
        BlockPos origin = new BlockPos(100, 64, -40);
        BlockPos hit = new BlockPos(105, 62, -37);
        var encoded = BlindnessPayloads.ScanHit.relativeTo(origin, hit, Direction.UP);
        assertEquals(hit, encoded.resolve(origin));
        assertEquals(Direction.UP.getId(), encoded.face());
        assertEquals(true, encoded.isValid(8));
    }
}
