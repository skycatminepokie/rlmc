/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

public class SpreadEntitiesHelper {
    // TODO test
    public static @Nullable Pair<BlockPos, BlockPos> getSpreadLocations(ServerWorld world,
                                                                        Vec3i center,
                                                                        Vec3i firstMaxFromCenter,
                                                                        Vec3i minSpread,
                                                                        Vec3i maxSpread,
                                                                        Random random) {
        Iterable<BlockPos> centerIterable = BlockPos.iterateOutwards(net.minecraft.util.math.BlockPos.ofFloored(center.getX(),0, center.getY()), firstMaxFromCenter.getX(), 0, firstMaxFromCenter.getZ());
        for (BlockPos blockPos : centerIterable) {
            BlockPos blockCenter = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, blockPos);
            BlockPos min = new BlockPos(blockCenter.getX() - minSpread.getX(), blockCenter.getY() - minSpread.getY(), blockCenter.getZ() - minSpread.getZ());
            BlockPos max = new BlockPos(blockCenter.getX() + maxSpread.getX(), blockCenter.getY() + maxSpread.getY(), blockCenter.getZ() + maxSpread.getZ());
            Iterable<BlockPos> secondIterable = BlockPos.iterateRandomly(random, 1000, min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
            for (BlockPos secondSpawn : secondIterable) {
                if (secondSpawn.getX() >= min.getX() && secondSpawn.getY() >= min.getY() && secondSpawn.getZ() >= min.getZ()) {
                    return new Pair<>(blockCenter, secondSpawn);
                }
            }
        }
        return null;
    }

    public static boolean spreadEntities(ServerWorld world,
                                         Vec3i center,
                                         Vec3i firstMaxFromCenter,
                                         Vec3i minSpread,
                                         Vec3i maxSpread,
                                         Entity entity1,
                                         Entity entity2,
                                         Random random) {
        @Nullable Pair<BlockPos, BlockPos> positions = getSpreadLocations(world, center, firstMaxFromCenter, minSpread, maxSpread, random);
        if (positions == null) {
            return false;
        }
        Vec3d firstPos = positions.getLeft().toCenterPos();
        Vec3d secondPos = positions.getRight().toCenterPos();
        entity1.teleport(world, firstPos.getX(), firstPos.getY(), firstPos.getZ(), Set.of(), (random.nextFloat() % 180) - 180,  (random.nextFloat() % 90) - 90);
        entity2.teleport(world, secondPos.getX(), secondPos.getY(), secondPos.getZ(), Set.of(), (random.nextFloat() % 180) - 180,  (random.nextFloat() % 90) - 90);
        return true;
    }
}
