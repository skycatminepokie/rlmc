/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.command.EnvironmentSettings;
import net.minecraft.server.MinecraftServer;

public abstract class WorldTickEnvironment<A, O> extends WorldEnvironment<A, O> {

    public WorldTickEnvironment(EnvironmentSettings environmentSettings, MinecraftServer server) {
        super(environmentSettings, server);
    }

}
