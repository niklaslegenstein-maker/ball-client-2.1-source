package de.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class TargetUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private TargetUtil() {
    }

    public static boolean matches(Entity e, boolean players, boolean mobs, boolean animals, boolean invisible) {
        if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || e instanceof ArmorStandEntity) {
            return false;
        }
        if (e instanceof PlayerEntity pe && pe.isSpectator()) return false;
        if (!invisible && le.isInvisible()) return false;
        return (players && e instanceof PlayerEntity)
                || (mobs && e instanceof Monster)
                || (animals && e instanceof PassiveEntity);
    }

    public static double distance(Vec3d eye, Box b) {
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    /**
     * @param sort "Distance", "Health" oder "Angle"
     * @param fov  Sichtfeld in Grad (360 = ringsum)
     */
    public static LivingEntity find(double range, boolean players, boolean mobs, boolean animals,
                                    boolean invisible, boolean walls, double fov, String sort) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return null;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        Vec3d eye = p.getEyePos();
        for (Entity e : mc.world.getEntities()) {
            if (!matches(e, players, mobs, animals, invisible)) continue;
            LivingEntity le = (LivingEntity) e;
            double dist = distance(eye, le.getBoundingBox());
            if (dist > range) continue;
            if (!walls && !p.canSee(le)) continue;
            double ang = Math.abs(MathHelper.wrapDegrees(Util.rotationsTo(le.getBoundingBox().getCenter())[0] - p.getYaw()));
            if (fov < 360 && ang > fov / 2.0) continue;
            double score = switch (sort) {
                case "Health" -> le.getHealth();
                case "Angle" -> ang;
                default -> dist;
            };
            if (score < bestScore) {
                best = le;
                bestScore = score;
            }
        }
        return best;
    }
}
