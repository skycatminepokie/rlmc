/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.skycatdev.rlmc.mixin.SpreadPlayersCommandAccessor;
import java.util.Collection;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.SpreadPlayersCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;

public class SpreadEntitiesHelper {
    /**
     * Totally and completely yoinked from SpreadPlayersCommand.
     */
    public static int spreadEntities(ServerWorld world, Vec2f center, float spreadDistance, float maxRange, int maxY, boolean respectTeams, Collection<? extends Entity> entities) {
        if (maxY < world.getBottomY()) {
            throw new IllegalArgumentException("Invalid height - it's below the world");
        } else {
            Random random = Random.create();
            double minX = center.x - maxRange;
            double minZ = center.y - maxRange;
            double maxX = center.x + maxRange;
            double maxZ = center.y + maxRange;
            SpreadPlayersCommand.Pile[] piles = SpreadPlayersCommandAccessor.callMakePiles(random, respectTeams ? SpreadPlayersCommandAccessor.callGetPileCountRespectingTeams(entities) : entities.size(), minX, minZ, maxX, maxZ);
            boolean success = false;
            for (int i = 0; i < 10000; i++) {
                try {
                    SpreadPlayersCommandAccessor.callSpread(center, spreadDistance, world, random, minX, minZ, maxX, maxZ, maxY, piles, respectTeams);
                } catch (CommandSyntaxException ex) {
                    minX++;
                    minZ++;
                    maxX++;
                    maxZ++;
                    continue;
                }
                success = true;
                break;
            }
            double h = SpreadPlayersCommandAccessor.callGetMinDistance(entities, world, piles, maxY, respectTeams);
            return piles.length;
        }
    }
}
