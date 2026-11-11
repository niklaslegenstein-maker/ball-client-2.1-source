package de.testclient.gui;

import de.testclient.TestClient;
import de.testclient.module.Category;
import de.testclient.module.ClientModule;
import de.testclient.setting.BoolSetting;
import de.testclient.setting.ModeSetting;
import de.testclient.setting.NumberSetting;
import de.testclient.setting.Setting;
import de.testclient.util.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** ClickGUI im Stil klassischer Hacked-Clients: ein Panel pro Kategorie. */
public class ClickGuiScreen extends Screen {
    private static final int PW = 132, HH = 18, MH = 15, SH = 13;

    private static final int C_ACCENT = HudRenderer.ACCENT;
    private static final int C_HEAD = 0xF0181C24;
    private static final int C_ROW = 0xD0121519;
    private static final int C_ROW_HOVER = 0xD01B1F26;
    private static final int C_ROW_ON = 0xD01F3A5C;
    private static final int C_SET = 0xD00D0F13;
    private static final int C_TEXT = 0xFFFFFFFF;
    private static final int C_DIM = 0xFF9AA3AF;

    private static final class Panel {
        final Category cat;
        int x, y;
        boolean open = true;

        Panel(Category cat, int x, int y) {
            this.cat = cat;
            this.x = x;
            this.y = y;
        }
    }

    private enum Kind {HEADER, MODULE, BOOL, NUMBER, MODE}

    private record Row(Kind kind, Panel panel, ClientModule module, Setting setting, int x, int y, int w, int h) {
    }

    // static: Panelpositionen bleiben zwischen dem Oeffnen erhalten
    private static final List<Panel> PANELS = new ArrayList<>();
    private static double scroll;

    static {
        int x = 20;
        for (Category c : Category.values()) {
            PANELS.add(new Panel(c, x, 20));
            x += PW + 12;
        }
    }

    private ClientModule listening;
    private Panel dragPanel;
    private int dragOffX, dragOffY;
    private NumberSetting dragSetting;
    private int dragX, dragW;

    public ClickGuiScreen() {
        super(Text.literal("TestClient"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save(TestClient.modules);
    }

    private List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();
        int sy = (int) scroll;
        for (Panel p : PANELS) {
            int y = p.y + sy;
            rows.add(new Row(Kind.HEADER, p, null, null, p.x, y, PW, HH));
            if (!p.open) continue;
            y += HH;
            for (ClientModule m : TestClient.modules.byCategory(p.cat)) {
                rows.add(new Row(Kind.MODULE, p, m, null, p.x, y, PW, MH));
                y += MH;
                if (m.expanded) {
                    for (Setting s : m.settings) {
                        Kind k = s instanceof BoolSetting ? Kind.BOOL
                                : s instanceof NumberSetting ? Kind.NUMBER : Kind.MODE;
                        rows.add(new Row(k, p, m, s, p.x, y, PW, SH));
                        y += SH;
                    }
                }
            }
        }
        return rows;
    }

    private static String keyName(int code) {
        if (code < 0) return "";
        switch (code) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            default: break;
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F12) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(code, 0);
        return n != null ? n.toUpperCase() : "K" + code;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x80000000);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ClientModule hoverModule = null;

