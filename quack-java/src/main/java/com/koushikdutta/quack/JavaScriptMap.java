package com.koushikdutta.quack;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

public class JavaScriptMap extends AbstractMap<String, Object> {
    private JavaScriptObject jo;
    private Set<Entry<String, Object>> set = null;

    private JavaScriptMap() {
    }

    public static JavaScriptMap of(JavaScriptObject jo) {
        JavaScriptMap map = new JavaScriptMap();
        map.jo = jo;
        return map;
    }

    public Set<Entry<String, Object>> entrySet() {
        if (set != null) {
            return set;
        }

        set = new HashSet<>();

        JavaScriptObject fn = jo.quackContext.evaluateForJavaScriptObject("Object.entries");
        JavaScriptObject entries = (JavaScriptObject) fn.call(jo);
        for (long i = 0; ; ++i) {
            JavaScriptObject entry = (JavaScriptObject) entries.get(i);
            if (entry == null) {
                break;
            }
            Object key = entry.get(0);
            Object value = jo.quackContext.coerceJavaScriptToJava(null, entry.get(1));
            set.add(new SimpleImmutableEntry<>(key == null ? "" : key.toString(), value));
        }

        return set;
    }
}
