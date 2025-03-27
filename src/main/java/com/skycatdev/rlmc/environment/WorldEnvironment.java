/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.command.EnvironmentSettings;
import java.util.Random;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public abstract class WorldEnvironment<A, O> extends Environment<A, O> {
    @Nullable protected RuntimeWorldHandle worldHandle;
    protected MinecraftServer server;

    public WorldEnvironment(EnvironmentSettings environmentSettings, MinecraftServer server) {
        super(environmentSettings);
        this.server = server;
    }

    protected abstract ChunkGenerator getChunkGenerator();

    @SuppressWarnings("unused") // Used by wrapped_basic_player_environment.py
    protected ServerWorld getWorld() {
        if (worldHandle == null) {
            Rlmc.LOGGER.trace("Creating world for basic player env \"{}\"", getUniqueEnvName());
            worldHandle = Fantasy.get(server).openTemporaryWorld(new RuntimeWorldConfig()
                    .setDimensionType(DimensionTypes.OVERWORLD)
                    .setDifficulty(Difficulty.HARD)
                    .setGameRule(GameRules.DO_DAYLIGHT_CYCLE, false)
                    .setGenerator(getChunkGenerator())
                    .setSeed(new Random().nextLong()));
            worldHandle.asWorld().setTimeOfDay(24000L);
            Rlmc.LOGGER.trace("Created world for basic player env \"{}\"", getUniqueEnvName());
        }
        return worldHandle.asWorld();
    }

    public abstract boolean isIn(ServerWorld world);

}
