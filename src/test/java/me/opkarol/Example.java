package me.opkarol;

import me.opkarol.oporm.DatabaseEntity;
import me.opkarol.oporm.Id;
import me.opkarol.oporm.IgnoreOrm;

import java.io.Serializable;

public class Example implements DatabaseEntity, Serializable {
    @Id
    private int id;
    @IgnoreOrm
    private String name;

    public Example(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Example(String name) {
        this.name = name;
    }

    public Example() {

    }

    @Override
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}