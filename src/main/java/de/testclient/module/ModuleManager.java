package de.testclient.module;

import de.testclient.modules.CombatModules;
import de.testclient.modules.MovementModules;
import de.testclient.modules.WorldModules;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public final List<ClientModule> modules = new ArrayList<>();

    public ModuleManager() {
        // Movement
        modules.add(new MovementModules.Fly());
        modules.add(new MovementModules.Speed());
        modules.add(new MovementModules.Sprint());
        modules.add(new MovementModules.NoFall());
        modules.add(new MovementModules.Jesus());
        modules.add(new MovementModules.Step());
        modules.add(new MovementModules.Spider());
        modules.add(new MovementModules.HighJump());
        modules.add(new MovementModules.LongJump());
        modules.add(new MovementModules.Dolphin());
        // Combat
        modules.add(new CombatModules.KillAura());
        modules.add(new CombatModules.AimAssist());
        modules.add(new CombatModules.TriggerBot());
        modules.add(new CombatModules.Reach());
        modules.add(new CombatModules.Velocity());
        modules.add(new CombatModules.Criticals());
        modules.add(new CombatModules.AutoTotem());
        modules.add(new CombatModules.AutoClicker());
        modules.add(new CombatModules.Hitbox());
        modules.add(new CombatModules.AntiKnockback());
        // World & Utility
        modules.add(new WorldModules.XRay());
        modules.add(new WorldModules.ESP());
        modules.add(new WorldModules.FullBright());
        modules.add(new WorldModules.ChestESP());
        modules.add(new WorldModules.Freecam());
        modules.add(new WorldModules.Scaffold());
        modules.add(new WorldModules.InventoryMove());
        modules.add(new WorldModules.FastPlace());
        modules.add(new WorldModules.AutoFish());
        modules.add(new WorldModules.Nuker());
    }

    public List<ClientModule> byCategory(Category c) {
        List<ClientModule> out = new ArrayList<>();
        for (ClientModule m : modules) {
            if (m.category == c) out.add(m);
        }
        return out;
    }
}
