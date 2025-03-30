/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Util {
    public static Vec3d rotationVector(Vec3d origin, Vec3d destination) {
        Vec3d posVec = destination.subtract(origin);
        // Vector calculations: https://stackoverflow.com/questions/58469297/how-do-i-calculate-the-yaw-pitch-and-roll-of-a-point-in-3d/58469298#58469298
        double yawRad = Math.atan2(posVec.getX(), posVec.getZ());
        double pitchRad = Math.atan2(posVec.getY(), Math.sqrt(Math.pow(posVec.getX(), 2) + Math.pow(posVec.getZ(), 2)));
        double yaw = MathHelper.wrapDegrees(Math.toDegrees(yawRad));
        double pitch = MathHelper.wrapDegrees(Math.toDegrees(pitchRad));
        return new Vec3d(yaw, pitch, posVec.length());
    }
}
