package de.testclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Rechnet Weltkoordinaten auf 2D-Bildschirmkoordinaten (GUI-Skalierung) um.
 * Gilt fuer die Ego-Perspektive. Das FOV wird naeherungsweise nachgebildet.
 */
public final class Projector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static Vec3d cam = Vec3d.ZERO, fwd = Vec3d.ZERO, right = Vec3d.ZERO, up = Vec3d.ZERO;
    private static double tanHalf = 1, aspect = 1, sw = 1, sh = 1;

    private Projector() {
    }

    public static boolean begin(float tp) {
        ClientPlayerEntity p = mc.player;
        if (p == null || !mc.options.getPerspective().isFirstPerson()) return false;

        cam = p.getLerpedPos(tp).add(0, p.getEyeY() - p.getY(), 0);
        double y = Math.toRadians(p.getYaw(tp));
        double pt = Math.toRadians(p.getPitch(tp));
        double cy = Math.cos(y), sy = Math.sin(y), cp = Math.cos(pt), sp = Math.sin(pt);
        fwd = new Vec3d(-sy * cp, -sp, cy * cp);
        right = new Vec3d(-cy, 0, -sy);
        up = new Vec3d(-sy * sp, cp, cy * sp);

        double fov = mc.options.getFov().getValue();
        double mult = (p.getAttributeValue(EntityAttributes.MOVEMENT_SPEED) / 0.1 + 1.0) / 2.0;
        fov *= MathHelper.clamp(mult, 0.5, 2.0);
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);

        sw = mc.getWindow().getScaledWidth();
        sh = mc.getWindow().getScaledHeight();
        aspect = sw / sh;
        return true;
    }

    /** @return {x, y, tiefe} oder null, wenn der Punkt hinter der Kamera liegt. */
    public static double[] project(Vec3d w) {
        Vec3d d = w.subtract(cam);
        double depth = d.dotProduct(fwd);
        if (depth < 0.05) return null;
        double nx = (d.dotProduct(right) / depth) / (tanHalf * aspect);
        double ny = (d.dotProduct(up) / depth) / tanHalf;
        return new double[]{sw / 2.0 * (1 + nx), sh / 2.0 * (1 - ny), depth};
    }

    /** @return {minX, minY, maxX, maxY} oder null, wenn eine Ecke hinter der Kamera liegt. */
    public static float[] rect(Box b) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            Vec3d c = new Vec3d(
                    (i & 1) == 0 ? b.minX : b.maxX,
                    (i & 2) == 0 ? b.minY : b.maxY,
                    (i & 4) == 0 ? b.minZ : b.maxZ);
            double[] s = project(c);
            if (s == null) return null;
            minX = Math.min(minX, s[0]);
            minY = Math.min(minY, s[1]);
            maxX = Math.max(maxX, s[0]);
            maxY = Math.max(maxY, s[1]);
        }
        return new float[]{(float) minX, (float) minY, (float) maxX, (float) maxY};
    }

    /** Hitbox der Entity an der interpolierten (weichen) Position. */
    public static Box entityBox(Entity e, float tp) {
        Vec3d l = e.getLerpedPos(tp);
        return e.getBoundingBox().offset(l.x - e.getX(), l.y - e.getY(), l.z - e.getZ());
    }
}
