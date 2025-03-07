/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class BasicPlayerEnvironment<O extends BasicPlayerObservation> extends Environment<FutureActionPack, O> {
    protected final int xRaycasts;
    protected final int yRaycasts;
    private final int getRaycastDistance = 10; // TODO: Make this a ctor param
    protected ServerPlayerEntity agent;
    protected Supplier<Float> initialHealth;
    protected Supplier<Integer> initialFoodLevel;
    protected FutureActionPack.History history;
    protected boolean justKilled;

    public BasicPlayerEnvironment(ServerPlayerEntity agent, Supplier<Float> initialHealth, Supplier<Integer> initialFoodLevel, int xRaycasts, int yRaycasts) {
        this.agent = agent;
        this.initialHealth = initialHealth;
        this.initialFoodLevel = initialFoodLevel;
        this.xRaycasts = xRaycasts;
        this.yRaycasts = yRaycasts;
        this.history = new FutureActionPack.History();
        this.justKilled = false;
        ((PlayerAgentCandidate) agent).rlmc$markAsAgent();
        ((PlayerAgentCandidate) agent).rlmc$setKilledTrigger(this::onAgentKilled);
    }

    public BasicPlayerEnvironment(ServerPlayerEntity agent, float initialHealth, int initialFoodLevel, int xRaycasts, int yRaycasts) {
        this(agent, () -> initialHealth, () -> initialFoodLevel, xRaycasts, yRaycasts);
    }

    public static @Nullable CompletableFuture<ServerPlayerEntity> createPlayerAgent(String name, MinecraftServer server, Vec3d pos, RegistryKey<World> world) {
        return createPlayerAgent(name, server, pos, 0.0, 0.0, world, GameMode.SURVIVAL, false);
    }

    private static @Nullable CompletableFuture<ServerPlayerEntity> createPlayerAgent(String name, MinecraftServer server, Vec3d pos, double yaw, double pitch, RegistryKey<World> world, GameMode gamemode, boolean flying) {
        if (EntityPlayerMPFake.createFake(name, server, pos, yaw, pitch, world, gamemode, flying)) {
            CompletableFuture<ServerPlayerEntity> future = new CompletableFuture<>();
            new Thread(() -> {
                @Nullable ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
                while (player == null) {
                    Thread.yield();
                    player = server.getPlayerManager().getPlayer(name);
                }
                ((PlayerAgentCandidate) player).rlmc$markAsAgent();
                future.complete(player);
            }, "RLMC Create Player Agent Thread").start();
            return future;
        } else {
            return null;
        }
    }

    protected boolean checkAndUpdateJustKilled() {
        boolean ret = false;
        if (justKilled) {
            ret = true;
            justKilled = false;
        }
        return ret;
    }

    @Override
    public void close() {
        super.close();
        ((PlayerAgentCandidate) agent).rlmc$unmarkAsAgent();
        agent.kill();
    }

    protected abstract HashMap<String, Object> getInfo(BasicPlayerObservation observation);

    public int getRaycastDistance() {
        return getRaycastDistance;
    }

    protected abstract O getObservation();

    @SuppressWarnings("unused") // Used by wrapped_basic_player_environment.py
    public int getRaycasts() {
        return xRaycasts * yRaycasts;
    }

    protected abstract int getReward(BasicPlayerObservation observation);

    protected BlockPos getStartBlockPos() {
        return BlockPos.ofFloored(getStartPos());
    }

    protected abstract Vec3d getStartPos();

    @SuppressWarnings("unused") // Used by wrapped_basic_player_environment.py
    protected abstract ServerWorld getWorld();

    /**
     * Called when resetting at the beginning of the tick. History, food, and health will be reset and the agent will be teleported after this.
     */
    protected abstract void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options);

    @Override
    protected ResetTuple<O> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        innerPreReset(seed, options);
        history = new FutureActionPack.History();
        agent.teleport(getWorld(), getStartPos().getX(), getStartPos().getY(), getStartPos().getZ(), Set.of(), 0, 0);
        agent.setHealth(initialHealth.get());
        agent.getHungerManager().setFoodLevel(initialFoodLevel.get());
        agent.setAir(20);
        agent.extinguish();
        agent.fallDistance = 0;


        return new ResetTuple<>(getObservation(), new HashMap<>());
    }

    @Override
    protected Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>> innerStep(FutureActionPack action) {
        FutureTask<Boolean> preTick = new FutureTask<>(() -> {
            action.copyTo(((ServerPlayerInterface) agent).getActionPack());
            history.step(action);
            return true;
        });
        FutureTask<StepTuple<O>> postTick = new FutureTask<>(() -> {
            O observation = getObservation();
            return new StepTuple<>(observation, getReward(observation), isTerminated(observation), isTruncated(observation), getInfo(observation));
        });
        return new Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>>(preTick, postTick);
    }

    protected abstract boolean isTerminated(BasicPlayerObservation observation);

    protected abstract boolean isTruncated(BasicPlayerObservation observation);

    protected void onAgentKilled(PlayerAgentCandidate agent) {
        //noinspection EqualsBetweenInconvertibleTypes mixins go brrr
        assert (agent == this.agent);
        justKilled = true;
    }
}
