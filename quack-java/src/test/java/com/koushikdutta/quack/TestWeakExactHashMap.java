package com.koushikdutta.quack;

import org.junit.Test;

import java.lang.ref.WeakReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class TestWeakExactHashMap {
    @Test
    public void testMap() {
        assumeFalse(System.getProperty("java.vm.name").equals("Dalvik"));

        Object key = new Object();
        Object value = new Object();
        WeakReference<Object> ref = new WeakReference<>(key);
        WeakExactHashMap<Object, Object> map = new WeakExactHashMap<>();
        map.put(key, value);
        assertEquals(map.get(key), value);
        assertEquals(map.size(), 1);

        for (int i = 0; i < 5; i++) {
            System.gc();
            try { Thread.sleep(250);} catch(InterruptedException ignore){}
        }

        assertEquals(map.get(key), value);
        assertEquals(map.size(), 1);

        key = null;
        while (ref.get() != null) {
            System.gc();
            try { Thread.sleep(250);} catch(InterruptedException ignore){}
        }
        map.purge();
        assertEquals(map.size(), 0);
    }
}
