package com.ikunkk02afk.blindness.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundedExpiryMapTest {
    @Test void evictsOldestAndExpiresEntries() {
        BoundedExpiryMap map = new BoundedExpiryMap();
        map.put(1, 10, 2);
        map.put(2, 20, 2);
        map.put(3, 30, 2);
        assertFalse(map.contains(1));
        assertEquals(2, map.size());
        map.prune(20);
        assertFalse(map.contains(2));
        assertTrue(map.contains(3));
    }
}
