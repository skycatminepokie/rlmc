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

public class FightSkeletonEnvironment extends Environment<FutureActionPack, VisionSelfHistoryObservation> {
    protected final ServerWorld serverWorld;
    protected ServerPlayerEntity agent;
    protected BlockPos agentStartPos;
    protected BlockPos skeletonStartPos;
    protected ServerWorld world;
    protected List<FutureActionPack> history = new ArrayList<>();
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
    protected ResetTuple<VisionSelfHistoryObservation> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
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
        history.clear();
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT), 0);
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN), 0);


        return new ResetTuple<>(VisionSelfHistoryObservation.fromPlayer(agent, 3, 3, 10, Math.PI/2, history), new HashMap<>());
    }

    protected void onAgentKilled(AgentCandidate agent) {
        //noinspection EqualsBetweenInconvertibleTypes mixins go brrr
        assert (agent == this.agent);
        justKilled = true;
    }

    @Override
    protected Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<VisionSelfHistoryObservation>>> innerStep(FutureActionPack action) {
        FutureTask<Boolean> preTick = new FutureTask<>(() -> {
            action.copyTo(((ServerPlayerInterface)agent).getActionPack());
            history.add(action);
            if (history.size() > historyLength) {
                history.removeFirst();
            }
            return true;
        });
        FutureTask<StepTuple<VisionSelfHistoryObservation>> postTick = new FutureTask<>(() -> {
            VisionSelfHistoryObservation observation = VisionSelfHistoryObservation.fromPlayer(agent,  3, 3, 10, Math.PI/2, history);

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
