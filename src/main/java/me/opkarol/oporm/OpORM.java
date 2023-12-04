package me.opkarol.oporm;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;

public class OpORM {
    private final HikariDataSource dataSource;

    public OpORM(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
    }

    public void save(DatabaseEntity entity) {
        try {
            if (findById(entity.getClass(), entity.getId()) == null) {
                insert(entity);
            } else {
                update(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeUpdateQuery(@NotNull DatabaseEntity entity, boolean isUpdate) {
        String tableName = entity.getClass().getSimpleName();
        String[] fieldNames = entity.getFieldNames();
        Object[] fieldValues = entity.getFieldValues();

        StringBuilder queryBuilder;
        if (isUpdate) {
            queryBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
            for (String fieldName : fieldNames) {
                queryBuilder.append(fieldName).append(" = ?, ");
            }
            queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length()); // Remove the last comma and space
            queryBuilder.append(" WHERE ").append(getPrimaryKeyFieldName(entity.getClass())).append(" = ?");
        } else {
            queryBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
            for (String fieldName : fieldNames) {
                queryBuilder.append(fieldName).append(", ");
            }
            queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length()); // Remove the last comma and space
            queryBuilder.append(") VALUES (");

            queryBuilder.append("?, ".repeat(fieldValues.length));
            queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length()); // Remove the last comma and space
            queryBuilder.append(")");
        }

        String query = queryBuilder.toString();

        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(query)) {
            for (int i = 0; i < fieldValues.length; i++) {
                statement.setObject(i + 1, fieldValues[i]);
            }
            if (isUpdate) {
                // Assuming there's a method to get the ID from an object
                statement.setObject(fieldValues.length + 1, entity.getId());
            }
            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private @NotNull String getPrimaryKeyFieldName(@NotNull Class<? extends DatabaseEntity> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(Id.class)) {
                    return field.getName();
                }
            }
        }
        throw new IllegalArgumentException("No field annotated with @Id found in " + entityClass.getSimpleName());
    }


    private void insert(DatabaseEntity entity) {
        executeUpdateQuery(entity, false);
    }

    private void update(DatabaseEntity entity) {
        executeUpdateQuery(entity, true);
    }

    /**
     * Create a table in the database based on the fields of a DatabaseEntity.
     *
     * @param entityClass The class of the DatabaseEntity.
     */
    public void createTable(@NotNull Class<? extends DatabaseEntity> entityClass) {
        if (!Serializable.class.isAssignableFrom(entityClass)) {
            throw new IllegalArgumentException("Class must implement Serializable: " + entityClass.getName());
        }

        String tableName = entityClass.getSimpleName();
        StringBuilder queryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            String fieldType = getDatabaseType(field.getType());
            queryBuilder.append(fieldName).append(" ").append(fieldType).append(", ");
        }

        queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length()); // Remove the last comma and space
        queryBuilder.append(")");

        String query = queryBuilder.toString();

        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Contract(pure = true)
    private @NotNull String getDatabaseType(Class<?> fieldType) {
        if (fieldType == int.class || fieldType == Integer.class) {
            return "INT";
        } else if (fieldType == long.class || fieldType == Long.class) {
            return "BIGINT";
        } else if (fieldType == short.class || fieldType == Short.class) {
            return "SMALLINT";
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            return "TINYINT";
        } else if (fieldType == double.class || fieldType == Double.class) {
            return "DOUBLE";
        } else if (fieldType == float.class || fieldType == Float.class) {
            return "FLOAT";
        } else if (fieldType == String.class) {
            return "VARCHAR(255)";
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            return "BOOLEAN";
        } else if (fieldType == Date.class) {
            return "TIMESTAMP";
        }

        return "VARCHAR(255)"; // Default to VARCHAR if the type is not recognized
    }

    private <T extends DatabaseEntity> @Nullable T getObjectFromResultSet(@NotNull Class<T> entityClass, @Nullable ResultSet resultSet) {
        if (resultSet == null) {
            return null;
        }

        try {
            T instance = entityClass.getDeclaredConstructor().newInstance();

            if (resultSet.next()) {
                Field[] fields = entityClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                        field.setAccessible(true);
                        Object value = resultSet.getObject(field.getName());
                        field.set(instance, value);
                    }
                }
            } else {
                return null;
            }

            resultSet.close();
            return instance;
        } catch (InstantiationException | IllegalAccessException | SQLException | NoSuchMethodException | java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns open ResultSet.
     */
    private @Nullable ResultSet getResultSetById(@NotNull Class<? extends DatabaseEntity> entityClass, int id) {
        String tableName = entityClass.getSimpleName();
        String query = "SELECT * FROM " + tableName + " WHERE id = ?";

        try {
            PreparedStatement statement = dataSource.getConnection().prepareStatement(query);
            statement.setInt(1, id);
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public @Nullable <T extends DatabaseEntity> T findById(@NotNull Class<T> entityClass, int id) {
        ResultSet resultSet = getResultSetById(entityClass, id);
        return getObjectFromResultSet(entityClass, resultSet);
    }

    public <T extends DatabaseEntity> void addToNextFreeId(@NotNull T entity) {
        Class<? extends DatabaseEntity> entityClass = entity.getClass();
        String tableName = entityClass.getSimpleName();

        // Query to find the maximum ID in the table
        String maxIdQuery = "SELECT MAX(id) FROM " + tableName;

        try (Statement statement = dataSource.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(maxIdQuery);

            // Get the maximum ID from the result set
            if (resultSet.next()) {
                int maxId = resultSet.getInt(1);

                // Use reflection to set the ID field of the entity to the next available ID
                Field idField = entityClass.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, maxId + 1);

                // Save the entity to the database
                save(entity);
            }
        } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void deleteById(@NotNull Class<? extends DatabaseEntity> entityClass, int id) {
        String tableName = entityClass.getSimpleName();

        // Query to delete the record by ID
        String deleteQuery = "DELETE FROM " + tableName + " WHERE id = ?";

        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(deleteQuery)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void beginTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void commitTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.getAutoCommit()) {
                connection.commit();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            rollbackTransaction(); // Rollback in case of an exception during commit
        }
    }

    public void rollbackTransaction() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
