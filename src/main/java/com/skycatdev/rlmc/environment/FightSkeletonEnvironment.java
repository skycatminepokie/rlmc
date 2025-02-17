/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.fakes.ServerPlayerInterface;
import java.util.*;
import java.util.concurrent.FutureTask;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FightSkeletonEnvironment extends Environment<FutureActionPack, BasicPlayerObservation> {
    protected final ServerWorld serverWorld;
    protected ServerPlayerEntity agent;
    protected BlockPos agentStartPos;
    protected BlockPos skeletonStartPos;
    protected ServerWorld world;
    protected FutureActionPack.History history = new FutureActionPack.History();
    protected int historyLength;
    protected @Nullable SkeletonEntity skeleton;
    protected boolean justKilled;

    public FightSkeletonEnvironment(ServerPlayerEntity agent, ServerWorld serverWorld, BlockPos agentStartPos, BlockPos skeletonStartPos, int historyLength) {
        this.agent = agent;
        ((AgentCandidate)agent).rlmc$markAsAgent();
        ((AgentCandidate)agent).rlmc$setKilledTrigger(this::onAgentKilled);
        this.agentStartPos = agentStartPos;
        this.skeletonStartPos = skeletonStartPos;
        this.serverWorld = serverWorld;
        this.historyLength = historyLength;
        world = serverWorld;
        justKilled = false;
    }

    @Override
    protected ResetTuple<BasicPlayerObservation> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        // TODO: Kill off arrows and dropped items
        PlayerInventory inventory = agent.getInventory();
        inventory.clear();
        inventory.offer(new ItemStack(Items.DIAMOND_SWORD), true);
        inventory.offer(new ItemStack(Items.DIAMOND_AXE), true);
        inventory.setStack(PlayerInventory.OFF_HAND_SLOT, new ItemStack(Items.SHIELD));
        agent.teleport(world, agentStartPos.getX(), agentStartPos.getY(), agentStartPos.getZ(), 0, 0);
        agent.setHealth(20);
        agent.getHungerManager().setFoodLevel(20);
        if (skeleton != null) {
            skeleton.discard();
        }
        skeleton = EntityType.SKELETON.spawn(world, skeletonStartPos, SpawnReason.COMMAND);
        if (skeleton == null) {
            throw new NullPointerException("Skeleton was null, expected non-null");
        }
        history = new FutureActionPack.History();
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT), 0);
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN), 0);


        return new ResetTuple<>(BasicPlayerObservation.fromPlayer(agent, 3, 3, 10, Math.PI / 2, history), new HashMap<>());
    }

    protected void onAgentKilled(AgentCandidate agent) {
        //noinspection EqualsBetweenInconvertibleTypes mixins go brrr
        assert (agent == this.agent);
        justKilled = true;
    }

    @Override
    protected Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<BasicPlayerObservation>>> innerStep(FutureActionPack action) {
        FutureTask<Boolean> preTick = new FutureTask<>(() -> {
            action.copyTo(((ServerPlayerInterface)agent).getActionPack());
            history.step(action);
            return true;
        });
        FutureTask<StepTuple<BasicPlayerObservation>> postTick = new FutureTask<>(() -> {
            BasicPlayerObservation observation = BasicPlayerObservation.fromPlayer(agent,  3, 3, 10, Math.PI / 2, history);

            int damageDealt = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
            int damageTaken = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
            int reward = damageDealt - damageTaken;

            boolean terminated = Objects.requireNonNull(skeleton).isDead();
            if (justKilled) {
                terminated = true;
                justKilled = false;
            }
            boolean truncated = agent.getServerWorld() != world;


            return new StepTuple<>(observation, reward, terminated, truncated, new HashMap<>());
        });
        return new Pair<>(preTick, postTick);
    }

    @Override
    public void close() {
        super.close();
        ((AgentCandidate)agent).rlmc$unmarkAsAgent();
        agent.kill();
    }
}
