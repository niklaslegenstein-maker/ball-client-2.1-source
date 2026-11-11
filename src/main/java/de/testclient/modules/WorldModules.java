package de.testclient.modules;

import de.testclient.gui.HudRenderer;
import de.testclient.module.Category;
import de.testclient.module.ClientModule;
import de.testclient.mixin.MinecraftClientAccessor;
import de.testclient.setting.BoolSetting;
import de.testclient.setting.ModeSetting;
import de.testclient.setting.NumberSetting;
import de.testclient.util.Projector;
import de.testclient.util.TargetUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldModules {
    private WorldModules() {
    }

    // ----------------------------------------------------------------- XRay
    public static class XRay extends ClientModule {
        public static XRay INSTANCE;
        private static final Map<Block, Boolean> CACHE = new ConcurrentHashMap<>();

        private final BoolSetting containers = add(new BoolSetting("Chests / Barrels", true));
        private final BoolSetting spawners = add(new BoolSetting("Spawners", true));

        public XRay() {
            super("X-Ray", "Erze durch Bloecke erkennen (nach Einstellungsaenderung kurz aus/an)", Category.WORLD);
            INSTANCE = this;
        }

        public static boolean active() {
            return INSTANCE != null && INSTANCE.isEnabled();
        }

        public static boolean isOre(BlockState s) {
            String p = Registries.BLOCK.getId(s.getBlock()).getPath();
            return p.endsWith("_ore") || p.equals("ancient_debris");
        }

        /** Wird vom Chunk-Mesher (Worker-Threads) aufgerufen. */
        public static boolean visible(BlockState s) {
            Block b = s.getBlock();
            Boolean cached = CACHE.get(b);
            if (cached != null) return cached;
            String p = Registries.BLOCK.getId(b).getPath();
            boolean v = p.endsWith("_ore") || p.equals("ancient_debris");
            XRay x = INSTANCE;
            if (x != null) {
                if (x.containers.get() && (p.equals("chest") || p.equals("trapped_chest") || p.equals("ender_chest")
                        || p.equals("barrel") || p.endsWith("shulker_box"))) v = true;
                if (x.spawners.get() && p.equals("spawner")) v = true;
            }
            CACHE.put(b, v);
            return v;
        }

        @Override
        protected void onEnable() {
            CACHE.clear();
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        }

        @Override
        protected void onDisable() {
            CACHE.clear();
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        }
    }

    // ------------------------------------------------------------------ ESP
    public static class ESP extends ClientModule {
        public static ESP INSTANCE;
        private final ModeSetting mode = add(new ModeSetting("Mode", "Both", "Glow", "Box", "Both"));
        private final BoolSetting players = add(new BoolSetting("Players", true));
        private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
        private final BoolSetting animals = add(new BoolSetting("Animals", false));
        private final BoolSetting names = add(new BoolSetting("Names", true));
        private final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 8));

        public ESP() {
            super("ESP", "Spieler und Mobs durch Waende erkennen", Category.WORLD);
            INSTANCE = this;
        }

        /** Wird von EntityMixin (isGlowing) genutzt. */
        public static boolean shouldGlow(Entity e) {
            ESP s = INSTANCE;
            if (s == null || !s.isEnabled() || !mc.isOnThread() || mc.player == null) return false;
            if (s.mode.is("Box")) return false;
            if (!TargetUtil.matches(e, s.players.get(), s.mobs.get(), s.animals.get(), true)) return false;
            return mc.player.squaredDistanceTo(e) <= s.range.get() * s.range.get();
        }

        private static int color(Entity e) {
            if (e instanceof PlayerEntity) return 0xFFFF4D4D;
            if (e instanceof Monster) return 0xFFFF9A3D;
            if (e instanceof PassiveEntity) return 0xFF5DDB6A;
            return 0xFFFFFFFF;
        }

        @Override
        public void onHud(DrawContext ctx, float tp) {
            if (mode.is("Glow")) return;
            double r2 = range.get() * range.get();
            for (Entity e : mc.world.getEntities()) {
                if (!TargetUtil.matches(e, players.get(), mobs.get(), animals.get(), true)) continue;
                if (mc.player.squaredDistanceTo(e) > r2) continue;
                float[] r = Projector.rect(Projector.entityBox(e, tp));
                if (r == null) continue;
                int c = color(e);
                HudRenderer.outline(ctx, (int) r[0], (int) r[1], (int) r[2], (int) r[3], c);
                if (names.get()) {
                    String label = e.getName().getString();
                    if (e instanceof LivingEntity le) label += " " + (int) Math.ceil(le.getHealth()) + "hp";
                    label += " " + (int) mc.player.distanceTo(e) + "m";
                    int w = mc.textRenderer.getWidth(label);
                    ctx.drawText(mc.textRenderer, label, (int) ((r[0] + r[2]) / 2 - w / 2), (int) r[1] - 10, c, true);
                }
            }
        }
    }

    // ----------------------------------------------------------- FullBright
    public static class FullBright extends ClientModule {
        public FullBright() {
            super("FullBright", "Dunkle Bereiche heller sehen", Category.WORLD);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (!p.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 100000, 0, false, false, false));
            }
        }

        @Override
        protected void onDisable() {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            StatusEffectInstance inst = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (inst != null && inst.getDuration() > 50000) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    // ------------------------------------------------------------- ChestESP
    public static class ChestESP extends ClientModule {
        private record Hit(Box box, int color, String label) {
        }

        private final BoolSetting chests = add(new BoolSetting("Chests", true));
        private final BoolSetting ender = add(new BoolSetting("Ender Chests", true));
        private final BoolSetting barrels = add(new BoolSetting("Barrels", false));
        private final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));
        private final NumberSetting range = add(new NumberSetting("Range", 64, 16, 256, 8));
        private List<Hit> hits = new ArrayList<>();
        private int timer;

        public ChestESP() {
            super("ChestESP", "Kisten durch Waende erkennen", Category.WORLD);
        }

        @Override
        protected void onEnable() {
            timer = 0;
        }

        @Override
        public void onTick() {
            if (timer-- > 0) return;
            timer = 10;
            List<Hit> list = new ArrayList<>();
            ClientPlayerEntity p = mc.player;
            int cr = (int) (range.get() / 16) + 1;
            int pcx = p.getBlockX() >> 4;
            int pcz = p.getBlockZ() >> 4;
            double r2 = range.get() * range.get();
            for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
                for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        BlockPos pos = be.getPos();
                        if (p.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r2) continue;
                        if (be instanceof EnderChestBlockEntity) {
                            if (ender.get()) list.add(new Hit(new Box(pos), 0xFFB04DFF, "Ender"));
                        } else if (be instanceof ChestBlockEntity) {
                            if (chests.get()) list.add(new Hit(new Box(pos), 0xFFFFC94D, "Chest"));
                        } else if (be instanceof BarrelBlockEntity) {
                            if (barrels.get()) list.add(new Hit(new Box(pos), 0xFFC98A4D, "Barrel"));
                        } else if (be instanceof ShulkerBoxBlockEntity) {
                            if (shulkers.get()) list.add(new Hit(new Box(pos), 0xFFFF6FD8, "Shulker"));
                        }
                    }
                }
            }
            hits = list;
        }

        @Override
        public void onHud(DrawContext ctx, float tp) {
            for (Hit h : hits) {
                float[] r = Projector.rect(h.box());
                if (r == null) continue;
                HudRenderer.outline(ctx, (int) r[0], (int) r[1], (int) r[2], (int) r[3], h.color());
                int w = mc.textRenderer.getWidth(h.label());
                ctx.drawText(mc.textRenderer, h.label(), (int) ((r[0] + r[2]) / 2 - w / 2), (int) r[1] - 10, h.color(), true);
            }
        }
    }

    // -------------------------------------------------------------- Freecam
    public static class Freecam extends ClientModule {
        public static Freecam INSTANCE;
        private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1));
        private Vec3d origin;
        private boolean f, b, l, r, u, d;

        public Freecam() {
            super("Freecam", "Kamera unabhaengig bewegen (Server sieht dich nicht bewegen)", Category.WORLD);
            INSTANCE = this;
        }

        /** Von ClientPlayNetworkHandlerMixin genutzt: Bewegungs-Pakete werden dann nicht gesendet. */
        public static boolean active() {
            return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.origin != null;
        }

        @Override
        protected void onEnable() {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            origin = new Vec3d(p.getX(), p.getY(), p.getZ());
            p.setNoGravity(true);
        }

        @Override
        protected void onDisable() {
            ClientPlayerEntity p = mc.player;
            if (p != null && origin != null) {
                p.setPosition(origin);
                p.setVelocity(Vec3d.ZERO);
                p.setNoGravity(false);
            }
            origin = null;
        }

        @Override
        public void onStartTick() {
            ClientPlayerEntity p = mc.player;
            f = mc.options.forwardKey.isPressed();
            b = mc.options.backKey.isPressed();
            l = mc.options.leftKey.isPressed();
            r = mc.options.rightKey.isPressed();
            u = mc.options.jumpKey.isPressed();
            d = mc.options.sneakKey.isPressed();
            // echten Spieler "stillhalten"
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);
            p.setVelocity(Vec3d.ZERO);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            double fw = (f ? 1 : 0) - (b ? 1 : 0);
            double st = (l ? 1 : 0) - (r ? 1 : 0);
            Vec3d h = de.testclient.util.Util.moveVec(fw, st, speed.get());
            double vy = ((u ? 1 : 0) - (d ? 1 : 0)) * speed.get();
            p.setPosition(p.getX() + h.x, p.getY() + vy, p.getZ() + h.z);
            p.setVelocity(Vec3d.ZERO);
            p.fallDistance = 0;
        }
    }

    // ------------------------------------------------------------- Scaffold
    public static class Scaffold extends ClientModule {
        private final NumberSetting delay = add(new NumberSetting("Delay (Ticks)", 0, 0, 10, 1));
        private final NumberSetting lookahead = add(new NumberSetting("Lookahead", 2.0, 0.0, 5.0, 0.5));
        private final BoolSetting silent = add(new BoolSetting("Switch Back", true));
        private int wait;

        public Scaffold() {
            super("Scaffold", "Automatisch Bloecke unter dir platzieren", Category.WORLD);
        }

        @Override
        public void onTick() {
            if (mc.currentScreen != null) return;
            if (wait > 0) {
                wait--;
                return;
            }
            ClientPlayerEntity p = mc.player;
            Vec3d v = p.getVelocity();
            BlockPos under = BlockPos.ofFloored(p.getX(), p.getY() - 1, p.getZ());
            BlockPos ahead = BlockPos.ofFloored(p.getX() + v.x * lookahead.get(), p.getY() - 1, p.getZ() + v.z * lookahead.get());
            BlockPos target = null;
            if (mc.world.getBlockState(under).isReplaceable()) target = under;
            else if (!ahead.equals(under) && mc.world.getBlockState(ahead).isReplaceable()) target = ahead;
            if (target == null) return;

            int slot = -1;
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getStack(i);
                if (s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof BlockWithEntity)
                        && !(bi.getBlock() instanceof FallingBlock)) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) return;

            for (Direction dir : Direction.values()) {
                BlockPos nb = target.offset(dir);
                BlockState st = mc.world.getBlockState(nb);
                if (st.isAir() || st.getCollisionShape(mc.world, nb).isEmpty()) continue;
                Direction face = dir.getOpposite();
                Vec3d hitPos = Vec3d.ofCenter(nb).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                int prev = p.getInventory().getSelectedSlot();
                p.getInventory().setSelectedSlot(slot);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, nb, false));
                p.swingHand(Hand.MAIN_HAND);
                if (silent.get()) p.getInventory().setSelectedSlot(prev);
                wait = delay.getInt();
                return;
            }
        }
    }

    // -------------------------------------------------------- InventoryMove
    public static class InventoryMove extends ClientModule {
        public InventoryMove() {
            super("InventoryMove", "Im Inventar weiterlaufen (WASD, Leertaste, Shift)", Category.WORLD);
        }

        private static void sync(KeyBinding kb, long window, int glfwKey) {
            kb.setPressed(GLFW.glfwGetKey(window, glfwKey) == GLFW.GLFW_PRESS);
        }

        @Override
        public void onStartTick() {
            if (!(mc.currentScreen instanceof HandledScreen<?>)) return;
            long w = mc.getWindow().getHandle();
            sync(mc.options.forwardKey, w, GLFW.GLFW_KEY_W);
            sync(mc.options.backKey, w, GLFW.GLFW_KEY_S);
            sync(mc.options.leftKey, w, GLFW.GLFW_KEY_A);
            sync(mc.options.rightKey, w, GLFW.GLFW_KEY_D);
            sync(mc.options.jumpKey, w, GLFW.GLFW_KEY_SPACE);
            sync(mc.options.sneakKey, w, GLFW.GLFW_KEY_LEFT_SHIFT);
        }
    }

    // ------------------------------------------------------------ FastPlace
    public static class FastPlace extends ClientModule {
        private final NumberSetting delay = add(new NumberSetting("Delay (Ticks)", 0, 0, 4, 1));
        private final BoolSetting blocksOnly = add(new BoolSetting("Blocks Only", true));

        public FastPlace() {
            super("FastPlace", "Bloecke schneller platzieren", Category.WORLD);
        }

        @Override
        public void onStartTick() {
            if (blocksOnly.get() && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
            MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
            if (acc.getItemUseCooldown() > delay.getInt()) acc.setItemUseCooldown(delay.getInt());
        }
    }

    // ------------------------------------------------------------- AutoFish
    public static class AutoFish extends ClientModule {
        private final NumberSetting threshold = add(new NumberSetting("Bite Threshold", -0.08, -0.5, -0.01, 0.01));
        private final NumberSetting recast = add(new NumberSetting("Recast Delay (Ticks)", 20, 5, 80, 1));
        private final BoolSetting autoCast = add(new BoolSetting("Auto Cast", true));
        private int cooldown;
        private int sinceCast;

        public AutoFish() {
            super("AutoFish", "Automatisch angeln (Angel in der Hand halten)", Category.WORLD);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (!p.getMainHandStack().isOf(Items.FISHING_ROD)) return;
            FishingBobberEntity hook = p.fishHook;
            if (hook == null) {
                sinceCast = 0;
                if (cooldown > 0) {
                    cooldown--;
                } else if (autoCast.get()) {
                    mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                    cooldown = 20;
                }
                return;
            }
            sinceCast++;
            if (sinceCast > 25 && hook.isTouchingWater() && hook.getVelocity().y < threshold.get()) {
                mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                cooldown = recast.getInt();
                sinceCast = 0;
            }
        }
    }

    // ---------------------------------------------------------------- Nuker
    public static class Nuker extends ClientModule {
        private final NumberSetting range = add(new NumberSetting("Range", 4.5, 1.0, 6.0, 0.5));
        private final NumberSetting perTick = add(new NumberSetting("Blocks / Tick (Creative)", 3, 1, 10, 1));
        private final BoolSetting oresOnly = add(new BoolSetting("Ores Only", false));
        private BlockPos current;

        public Nuker() {
            super("Nuker", "Bloecke automatisch abbauen", Category.WORLD);
        }

        private boolean breakable(BlockPos pos, Vec3d eye, double r) {
            if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > r * r) return false;
            BlockState s = mc.world.getBlockState(pos);
            if (s.isAir() || !s.getFluidState().isEmpty()) return false;
            if (s.getHardness(mc.world, pos) < 0) return false;
            return !oresOnly.get() || XRay.isOre(s);
        }

        @Override
        protected void onDisable() {
            current = null;
        }

        @Override
        public void onTick() {
            if (mc.currentScreen != null || mc.interactionManager == null) return;
            ClientPlayerEntity p = mc.player;
            double r = range.get();
            Vec3d eye = p.getEyePos();
            boolean creative = mc.interactionManager.getCurrentGameMode().isCreative();

            if (!creative && current != null && breakable(current, eye, r)) {
                hit(current, eye);
                return;
            }
            current = null;

            int ir = (int) Math.ceil(r);
            BlockPos base = p.getBlockPos();
            List<BlockPos> list = new ArrayList<>();
            for (int x = -ir; x <= ir; x++) {
                for (int y = -ir; y <= ir; y++) {
                    for (int z = -ir; z <= ir; z++) {
                        BlockPos pos = base.add(x, y, z);
                        if (breakable(pos, eye, r)) list.add(pos);
                    }
                }
            }
            if (list.isEmpty()) return;
            list.sort(Comparator.comparingDouble(pos -> eye.squaredDistanceTo(Vec3d.ofCenter(pos))));
            int n = creative ? Math.min(perTick.getInt(), list.size()) : 1;
            for (int i = 0; i < n; i++) hit(list.get(i), eye);
            if (!creative) current = list.get(0);
        }

        private void hit(BlockPos pos, Vec3d eye) {
            Vec3d c = Vec3d.ofCenter(pos);
            Direction face = Direction.getFacing(eye.x - c.x, eye.y - c.y, eye.z - c.z);
            mc.interactionManager.updateBlockBreakingProgress(pos, face);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
