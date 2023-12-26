package me.opkarol.oporm;

public interface SerializableFieldOrm {
    String serialize();
    Object deserialize(String value);
}