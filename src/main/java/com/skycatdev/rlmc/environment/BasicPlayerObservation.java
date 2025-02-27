/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class BasicPlayerObservation {
    private final List<BlockHitInfo> blocks;
    private final List<@Nullable EntityHitResult> entities;
    private final ServerPlayerEntity self;
    private final FutureActionPack.History history;

    public BasicPlayerObservation(List<BlockHitInfo> blocks, List<@Nullable EntityHitResult> entities,
                                  ServerPlayerEntity self, FutureActionPack.History history) {
        this.blocks = blocks;
        this.entities = entities;
        this.self = self;
        this.history = history;
    }

    public static BasicPlayerObservation fromPlayer(ServerPlayerEntity player, int xRaycasts, int yRaycasts, double maxDistance, double fovRad, FutureActionPack.History history) {
        List<BlockHitInfo> blocks = new ArrayList<>();
        List<@Nullable EntityHitResult> entities = new ArrayList<>();
        double deltaAngleX = fovRad / xRaycasts;
        double deltaAngleY = fovRad / yRaycasts;
        for (double i = 0; i < xRaycasts; i++) {
            for (double j = 0; j < yRaycasts; j++) {
                Vec3d pos = player.getCameraPosVec(0);
                Vec3d rot = player.getRotationVec(0).rotateX((float) (i * deltaAngleX - (fovRad / 2)));
                rot = rot.rotateY((float) (j * deltaAngleY - (fovRad / 2)));
                Vec3d max = pos.add(rot.x * maxDistance, rot.y * maxDistance, rot.z * maxDistance);
                // Blocks (see Entity#raycast)
                BlockHitResult blockHitResult = player.getServerWorld().raycast(new RaycastContext(pos, max, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, player));
                blocks.add(new BlockHitInfo(blockHitResult.getSide(), blockHitResult.getBlockPos(), player.getServerWorld().getBlockState(blockHitResult.getBlockPos())));

                // Entities (see GameRenderer#findCrosshairTarget)
                Box box = player.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1, 1, 1);

                @Nullable EntityHitResult entityHitResult = ProjectileUtil.raycast(player, pos, max, box, entity -> !entity.isInvisibleTo(player), Math.pow(maxDistance, 2));
                entities.add(entityHitResult);
            }
        }
        return new BasicPlayerObservation(blocks, entities, player, history);
    }

    public List<BlockHitInfo> blocks() {
        return blocks;
    }

    public List<@Nullable EntityHitResult> entities() {
        return entities;
    }

    public ServerPlayerEntity self() {
        return self;
    }

    public FutureActionPack.History history() {
        return history;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BasicPlayerObservation) obj;
        return Objects.equals(this.blocks, that.blocks) &&
               Objects.equals(this.entities, that.entities) &&
               Objects.equals(this.self, that.self) &&
               Objects.equals(this.history, that.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocks, entities, self, history);
    }

    @Override
    public String toString() {
        return "BasicPlayerObservation[" +
               "blocks=" + blocks + ", " +
               "entities=" + entities + ", " +
               "self=" + self + ", " +
               "history=" + history + ']';
    }

}
