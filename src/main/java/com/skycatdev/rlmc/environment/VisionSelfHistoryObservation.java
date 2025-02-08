/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public record VisionSelfHistoryObservation(List<BlockHitResult> blocks, List<@Nullable EntityHitResult> entities,
                                           ServerPlayerEntity self, List<FutureActionPack> history) {
    public static VisionSelfHistoryObservation fromPlayer(ServerPlayerEntity player, int raycasts, double maxDistance, double fov, List<FutureActionPack> history) {
        List<BlockHitResult> blocks = new ArrayList<>();
        List<@Nullable EntityHitResult> entities = new ArrayList<>();
        double sqrtRaycasts = Math.sqrt(raycasts);
        double deltaAngle = fov / raycasts;
        for (double i = -sqrtRaycasts / 2; i < sqrtRaycasts / 2; i++) {
            for (double j = -sqrtRaycasts / 2; j < sqrtRaycasts / 2; j++) {
                Vec3d pos = player.getCameraPosVec(1);
                Vec3d rot = player.getRotationVec(1).add(i * deltaAngle, j * deltaAngle, 0);
                Vec3d max = pos.add(rot.x * maxDistance, rot.y * maxDistance, rot.x * maxDistance);
                // Blocks (see Entity#raycast)
                BlockHitResult blockHitResult = player.getServerWorld().raycast(new RaycastContext(pos, max, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, player));
                blocks.add(blockHitResult);

                // Entities (see GameRenderer#findCrosshairTarget)
                Box box = player.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1, 1, 1);

                @Nullable EntityHitResult entityHitResult = ProjectileUtil.raycast(player, pos, max, box, entity -> !entity.isInvisibleTo(player), Math.pow(maxDistance, 2));
                entities.add(entityHitResult);
            }
        }
        return new VisionSelfHistoryObservation(blocks, entities, player, history);
    }
}