        for (Row r : buildRows()) {
            boolean hover = mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h;
            switch (r.kind) {
                case HEADER -> {
                    ctx.fill(r.x, r.y, r.x + r.w, r.y + r.h, C_HEAD);
                    ctx.fill(r.x, r.y + r.h - 1, r.x + r.w, r.y + r.h, C_ACCENT);
                    ctx.drawText(tr, r.panel.cat.label, r.x + 6, r.y + 5, C_TEXT, true);
                    ctx.drawText(tr, r.panel.open ? "-" : "+", r.x + r.w - 10, r.y + 5, C_DIM, false);
                }
                case MODULE -> {
                    ClientModule m = r.module;
                    int bg = m.isEnabled() ? C_ROW_ON : (hover ? C_ROW_HOVER : C_ROW);
                    ctx.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
                    if (m.isEnabled()) ctx.fill(r.x, r.y, r.x + 2, r.y + r.h, C_ACCENT);
                    ctx.drawText(tr, m.name, r.x + 7, r.y + 4, m.isEnabled() ? C_TEXT : C_DIM, false);
                    String right = listening == m ? "[...]" : (m.key >= 0 ? "[" + keyName(m.key) + "]" : "");
                    int rx = r.x + r.w - 6;
                    if (!m.settings.isEmpty()) {
                        ctx.drawText(tr, m.expanded ? "v" : ">", r.x + r.w - 10, r.y + 4, C_DIM, false);
                        rx -= 8;
                    }
                    if (!right.isEmpty()) {
                        ctx.drawText(tr, right, rx - tr.getWidth(right), r.y + 4, C_ACCENT, false);
                    }
                    if (hover) hoverModule = m;
                }
                case BOOL -> {
                    BoolSetting s = (BoolSetting) r.setting;
                    ctx.fill(r.x, r.y, r.x + r.w, r.y + r.h, C_SET);
                    ctx.drawText(tr, s.name, r.x + 9, r.y + 3, C_DIM, false);
                    int bx = r.x + r.w - 14;
                    ctx.fill(bx, r.y + 3, bx + 8, r.y + 11, s.get() ? C_ACCENT : 0xFF2A2F38);
                }
                case NUMBER -> {
                    NumberSetting s = (NumberSetting) r.setting;
                    ctx.fill(r.x, r.y, r.x + r.w, r.y + r.h, C_SET);
                    ctx.fill(r.x, r.y, r.x + (int) (r.w * s.progress()), r.y + r.h, 0x704DA3FF);
                    ctx.drawText(tr, s.name, r.x + 9, r.y + 3, C_DIM, false);
                    String v = s.display();
                    ctx.drawText(tr, v, r.x + r.w - 6 - tr.getWidth(v), r.y + 3, C_TEXT, false);
                }
                case MODE -> {
                    ModeSetting s = (ModeSetting) r.setting;
                    ctx.fill(r.x, r.y, r.x + r.w, r.y + r.h, C_SET);
                    ctx.drawText(tr, s.name, r.x + 9, r.y + 3, C_DIM, false);
                    String v = s.get();
                    ctx.drawText(tr, v, r.x + r.w - 6 - tr.getWidth(v), r.y + 3, C_ACCENT, false);
                }
            }
        }

        if (hoverModule != null) {
            String tip = hoverModule.description;
            int w = tr.getWidth(tip);
            int tx = Math.min(mx + 8, this.width - w - 8);
            ctx.fill(tx - 3, my + 10, tx + w + 3, my + 23, 0xF0101216);
            ctx.drawText(tr, tip, tx, my + 13, C_TEXT, false);
        }
        String help = "LMB: an/aus   RMB: Einstellungen   MMB: Taste belegen   Scrollen: verschieben   RShift: schliessen";
        ctx.drawText(tr, help, 6, this.height - 12, C_DIM, false);
    }

    private void setFromMouse(double mouseX) {
        if (dragSetting == null) return;
        dragSetting.setProgress((mouseX - dragX) / (double) dragW);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        List<Row> rows = buildRows();
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row r = rows.get(i);
            if (mx < r.x || mx >= r.x + r.w || my < r.y || my >= r.y + r.h) continue;
            listening = null;
            switch (r.kind) {
                case HEADER -> {
                    if (btn == 0) {
                        dragPanel = r.panel;
                        dragOffX = (int) mx - r.panel.x;
                        dragOffY = (int) my - r.y;
                    } else if (btn == 1) {
                        r.panel.open = !r.panel.open;
                    }
                }
                case MODULE -> {
                    if (btn == 0) r.module.toggle();
                    else if (btn == 1 && !r.module.settings.isEmpty()) r.module.expanded = !r.module.expanded;
                    else if (btn == 2) listening = r.module;
                }
                case BOOL -> ((BoolSetting) r.setting).toggle();
                case NUMBER -> {
                    dragSetting = (NumberSetting) r.setting;
                    dragX = r.x;
                    dragW = r.w;
                    setFromMouse(mx);
                }
                case MODE -> ((ModeSetting) r.setting).cycle(btn == 1 ? -1 : 1);
            }
            return true;
        }
        listening = null;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragPanel != null) {
            dragPanel.x = (int) click.x() - dragOffX;
            dragPanel.y = (int) (click.y() - dragOffY - scroll);
            return true;
        }
        if (dragSetting != null) {
            setFromMouse(click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragPanel = null;
        dragSetting = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(-3000, Math.min(0, scroll + verticalAmount * 16));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int code = input.getKeycode();
        if (listening != null) {
            listening.key = (code == GLFW.GLFW_KEY_ESCAPE || code == GLFW.GLFW_KEY_DELETE
                    || code == GLFW.GLFW_KEY_BACKSPACE) ? -1 : code;
            listening = null;
            return true;
        }
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }
}
