package de.testclient.module;

import de.testclient.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class ClientModule {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    /** GLFW-Tastencode, -1 = keine Taste */
    public int key = -1;
    /** Nur fuers GUI: Einstellungen aufgeklappt? */
    public boolean expanded;

    private boolean enabled;

    protected ClientModule(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Anfang jedes Client-Ticks (vor der Spielerbewegung). */
    public void onStartTick() {
    }

    /** Ende jedes Client-Ticks. */
    public void onTick() {
    }

    /** HUD-Overlay (2D). */
    public void onHud(DrawContext ctx, float tickProgress) {
    }
}
