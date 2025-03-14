/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.Rlmc;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

public class GoNorthEnvironment extends BasicPlayerEnvironment<BasicPlayerObservation> {
    private double prevX;

    public static @Nullable Future<GoNorthEnvironment> makeAndConnect(String agentName, MinecraftServer server) {
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(agentName, server, Vec3d.ZERO, server.getOverworld().getRegistryKey());
        if (agentFuture != null) {
            Function<ServerPlayerEntity, GoNorthEnvironment> environmentFuture = agent -> {
                GoNorthEnvironment environment = new GoNorthEnvironment(agent);
                Rlmc.addEnvironment(environment);
                Rlmc.getPythonEntrypoint().connectEnvironment("go_north", environment);
                return environment;
            };
            return agentFuture.thenApplyAsync(environmentFuture, (runnable) -> new Thread(runnable).start());
        }
        return null;
    }

    public GoNorthEnvironment(ServerPlayerEntity agent) {
        super(agent, 20, 20, 3, 3);
    }

    @Override
    protected HashMap<String, Object> getInfo(BasicPlayerObservation observation) {
        return new HashMap<>();
    }

    @Override
    protected BasicPlayerObservation getObservation() {
        return BasicPlayerObservation.fromPlayer(agent, xRaycasts, yRaycasts, 10, Math.PI/2, history);
    }

    @Override
    protected double getReward(BasicPlayerObservation observation) {
        double prevX = this.prevX;
        this.prevX = agent.getX();
        return (agent.getX() - prevX)/25d;
    }

    @Override
    protected Vec3d getStartPos() {
        return getWorld().getTopPosition(Heightmap.Type.WORLD_SURFACE, BlockPos.ORIGIN).toCenterPos();
    }


    @Override
    protected boolean isTerminated(BasicPlayerObservation observation) {
        return agent.getX() > 20 || agent.getX() < -20;
    }

    @Override
    protected boolean isTruncated(BasicPlayerObservation observation) {
        return false;
    }

    @Override
    protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {

    }
}
