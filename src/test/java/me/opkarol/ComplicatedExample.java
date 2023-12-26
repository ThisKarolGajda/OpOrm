package me.opkarol;

import me.opkarol.oporm.DatabaseEntity;
import me.opkarol.oporm.Id;
import me.opkarol.oporm.SerializableOrm;

import java.io.Serializable;

public class ComplicatedExample implements DatabaseEntity, Serializable {
    @Id
    private int id;
    @SerializableOrm
    private ComplicatedSerializableObject object;
    private String name;

    public ComplicatedExample(int id, ComplicatedSerializableObject object, String name) {
        this.id = id;
        this.object = object;
        this.name = name;
    }

    public ComplicatedExample(ComplicatedSerializableObject object, String name) {
        this.object = object;
        this.name = name;
    }

    public ComplicatedExample() {

    }

    @Override
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ComplicatedSerializableObject getObject() {
        return object;
    }
}
