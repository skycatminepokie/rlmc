/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class FightSkeletonEnvironment extends BasicPlayerEnvironment {
    protected BlockPos skeletonStartPos;
    protected @Nullable SkeletonEntity skeleton;
    protected boolean justKilled;

    public FightSkeletonEnvironment(ServerPlayerEntity agent, Vec3d agentStartPos, BlockPos skeletonStartPos) {
        super(agent.getServerWorld(), agent, agentStartPos, 20, 20, 3, 3);
        this.skeletonStartPos = skeletonStartPos;
        justKilled = false;
    }

    @Override
    protected HashMap<String, Object> getInfo(BasicPlayerObservation observation) {
        return new HashMap<>();
    }

    @Override
    protected int getReward(BasicPlayerObservation observation) {
        int damageDealt = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
        int damageTaken = agent.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
        return damageDealt - damageTaken;
    }

    @Override
    protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        PlayerInventory inventory = agent.getInventory();
        inventory.clear();
        inventory.offer(new ItemStack(Items.DIAMOND_SWORD), true);
        inventory.offer(new ItemStack(Items.DIAMOND_AXE), true);
        inventory.setStack(PlayerInventory.OFF_HAND_SLOT, new ItemStack(Items.SHIELD));
        if (skeleton != null) {
            skeleton.discard();
        }
        skeleton = EntityType.SKELETON.spawn(getWorld(), skeletonStartPos, SpawnReason.COMMAND);
        if (skeleton == null) {
            throw new NullPointerException("Skeleton was null, expected non-null");
        }
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT), 0);
        agent.getStatHandler().setStat(agent, Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN), 0);
    }

    @Override
    protected boolean isTerminated(BasicPlayerObservation observation) {
        if (skeleton == null) {
            throw new RuntimeException("Skeleton should not have been null");
        }
        return checkAndUpdateJustKilled() || skeleton.isDead();
    }

    @Override
    protected boolean isTruncated(BasicPlayerObservation observation) {
        return agent.getServerWorld() != getWorld();
    }

}
