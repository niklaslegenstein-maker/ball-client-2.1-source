package de.testclient.setting;

import com.google.gson.JsonElement;

public abstract class Setting {
    public final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement e);
}
