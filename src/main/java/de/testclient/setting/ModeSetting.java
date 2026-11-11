package de.testclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting {
    public final String[] modes;
    private int index;

    public ModeSetting(String name, String def, String... modes) {
        super(name);
        this.modes = modes;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(def)) index = i;
        }
    }

    public String get() {
        return modes[index];
    }

    public boolean is(String m) {
        return modes[index].equalsIgnoreCase(m);
    }

    public void cycle(int dir) {
        index = (index + dir + modes.length) % modes.length;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement e) {
        String s = e.getAsString();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(s)) index = i;
        }
    }
}
