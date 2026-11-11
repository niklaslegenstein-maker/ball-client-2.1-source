package de.testclient.module;

public enum Category {
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    WORLD("World & Utility");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
