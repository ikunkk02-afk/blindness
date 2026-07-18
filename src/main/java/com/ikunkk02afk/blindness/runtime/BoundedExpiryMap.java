package com.ikunkk02afk.blindness.runtime;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoundedExpiryMap {
    private final LinkedHashMap<Long, Long> entries = new LinkedHashMap<>();

    public void put(long key, long expiryTick, int maximumSize) {
        entries.put(key, expiryTick);
        int limit = Math.max(1, maximumSize);
        while (entries.size() > limit) {
            Iterator<Long> iterator = entries.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public boolean contains(long key) { return entries.containsKey(key); }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
    public void prune(long tick) { entries.entrySet().removeIf(entry -> entry.getValue() <= tick); }
    public Map<Long, Long> view() { return Map.copyOf(entries); }
}
