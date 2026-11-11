package de.testclient.gui;

import de.testclient.TestClient;
import de.testclient.module.ClientModule;
import de.testclient.util.Projector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    public static final int ACCENT = 0xFF4DA3FF;

    private HudRenderer() {
    }

    public static void outline(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    public static void render(DrawContext ctx, float tp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || TestClient.modules == null) return;
        TextRenderer tr = mc.textRenderer;

        // Overlays der Module (ESP, ChestESP, Hitbox ...)
        if (Projector.begin(tp)) {
            for (ClientModule m : TestClient.modules.modules) {
                if (m.isEnabled()) m.onHud(ctx, tp);
            }
        }

        // Wasserzeichen
        String title = "TestClient " + TestClient.VERSION;
        ctx.fill(4, 4, 4 + tr.getWidth(title) + 8, 17, 0xB0101216);
        ctx.fill(4, 4, 6, 17, ACCENT);
        ctx.drawText(tr, title, 10, 7, ACCENT, true);

        // ArrayList (aktive Module, rechts oben)
        List<ClientModule> on = new ArrayList<>();
        for (ClientModule m : TestClient.modules.modules) {
            if (m.isEnabled()) on.add(m);
        }
        on.sort(Comparator.comparingInt((ClientModule m) -> -tr.getWidth(m.name)));
        int sw = mc.getWindow().getScaledWidth();
        int y = 4;
        for (ClientModule m : on) {
            int tw = tr.getWidth(m.name);
            int x = sw - tw - 8;
            ctx.fill(x - 2, y, sw, y + 11, 0xA0101216);
            ctx.fill(sw - 2, y, sw, y + 11, ACCENT);
            ctx.drawText(tr, m.name, x, y + 2, ACCENT, true);
            y += 11;
        }
    }
}
