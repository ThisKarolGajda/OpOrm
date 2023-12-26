package me.opkarol;

import me.opkarol.oporm.OpOrm;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ComplicatedOrmTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/orm-test";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Test
    public void complicatedOrmTest() {
        // Preparation
        ComplicatedExample object = new ComplicatedExample(new ComplicatedSerializableObject(Map.of(ComplicatedSerializableObject.Type.ANIMALS_GROWTH_UPGRADE, 1)), "jęćzmiońąś");
        OpOrm orm = new OpOrm(DB_URL, DB_USER, DB_PASSWORD);

        orm.createTable(object.getClass());
        orm.addToNextFreeId(object);

        ComplicatedExample returnedObject = orm.findById(object.getClass(), object.getId());

        assertNotNull(returnedObject);
        assertEquals(object.getId(), returnedObject.getId());
        assertEquals(object.getName(), returnedObject.getName());
        assertEquals(object.getObject().serialize(), returnedObject.getObject().serialize());

    }
}
