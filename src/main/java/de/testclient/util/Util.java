package de.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Util {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Util() {
    }

    public static boolean moving() {
        GameOptions o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    /** Horizontaler Bewegungsvektor (Bloecke/Tick) aus den WASD-Tasten und der Blickrichtung. */
    public static Vec3d moveVec(double speed) {
        GameOptions o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        return moveVec(f, s, speed);
    }

    public static Vec3d moveVec(double forward, double strafeLeft, double speed) {
        if (forward == 0 && strafeLeft == 0) return Vec3d.ZERO;
        double yaw = Math.toRadians(mc.player.getYaw());
        double x = -Math.sin(yaw) * forward + Math.cos(yaw) * strafeLeft;
        double z = Math.cos(yaw) * forward + Math.sin(yaw) * strafeLeft;
        double len = Math.sqrt(x * x + z * z);
        return new Vec3d(x / len * speed, 0, z / len * speed);
    }

    /** {yaw, pitch} zum Zielpunkt. */
    public static float[] rotationsTo(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, h)));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    public static void attack(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
