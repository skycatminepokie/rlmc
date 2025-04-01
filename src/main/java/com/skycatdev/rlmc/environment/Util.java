/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class Util {
    public static Vec3d rotationVector(Vec3d origin, Vec3d destination) {
        Vec3d posVec = destination.subtract(origin);
        return posVecToRotVec(posVec);
    }

    public static Vec3d posVecToRotVec(Vec3d posVec) {
        // Calculations: https://stackoverflow.com/questions/58469297/how-do-i-calculate-the-yaw-pitch-and-roll-of-a-point-in-3d/58469298#58469298
        double yawRad = Math.atan2(posVec.getX(), posVec.getZ());
        double pitchRad = Math.atan2(posVec.getY(), Math.sqrt(Math.pow(posVec.getX(), 2) + Math.pow(posVec.getZ(), 2)));
        double yaw = MathHelper.wrapDegrees(Math.toDegrees(yawRad));
        double pitch = MathHelper.wrapDegrees(Math.toDegrees(pitchRad));
        return new Vec3d(yaw, pitch, posVec.length());
    }

    /**
     * Create a pos vector from a rot vec (yaw, pitch, distance) (in degrees)
     * @param rotVec The rotation vector
     * @return A new position vector
     */
    public static Vec3d rotVecToPosVec(Vec3d rotVec) {
        double yaw = Math.toRadians(rotVec.x);
        double pitch = Math.toRadians(rotVec.y);
        Vec3d posVec = new Vec3d(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        posVec = posVec.normalize().multiply(rotVec.z);
        return posVec;
    }

    public static Vector3f rotVecToPosVec(Vector3f rotVec) {
        double yaw = Math.toRadians(rotVec.x);
        double pitch = Math.toRadians(rotVec.y);
        Vector3f posVec = new Vector3f((float) (Math.sin(yaw) * Math.cos(pitch)), (float) Math.sin(pitch), (float) (Math.cos(yaw) * Math.cos(pitch)));
        posVec = posVec.normalize().mul(rotVec.z);
        return posVec;
    }

}
