package io.openleap.starter.core;

import java.lang.reflect.Field;

public class ReflectionUtils {

    private ReflectionUtils() {

    }

    public static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);

            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}