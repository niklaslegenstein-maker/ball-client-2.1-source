package de.testclient.modules;

import de.testclient.module.Category;
import de.testclient.module.ClientModule;
import de.testclient.setting.BoolSetting;
import de.testclient.setting.NumberSetting;
import de.testclient.util.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;

public final class MovementModules {
    private MovementModules() {
    }

    static void setAttribute(RegistryEntry<EntityAttribute> attr, double value) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    // ------------------------------------------------------------------ Fly
    public static class Fly extends ClientModule {
        private final NumberSetting speed = add(new NumberSetting("Speed", 0.8, 0.1, 5.0, 0.1));
        private final NumberSetting vertical = add(new NumberSetting("Vertical", 0.5, 0.1, 3.0, 0.1));
        private final BoolSetting antiKick = add(new BoolSetting("AntiKick", true));
        private int ticks;

        public Fly() {
            super("Fly", "Fliegen (Leertaste hoch, Shift runter)", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            Vec3d h = Util.moveVec(speed.get());
            double vy = 0;
            if (mc.options.jumpKey.isPressed()) vy += vertical.get();
            if (mc.options.sneakKey.isPressed()) vy -= vertical.get();
            if (vy == 0 && antiKick.get() && ++ticks % 20 == 0) vy = -0.05;
            p.setVelocity(h.x, vy, h.z);
        }
    }

    // ---------------------------------------------------------------- Speed
    public static class Speed extends ClientModule {
        private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.1, 2.0, 0.05));
        private final BoolSetting onlyGround = add(new BoolSetting("Only Ground", false));
        private final BoolSetting bhop = add(new BoolSetting("BunnyHop", false));

        public Speed() {
            super("Speed", "Schneller laufen", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (!Util.moving()) return;
            if (bhop.get() && p.isOnGround()) mc.options.jumpKey.setPressed(true);
            if (onlyGround.get() && !p.isOnGround()) return;
            Vec3d h = Util.moveVec(speed.get());
            p.setVelocity(h.x, p.getVelocity().y, h.z);
        }
    }

    // --------------------------------------------------------------- Sprint
    public static class Sprint extends ClientModule {
        public Sprint() {
            super("Sprint", "Automatisch sprinten", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && p.getHungerManager().getFoodLevel() > 6) {
                mc.options.sprintKey.setPressed(true);
            }
        }
    }

    // --------------------------------------------------------------- NoFall
    public static class NoFall extends ClientModule {
        private final NumberSetting distance = add(new NumberSetting("Fall Distance", 2.5, 1.0, 10.0, 0.5));

        public NoFall() {
            super("NoFall", "Kein Fallschaden", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (!p.isOnGround() && p.fallDistance > distance.get()) {
                p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            }
        }
    }

    // ---------------------------------------------------------------- Jesus
    public static class Jesus extends ClientModule {
        private final NumberSetting lift = add(new NumberSetting("Lift", 0.11, 0.02, 0.3, 0.01));

        public Jesus() {
            super("Jesus", "Ueber Wasser laufen (Shift = tauchen)", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (p.isSneaking() || !p.isTouchingWater()) return;
            Vec3d v = p.getVelocity();
            double vy = mc.options.jumpKey.isPressed() ? 0.2 : lift.get();
            p.setVelocity(v.x, vy, v.z);
        }
    }

    // ----------------------------------------------------------------- Step
    public static class Step extends ClientModule {
        private final NumberSetting height = add(new NumberSetting("Height", 1.0, 0.6, 5.0, 0.1));

        public Step() {
            super("Step", "Hoehere Bloecke automatisch erklimmen", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            setAttribute(EntityAttributes.STEP_HEIGHT, height.get());
        }

        @Override
        protected void onDisable() {
            setAttribute(EntityAttributes.STEP_HEIGHT, 0.6);
        }
    }

    // --------------------------------------------------------------- Spider
    public static class Spider extends ClientModule {
        private final NumberSetting climb = add(new NumberSetting("Climb Speed", 0.2, 0.05, 0.6, 0.01));

        public Spider() {
            super("Spider", "An Waenden hochklettern", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (p.horizontalCollision && !p.isSneaking()) {
                Vec3d v = p.getVelocity();
                p.setVelocity(v.x, climb.get(), v.z);
            }
        }
    }

    // ------------------------------------------------------------- HighJump
    public static class HighJump extends ClientModule {
        private final NumberSetting power = add(new NumberSetting("Power", 0.8, 0.42, 2.0, 0.02));

        public HighJump() {
            super("HighJump", "Hoeher springen", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            setAttribute(EntityAttributes.JUMP_STRENGTH, power.get());
        }

        @Override
        protected void onDisable() {
            setAttribute(EntityAttributes.JUMP_STRENGTH, 0.42);
        }
    }

    // ------------------------------------------------------------- LongJump
    public static class LongJump extends ClientModule {
        private final NumberSetting boost = add(new NumberSetting("Boost", 0.7, 0.2, 2.0, 0.05));
        private boolean wasGround;

        public LongJump() {
            super("LongJump", "Weiter springen", Category.MOVEMENT);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            boolean ground = p.isOnGround();
            if (wasGround && !ground && p.getVelocity().y > 0 && Util.moving()) {
                Vec3d h = Util.moveVec(boost.get());
                p.setVelocity(h.x, p.getVelocity().y, h.z);
            }
            wasGround = ground;
        }
    }

    // -------------------------------------------------------------- Dolphin
    public static class Dolphin extends ClientModule {
        private final NumberSetting speed = add(new NumberSetting("Speed", 0.35, 0.1, 1.5, 0.05));

        public Dolphin() {
            super("Dolphin", "Schneller schwimmen", Category.MOVEMENT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (!p.isTouchingWater() || !Util.moving()) return;
            Vec3d h = Util.moveVec(speed.get());
            p.setVelocity(h.x, p.getVelocity().y, h.z);
        }
    }
}
