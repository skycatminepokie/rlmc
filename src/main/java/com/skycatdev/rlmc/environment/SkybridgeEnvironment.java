/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class SkybridgeEnvironment extends BasicPlayerEnvironment {
	protected int distance;
	protected int historyLength;

	public SkybridgeEnvironment(ServerPlayerEntity agent, BlockPos startPos, int distance, int historyLength, int xRaycasts, int yRaycasts) {
		super(agent.getServerWorld(), agent, Vec3d.of(startPos),20, 20, xRaycasts, yRaycasts);
		this.distance = distance;
		this.historyLength = historyLength;
    }

	@Override
	protected HashMap<String, Object> getInfo(BasicPlayerObservation observation) {
		return new HashMap<>();
	}

	@Override
	protected boolean isTerminated(BasicPlayerObservation observation) {
		return agent.getBlockY() < getStartPos().getY() - 1 || getStartPos().getX() - agent.getBlockX() >= distance;
	}

	@Override
	protected boolean isTruncated(BasicPlayerObservation observation) {
		return checkAndUpdateJustKilled() || !agent.getWorld().equals(getWorld());
	}

	@Override
	protected int getReward(BasicPlayerObservation observation) {
		int reward = 0;
		BlockPos.Mutable blockPos = new BlockPos.Mutable(getStartPos().getX(), getStartPos().getY(), getStartPos().getZ());
		while (!getWorld().getBlockState(blockPos.move(Direction.NORTH)).isAir()) {
			reward++;
		}
		return reward;
	}

	@Override
	protected void innerPreReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
		agent.getInventory().clear();
		for (BlockPos pos : BlockPos.iterateOutwards(getStartBlockPos(), distance + 6, 40, distance + 6)) {
			getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
		}
		getWorld().setBlockState(getStartBlockPos().down(), Blocks.STONE.getDefaultState());
		for (int i = 0; i < 36; i++) {
			agent.getInventory().offer(new ItemStack(Items.STONE, 64), true);
		}
	}
}
