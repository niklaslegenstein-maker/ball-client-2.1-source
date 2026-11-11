package de.testclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.testclient.module.ClientModule;
import de.testclient.module.ModuleManager;
import de.testclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/** Speichert Einstellungen und Tastenbelegungen in config/testclient.json. */
public final class Config {
    private static final Logger LOG = LoggerFactory.getLogger("TestClient");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("testclient.json");
    }

    public static void load(ModuleManager mm) {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (ClientModule m : mm.modules) {
                JsonObject jm = root.getAsJsonObject(m.name);
                if (jm == null) continue;
                if (jm.has("key")) m.key = jm.get("key").getAsInt();
                JsonObject js = jm.getAsJsonObject("settings");
                if (js == null) continue;
                for (Setting s : m.settings) {
                    JsonElement e = js.get(s.name);
                    if (e != null) s.fromJson(e);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Konfiguration konnte nicht geladen werden", ex);
        }
    }

    public static void save(ModuleManager mm) {
        try {
            JsonObject root = new JsonObject();
            for (ClientModule m : mm.modules) {
                JsonObject jm = new JsonObject();
                jm.addProperty("key", m.key);
                JsonObject js = new JsonObject();
                for (Setting s : m.settings) js.add(s.name, s.toJson());
                jm.add("settings", js);
                root.add(m.name, jm);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception ex) {
            LOG.warn("Konfiguration konnte nicht gespeichert werden", ex);
        }
    }
}
