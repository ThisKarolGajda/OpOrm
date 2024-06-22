package me.opkarol.oporm;


import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public interface DatabaseEntity<PK extends Serializable> {
    PK getId();

    default String[] getFieldNames() {
        Field[] fields = getClass().getDeclaredFields();

        return Arrays.stream(fields)
                .filter(field -> !Modifier.isStatic(field.getModifiers())
                        && !field.isSynthetic()
                        && !field.isAnnotationPresent(IgnoreOrm.class))
                .map(Field::getName)
                .toArray(String[]::new);
    }

    default Object[] getFieldValues() {
        Field[] fields = getClass().getDeclaredFields();

        return Arrays.stream(fields)
                .filter(field -> !Modifier.isStatic(field.getModifiers())
                        && !field.isSynthetic()
                        && !field.isAnnotationPresent(IgnoreOrm.class))
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(this);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .toArray();
    }
}
