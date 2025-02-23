/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.SpreadPlayersCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("RedundantThrows")
@Mixin(SpreadPlayersCommand.class)
public interface SpreadPlayersCommandAccessor {
    @Invoker
    static int callGetPileCountRespectingTeams(Collection<? extends Entity> players) {
        throw new IllegalArgumentException("Implemented in a Mixin.");
    }

    @Invoker
    static void callSpread(Vec2f center,
                           double spreadDistance,
                           ServerWorld world,
                           Random random,
                           double minX,
                           double minZ,
                           double maxX,
                           double maxZ,
                           int maxY,
                           SpreadPlayersCommand.Pile[] piles,
                           boolean respectTeams
    ) throws CommandSyntaxException {
        throw new AssertionError("Implemented in a mixin.");
    }

    @Invoker static SpreadPlayersCommand.Pile[] callMakePiles(Random random, int count, double minX, double minZ, double maxX, double maxZ) {
        throw new AssertionError("Implemented in a mixin.");
    }

    @Invoker static double callGetMinDistance(Collection<? extends Entity> entities, ServerWorld world, SpreadPlayersCommand.Pile[] piles, int maxY, boolean respectTeams) {
        throw new AssertionError("Implemented in a mixin.");
    }
}
