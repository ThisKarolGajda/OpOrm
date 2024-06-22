package me.opkarol.oporm;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class OpOrm {
    private final HikariDataSource dataSource;
    private Connection connection;

    public OpOrm(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(25);

        dataSource = new HikariDataSource(config);
        connection = null;
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = dataSource.getConnection();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public <PK extends Serializable> void save(DatabaseEntity<PK> entity) {
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

    private void executeUpdateQuery(@NotNull DatabaseEntity<?> entity, boolean isUpdate) {
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
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            for (int i = 0; i < fieldValues.length; i++) {
                if (fieldValues[i] instanceof SerializableFieldOrm serializableFieldOrm) {
                    // Serialize the value if it implements SerializableField
                    statement.setString(i + 1, serializableFieldOrm.serialize());
                } else {
                    statement.setObject(i + 1, fieldValues[i]);
                }
            }
            if (isUpdate) {
                statement.setObject(fieldValues.length + 1, entity.getId());
            }

            statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insert(DatabaseEntity<?> entity) {
        executeUpdateQuery(entity, false);
    }

    private void update(DatabaseEntity<?> entity) {
        executeUpdateQuery(entity, true);
    }

    /**
     * Create a table in the database based on the fields of a DatabaseEntity.
     *
     * @param entityClass The class of the DatabaseEntity.
     */
    public void createTable(@NotNull Class<? extends DatabaseEntity<?>> entityClass) {
        if (!Serializable.class.isAssignableFrom(entityClass)) {
            throw new IllegalArgumentException("Class must implement Serializable: " + entityClass.getName());
        }

        String tableName = entityClass.getSimpleName();
        StringBuilder queryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (shouldIncludeField(field)) {
                String fieldName = field.getName();
                String fieldType = getDatabaseType(field.getType());
                queryBuilder.append(fieldName).append(" ").append(fieldType).append(", ");
            }
        }

        queryBuilder.delete(queryBuilder.length() - 2, queryBuilder.length()); // Remove the last comma and space
        queryBuilder.append(") DEFAULT CHARSET=utf8");

        String query = queryBuilder.toString();

        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldIncludeField(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && !field.isSynthetic()
                && !field.isAnnotationPresent(IgnoreOrm.class);
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

    public <PK extends Serializable, T extends DatabaseEntity<PK>> T findById(@NotNull Class<T> entityClass, PK id) {
        String tableName = entityClass.getSimpleName();
        String primaryKeyFieldName = getPrimaryKeyFieldName(entityClass);

        // Use the primary key field in the WHERE clause
        String query = "SELECT * FROM " + tableName + " WHERE " + primaryKeyFieldName + " = ?";

        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            setPrimaryKeyParameter(statement, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                T instance = null;
                if (resultSet.next()) {
                    instance = entityClass.getDeclaredConstructor().newInstance();
                    Field[] fields = entityClass.getDeclaredFields();

                    for (Field field : fields) {
                        if (shouldIncludeField(field)) {
                            field.setAccessible(true);
                            if (SerializableFieldOrm.class.isAssignableFrom(field.getType())) {
                                SerializableFieldOrm serializableFieldOrm = (SerializableFieldOrm) field.getType().newInstance();
                                field.set(instance, serializableFieldOrm.deserialize(resultSet.getString(field.getName())));
                            } else {
                                Object value = resultSet.getObject(field.getName());
                                field.set(instance, value);
                            }
                        }
                    }
                }

                return instance;
            }

        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public <PK extends Serializable> void deleteById(@NotNull Class<? extends DatabaseEntity<?>> entityClass, PK id) {
        String tableName = entityClass.getSimpleName();
        String primaryKeyFieldName = getPrimaryKeyFieldName(entityClass);

        // Query to delete the record by primary key
        String deleteQuery = "DELETE FROM " + tableName + " WHERE " + primaryKeyFieldName + " = ?";

        try (PreparedStatement statement = getConnection().prepareStatement(deleteQuery)) {
            setPrimaryKeyParameter(statement, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void beginTransaction() {
        try (Connection connection = getConnection()) {
            if (connection != null && connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void commitTransaction() {
        try (Connection connection = getConnection()) {
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
        try (Connection connection = getConnection()) {
            if (connection != null && !connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public <T extends DatabaseEntity> List<T> findAll(@NotNull Class<T> entityClass) {
        List<T> resultList = new ArrayList<>();
        String tableName = entityClass.getSimpleName();
        String selectAllQuery = "SELECT * FROM " + tableName;

        try (PreparedStatement statement = getConnection().prepareStatement(selectAllQuery);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                T instance = entityClass.getDeclaredConstructor().newInstance();
                Field[] fields = entityClass.getDeclaredFields();

                for (Field field : fields) {
                    if (shouldIncludeField(field)) {
                        field.setAccessible(true);

                        if (SerializableFieldOrm.class.isAssignableFrom(field.getType())) {
                            SerializableFieldOrm serializableFieldOrm = (SerializableFieldOrm) field.getType().newInstance();
                            field.set(instance, serializableFieldOrm.deserialize(resultSet.getString(field.getName())));

                        } else {
                            Object value = resultSet.getObject(field.getName());
                            field.set(instance, value);
                        }
                    }
                }

                resultList.add(instance);
            }

        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 java.lang.reflect.InvocationTargetException e) {
            if (e.getClass().equals(SQLSyntaxErrorException.class)) {
                Bukkit.getLogger().info("[OpOrm] It is recommended to restart your server! New MySql table had been created!!");
            } else {
                e.printStackTrace();
            }
        }

        return resultList;
    }

    private <PK extends Serializable> void setPrimaryKeyParameter(PreparedStatement statement, PK id) throws SQLException {
        if (id instanceof String) {
            statement.setString(1, (String) id);
        } else if (id instanceof Integer) {
            statement.setInt(1, (Integer) id);
        } else if (id instanceof Long) {
            statement.setLong(1, (Long) id);
        } else if (id instanceof UUID) {
            statement.setString(1, id.toString());
        } else {
            statement.setObject(1, id);
        }
    }

    private @NotNull String getPrimaryKeyFieldName(@NotNull Class<? extends DatabaseEntity> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                return field.getName();
            }
        }
        throw new IllegalArgumentException("No field annotated with @PrimaryKey found in " + entityClass.getSimpleName());
    }
}
