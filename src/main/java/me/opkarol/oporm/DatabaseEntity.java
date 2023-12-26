package me.opkarol.oporm;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public interface DatabaseEntity {
    /**
     * Get the unique identifier (ID) of the database entity.
     *
     * @return The ID of the entity.
     */
    int getId();

    /**
     * Get an array of field names for the database entity.
     *
     * @return An array of field names.
     */
    default String[] getFieldNames() {
        Field[] fields = getClass().getDeclaredFields();

        return Arrays.stream(fields)
                .filter(field -> !Modifier.isStatic(field.getModifiers())
                        && !field.isSynthetic()
                        && !field.isAnnotationPresent(IgnoreOrm.class))
                .map(Field::getName)
                .toArray(String[]::new);
    }

    /**
     * Get an array of field values for the database entity.
     *
     * @return An array of field values.
     */
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
