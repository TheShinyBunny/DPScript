package com.shinysponge.dpscript.entities;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NBT {

    public static final int TAG = 0;
    public static final int INT = 1;
    private Map<String, Object> entries = new HashMap<>();

    public NBT() {
    }

    public NBT(Object model) {
        try {
            for (Field f : model.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Key k = f.getAnnotation(Key.class);
                if (k != null) {
                    entries.put(k.value(), f.get(model));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public NBT set(String key, int value) {
        return put(key,value);
    }

    public NBT set(String key, double value) {
        return put(key,value);
    }

    public NBT set(String key, float value) {
        return put(key,value);
    }

    public NBT set(String key, short value) {
        return put(key,value);
    }

    public NBT set(String key, long value) {
        return put(key,value);
    }

    public NBT set(String key, byte value) {
        return put(key,value);
    }

    public NBT set(String key, String value) {
        return put(key,value);
    }

    public NBT set(String key, NBT value) {
        return put(key,value);
    }

    public NBT set(String key, List<?> value) {
        return put(key,value);
    }

    private NBT put(String key, Object value) {
        this.entries.put(key,value);
        return this;
    }

    public NBT set(String key, boolean value) {
        return set(key,(byte)(value ? 1 : 0));
    }

}
