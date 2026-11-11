package de.testclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting {
    public final double min, max, step;
    private double value;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        set(def);
    }

    public double get() {
        return value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round(v / step) * step;
        value = Math.round(v * 1000.0) / 1000.0;
    }

    public double progress() {
        return (value - min) / (max - min);
    }

    public void setProgress(double p) {
        set(min + (max - min) * Math.max(0, Math.min(1, p)));
    }

    public String display() {
        if (step >= 1) return String.valueOf((int) Math.round(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement e) {
        set(e.getAsDouble());
    }
}
