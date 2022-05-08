package com.koushikdutta.quack;

import java.util.AbstractList;

public class JavaScriptList extends AbstractList<Object> {
    private JavaScriptObject jo;

    private JavaScriptList() {
    }

    public static JavaScriptList of(JavaScriptObject jo) {
        JavaScriptList list = new JavaScriptList();
        list.jo = jo;
        return list;
    }

    @Override
    public int size() {
        Object length = jo.get("length");
        if (length instanceof Integer) {
            return (Integer) length;
        }
        return 0;
    }

    @Override
    public Object get(int i) {
        return jo.get(i);
    }
}
