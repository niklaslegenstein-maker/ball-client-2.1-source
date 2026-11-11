package de.testclient.modules;

import de.testclient.gui.HudRenderer;
import de.testclient.module.Category;
import de.testclient.module.ClientModule;
import de.testclient.setting.BoolSetting;
import de.testclient.setting.ModeSetting;
import de.testclient.setting.NumberSetting;
import de.testclient.util.Projector;
import de.testclient.util.TargetUtil;
import de.testclient.util.Util;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class CombatModules {
    private CombatModules() {
    }

    // ------------------------------------------------------------ KillAura
    public static class KillAura extends ClientModule {
        private final NumberSetting range = add(new NumberSetting("Range", 4.2, 1.0, 6.5, 0.1));
        private final BoolSetting players = add(new BoolSetting("Players", true));
        private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
        private final BoolSetting animals = add(new BoolSetting("Animals", false));
        private final BoolSetting invisible = add(new BoolSetting("Invisible", false));
        private final BoolSetting walls = add(new BoolSetting("Through Walls", false));
        private final BoolSetting rotate = add(new BoolSetting("Rotate", true));
        private final BoolSetting cooldown = add(new BoolSetting("Wait Cooldown", true));
        private final NumberSetting cps = add(new NumberSetting("CPS", 10, 1, 20, 1));
        private final ModeSetting sort = add(new ModeSetting("Sort", "Distance", "Distance", "Health", "Angle"));
        private long last;

        public KillAura() {
            super("KillAura", "Automatisches Angreifen", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            if (mc.currentScreen != null) return;
            ClientPlayerEntity p = mc.player;
            LivingEntity t = TargetUtil.find(range.get(), players.get(), mobs.get(), animals.get(),
                    invisible.get(), walls.get(), 360, sort.get());
            if (t == null) return;
            if (rotate.get()) {
                float[] r = Util.rotationsTo(t.getBoundingBox().getCenter());
                p.setYaw(r[0]);
                p.setPitch(r[1]);
            }
            if (cooldown.get()) {
                if (p.getAttackCooldownProgress(0.5f) < 1.0f) return;
            } else {
                long now = System.currentTimeMillis();
                if (now - last < 1000.0 / cps.get()) return;
                last = now;
            }
            Util.attack(t);
        }
    }

    // ----------------------------------------------------------- AimAssist
    public static class AimAssist extends ClientModule {
        private final NumberSetting range = add(new NumberSetting("Range", 5.0, 1.0, 8.0, 0.1));
        private final NumberSetting fov = add(new NumberSetting("FOV", 90, 10, 360, 5));
        private final NumberSetting speed = add(new NumberSetting("Speed", 0.25, 0.05, 1.0, 0.05));
        private final BoolSetting onlyAttacking = add(new BoolSetting("Only When Attacking", true));
        private final BoolSetting players = add(new BoolSetting("Players", true));
        private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
        private final BoolSetting animals = add(new BoolSetting("Animals", false));
        private final BoolSetting walls = add(new BoolSetting("Through Walls", false));

        public AimAssist() {
            super("AimAssist", "Unterstuetzung beim Zielen", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            if (mc.currentScreen != null) return;
            if (onlyAttacking.get() && !mc.options.attackKey.isPressed()) return;
            ClientPlayerEntity p = mc.player;
            LivingEntity t = TargetUtil.find(range.get(), players.get(), mobs.get(), animals.get(),
                    false, walls.get(), fov.get(), "Angle");
            if (t == null) return;
            float[] r = Util.rotationsTo(t.getBoundingBox().getCenter());
            float dy = MathHelper.wrapDegrees(r[0] - p.getYaw());
            float dp = r[1] - p.getPitch();
            double s = speed.get();
            p.setYaw((float) (p.getYaw() + dy * s));
            p.setPitch(MathHelper.clamp((float) (p.getPitch() + dp * s), -90f, 90f));
        }
    }

    // ----------------------------------------------------------- TriggerBot
    public static class TriggerBot extends ClientModule {
        private final BoolSetting players = add(new BoolSetting("Players", true));
        private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
        private final BoolSetting animals = add(new BoolSetting("Animals", false));
        private final NumberSetting minCooldown = add(new NumberSetting("Min Cooldown", 0.95, 0.1, 1.0, 0.05));

        public TriggerBot() {
            super("TriggerBot", "Greift automatisch an, wenn ein Ziel im Fadenkreuz ist", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            if (mc.currentScreen != null) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
            Entity e = hit.getEntity();
            if (!TargetUtil.matches(e, players.get(), mobs.get(), animals.get(), true)) return;
            if (mc.player.getAttackCooldownProgress(0.5f) < minCooldown.get()) return;
            Util.attack(e);
        }
    }

    // ---------------------------------------------------------------- Reach
    public static class Reach extends ClientModule {
        private final NumberSetting entityReach = add(new NumberSetting("Entity Reach", 4.5, 3.0, 6.0, 0.1));
        private final NumberSetting blockReach = add(new NumberSetting("Block Reach", 5.0, 4.5, 8.0, 0.1));

        public Reach() {
            super("Reach", "Erhoehte Angriffs- und Bloeckreichweite", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            MovementModules.setAttribute(EntityAttributes.ENTITY_INTERACTION_RANGE, entityReach.get());
            MovementModules.setAttribute(EntityAttributes.BLOCK_INTERACTION_RANGE, blockReach.get());
        }

        @Override
        protected void onDisable() {
            MovementModules.setAttribute(EntityAttributes.ENTITY_INTERACTION_RANGE, 3.0);
            MovementModules.setAttribute(EntityAttributes.BLOCK_INTERACTION_RANGE, 4.5);
        }
    }

    // ------------------------------------------------------------- Velocity
    public static class Velocity extends ClientModule {
        private final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 200, 5));
        private final NumberSetting vertical = add(new NumberSetting("Vertical %", 0, 0, 200, 5));

        public Velocity() {
            super("Velocity", "Rueckstoss beeinflussen (in Prozent)", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (p.maxHurtTime > 0 && p.hurtTime == p.maxHurtTime) {
                Vec3d v = p.getVelocity();
                double h = horizontal.get() / 100.0;
                p.setVelocity(v.x * h, v.y * (vertical.get() / 100.0), v.z * h);
            }
        }
    }

    // ------------------------------------------------------------ Criticals
    public static class Criticals extends ClientModule {
        public static Criticals INSTANCE;
        private final NumberSetting height = add(new NumberSetting("Height", 0.0625, 0.01, 0.25, 0.005));

        public Criticals() {
            super("Criticals", "Kritische Treffer automatisieren", Category.COMBAT);
            INSTANCE = this;
        }

        /** Wird von ClientPlayerInteractionManagerMixin vor jedem Angriff aufgerufen. */
        public static void onAttack(Entity target) {
            Criticals c = INSTANCE;
            if (c == null || !c.isEnabled()) return;
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            if (!p.isOnGround() || p.isTouchingWater() || p.isClimbing() || p.isInLava()) return;
            double h = c.height.get();
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    p.getX(), p.getY() + h, p.getZ(), false, p.horizontalCollision));
            p.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    p.getX(), p.getY(), p.getZ(), false, p.horizontalCollision));
        }
    }

    // ------------------------------------------------------------ AutoTotem
    public static class AutoTotem extends ClientModule {
        private final NumberSetting health = add(new NumberSetting("Health", 20, 1, 20, 1));
        private final NumberSetting delay = add(new NumberSetting("Delay (Ticks)", 0, 0, 10, 1));
        private int wait;

        public AutoTotem() {
            super("AutoTotem", "Totem automatisch in die Hand nehmen", Category.COMBAT);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
            if (p.getHealth() > health.get()) return;
            if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            if (wait > 0) {
                wait--;
                return;
            }
            int slot = -1;
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return;
            int handlerSlot = slot < 9 ? 36 + slot : slot;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, handlerSlot, 40, SlotActionType.SWAP, p);
            wait = delay.getInt();
        }
    }

    // ---------------------------------------------------------- AutoClicker
    public static class AutoClicker extends ClientModule {
        private final NumberSetting minCps = add(new NumberSetting("Min CPS", 8, 1, 20, 1));
        private final NumberSetting maxCps = add(new NumberSetting("Max CPS", 12, 1, 20, 1));
        private final BoolSetting cooldown = add(new BoolSetting("Respect Cooldown", false));
        private final Random random = new Random();
        private long next;

        public AutoClicker() {
            super("AutoClicker", "Automatisches Klicken (linke Maustaste halten)", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            if (mc.currentScreen != null || !mc.options.attackKey.isPressed()) return;
            if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
            long now = System.currentTimeMillis();
            if (now < next) return;
            if (cooldown.get() && mc.player.getAttackCooldownProgress(0.5f) < 0.9f) return;
            Util.attack(hit.getEntity());
            double lo = Math.min(minCps.get(), maxCps.get());
            double hi = Math.max(minCps.get(), maxCps.get());
            double cps = lo + random.nextDouble() * (hi - lo);
            next = now + (long) (1000.0 / cps);
        }
    }

    // --------------------------------------------------------------- Hitbox
    public static class Hitbox extends ClientModule {
        public static Hitbox INSTANCE;
        private final NumberSetting expand = add(new NumberSetting("Expand", 0.2, 0.0, 1.0, 0.05));
        private final BoolSetting playersOnly = add(new BoolSetting("Players Only", true));
        private final BoolSetting show = add(new BoolSetting("Show Boxes", true));

        public Hitbox() {
            super("Hitbox", "Trefferboxen anzeigen / vergroessern", Category.COMBAT);
            INSTANCE = this;
        }

        /** Wird von EntityMixin (getTargetingMargin) genutzt. */
        public static float margin(Entity e) {
            Hitbox h = INSTANCE;
            if (h == null || !h.isEnabled() || !mc.isOnThread() || e == mc.player) return 0f;
            if (!(e instanceof LivingEntity)) return 0f;
            if (h.playersOnly.get() && !(e instanceof PlayerEntity)) return 0f;
            return (float) h.expand.get();
        }

        @Override
        public void onHud(DrawContext ctx, float tp) {
            if (!show.get()) return;
            for (Entity e : mc.world.getEntities()) {
                if (!TargetUtil.matches(e, true, true, true, true)) continue;
                if (playersOnly.get() && !(e instanceof PlayerEntity)) continue;
                if (mc.player.squaredDistanceTo(e) > 48 * 48) continue;
                float[] r = Projector.rect(Projector.entityBox(e, tp).expand(expand.get()));
                if (r == null) continue;
                HudRenderer.outline(ctx, (int) r[0], (int) r[1], (int) r[2], (int) r[3], 0xFFFFD84D);
            }
        }
    }

    // -------------------------------------------------------- AntiKnockback
    public static class AntiKnockback extends ClientModule {
        private final NumberSetting strength = add(new NumberSetting("Strength %", 100, 0, 100, 5));

        public AntiKnockback() {
            super("AntiKnockback", "Rueckstoss waehrend der Trefferphase reduzieren", Category.COMBAT);
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            if (p.hurtTime <= 0) return;
            double k = 1.0 - strength.get() / 100.0;
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x * k, v.y, v.z * k);
        }
    }
}
