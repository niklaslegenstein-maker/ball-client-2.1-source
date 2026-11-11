package de.testclient;

import de.testclient.gui.ClickGuiScreen;
import de.testclient.gui.HudRenderer;
import de.testclient.module.ClientModule;
import de.testclient.module.ModuleManager;
import de.testclient.util.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestClient implements ClientModInitializer {
    public static final String MOD_ID = "testclient";
    public static final String VERSION = "2.1.0";
    public static final Logger LOG = LoggerFactory.getLogger("TestClient");

    public static ModuleManager modules;

    private static KeyBinding menuKey;
    private final Map<ClientModule, Boolean> keyDown = new HashMap<>();

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        Config.load(modules);

        // Menue: Rechts-Shift (aenderbar unter Steuerung)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.testclient.menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(MOD_ID, "hud"),
                (ctx, tick) -> {
                    try {
                        HudRenderer.render(ctx, tick.getTickProgress(false));
                    } catch (Throwable t) {
                        LOG.error("HUD-Fehler", t);
                    }
                });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            for (ClientModule m : modules.modules) {
                if (!m.isEnabled()) continue;
                try {
                    m.onStartTick();
                } catch (Throwable t) {
                    fail(m, t);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            if (client.player == null || client.world == null) return;
            handleBinds(client);
            for (ClientModule m : modules.modules) {
                if (!m.isEnabled()) continue;
                try {
                    m.onTick();
                } catch (Throwable t) {
                    fail(m, t);
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save(modules));
    }

    private void handleBinds(MinecraftClient client) {
        if (client.currentScreen != null) return;
        long window = client.getWindow().getHandle();
        for (ClientModule m : modules.modules) {
            if (m.key < 0) continue;
            boolean down = GLFW.glfwGetKey(window, m.key) == GLFW.GLFW_PRESS;
            boolean was = keyDown.getOrDefault(m, false);
            if (down && !was) m.toggle();
            keyDown.put(m, down);
        }
    }

    private static void fail(ClientModule m, Throwable t) {
        LOG.error("Modul {} verursachte einen Fehler und wird deaktiviert", m.name, t);
        try {
            m.setEnabled(false);
        } catch (Throwable ignored) {
        }
    }
}
