package me.opkarol;

import me.opkarol.oporm.OpORM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ORMTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/orm-test";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Test
    void testSaveAndFindById() {
        // Preparation
        final int id = 2;
        OpORM orm = new OpORM(DB_URL, DB_USER, DB_PASSWORD);

        orm.createTable(Example.class);
        orm.save(new Example(id, ""));
        // Test

        orm.deleteById(Example.class, id);

        Example example = orm.findById(Example.class, id);

        assertNull(example);
    }

    @Test
    void testAddingToNextFreeId() {
        final String name = "Testek1";
        OpORM orm = new OpORM(DB_URL, DB_USER, DB_PASSWORD);

        orm.createTable(Example.class);

        // me.opkarol.Example object
        Example example = new Example(name);

        // Save the object to the database
        orm.addToNextFreeId(example);

        int id = example.getId();

        // Retrieve the object by ID
        Example retrievedExample = orm.findById(Example.class, id);

        assertNotNull(retrievedExample);
        assertEquals(name, retrievedExample.getName());
    }

    @Test
    void testTransaction() {
        OpORM orm = new OpORM(DB_URL, DB_USER, DB_PASSWORD);

        // me.opkarol.Example class (assuming it has an id field and a name field)
        orm.createTable(Example.class);

        // Begin a transaction
        orm.beginTransaction();

        try {
            // Create an Example object with ID set to the next available ID
            Example example1 = new Example("ExampleObject1");
            orm.addToNextFreeId(example1);

            // Create another Example object with ID set to the next available ID
            Example example2 = new Example("ExampleObject2");
            orm.addToNextFreeId(example2);

            // Commit the transaction
            orm.commitTransaction();
        } catch (Exception e) {
            // Handle exceptions and rollback the transaction
            e.printStackTrace();
            orm.rollbackTransaction();
        }

        // Verify that the records were added successfully
        Example retrievedExample1 = orm.findById(Example.class, 1);
        Example retrievedExample2 = orm.findById(Example.class, 2);

        assertNotNull(retrievedExample1);
        assertNotNull(retrievedExample2);

        assertEquals("ExampleObject1", retrievedExample1.getName());
        assertEquals("ExampleObject2", retrievedExample2.getName());
    }
}