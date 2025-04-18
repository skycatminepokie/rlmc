/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.NameGenerator;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.command.EnvironmentSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

public class GoNorthEnvironment extends BasicPlayerEnvironment<BasicPlayerObservation> {
    private double prevX;

    public static @Nullable Future<GoNorthEnvironment> makeAndConnect(EnvironmentSettings settings, String agentName, MinecraftServer server) {
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(agentName, server, Vec3d.ZERO, server.getOverworld().getRegistryKey());
        if (agentFuture != null) {
            Function<ServerPlayerEntity, GoNorthEnvironment> environmentFuture = agent -> {
                GoNorthEnvironment environment = new GoNorthEnvironment(settings, agent);
                Rlmc.addEnvironment(environment);
                Rlmc.getPythonEntrypoint().connectEnvironment("go_north", environment);
                return environment;
            };
            return agentFuture.thenApplyAsync(environmentFuture, (runnable) -> new Thread(runnable).start());
        }
        return null;
    }

    public GoNorthEnvironment(EnvironmentSettings settings, ServerPlayerEntity agent) {
        super(settings, agent, 20, 20, 3, 3);
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
    protected boolean isTerminated(BasicPlayerObservation observation) {
        return agent.getX() > 20 || agent.getX() < -20;
    }

    @Override
    protected boolean isTruncated(BasicPlayerObservation observation) {
        return false;
    }

    @Override
    protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        Random random = Random.create();
        Vec3d agentPos = getWorld().getTopPosition(Heightmap.Type.WORLD_SURFACE, BlockPos.ORIGIN).toCenterPos();
        agent.teleport(getWorld(), agentPos.getX(), agentPos.getY(), agentPos.getZ(), Set.of(), (random.nextFloat() % 180) - 180,  (random.nextFloat() % 90) - 90);
    }

    @Override
    public Future<Future<? extends Environment<FutureActionPack, BasicPlayerObservation>>> makeAnother() {
        FutureTask<Future<? extends Environment<FutureActionPack, BasicPlayerObservation>>> futureTask = new FutureTask<>(() -> Objects.requireNonNull(makeAndConnect(settings, NameGenerator.newName(getWorld().getServer().getPlayerManager().getPlayerList()), getWorld().getServer())));
        Rlmc.runBeforeNextTick(futureTask);
        return futureTask;
    }
}
