package me.opkarol;

import me.opkarol.oporm.SerializableFieldOrm;

import java.util.HashMap;
import java.util.Map;

public class ComplicatedSerializableObject implements SerializableFieldOrm {
    private Map<Type, Integer> levels;

    public ComplicatedSerializableObject(Map<Type, Integer> levels) {
        this.levels = levels;
    }

    public ComplicatedSerializableObject() {

    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlotUpgrades[");

        for (Type type : Type.values()) {
            sb.append(type.name()).append(":").append(1).append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");

        return sb.toString();
    }

    @Override
    public Object deserialize(String value) {
        String[] parts = value.substring(value.indexOf("[") + 1, value.indexOf("]")).split(",");
        Map<Type, Integer> levels = new HashMap<>();

        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                Type type = Type.valueOf(keyValue[0]);
                int level = Integer.parseInt(keyValue[1]);
                levels.put(type, level);
            }
        }

        return new ComplicatedSerializableObject(levels);
    }

    public Map<Type, Integer> getLevels() {
        return levels;
    }

    enum Type {
        PLOT_SIZE_UPGRADE,
        PLANTS_GROWTH_UPGRADE,
        ANIMALS_GROWTH_UPGRADE,
        PLAYER_LIMIT_UPGRADE;
    }
}
