/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.fakes.ServerPlayerInterface;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class SkybridgeEnvironment extends Environment<FutureActionPack, VisionSelfHistoryObservation> {
	protected ServerPlayerEntity agent;
	protected BlockPos startPos;
	protected ServerWorld world;
	protected int distance;
	protected List<FutureActionPack> history = new ArrayList<>();
	protected int historyLength;
	protected final int xRaycasts;
	protected final int yRaycasts;

	public SkybridgeEnvironment(ServerPlayerEntity agent, BlockPos startPos, int distance, int historyLength, int xRaycasts, int yRaycasts) {
		this.agent = agent;
		this.startPos = startPos;
		this.distance = distance;
		this.historyLength = historyLength;
		world = agent.getServerWorld();
        this.xRaycasts = xRaycasts;
		this.yRaycasts = yRaycasts;
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
			VisionSelfHistoryObservation observation = VisionSelfHistoryObservation.fromPlayer(agent,  xRaycasts, yRaycasts, 10, Math.PI/2, history);

			int reward = 0;
			BlockPos.Mutable blockPos = new BlockPos.Mutable(startPos.getX(), startPos.getY(), startPos.getZ());
			while (!world.getBlockState(blockPos.move(Direction.NORTH)).isAir()) {
				reward++;
			}
			boolean terminated = startPos.getX() - agent.getBlockX() >= distance || agent.getBlockY() < startPos.getY() - 1;
			boolean truncated = agent.getServerWorld() != world || agent.isDead();

			return new StepTuple<>(observation, reward, terminated, truncated, new HashMap<>());
		});
		return new Pair<>(preTick, postTick);
	}

	@Override
	protected ResetTuple<VisionSelfHistoryObservation> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
		agent.getInventory().clear();
		for (BlockPos pos : BlockPos.iterateOutwards(startPos, distance + 6, 40, distance + 6)) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
		}
		world.setBlockState(startPos.down(), Blocks.STONE.getDefaultState());
		for (int i = 0; i < 36; i++) {
			agent.getInventory().offer(new ItemStack(Items.STONE, 64), true);
		}
		history.clear();
		agent.teleport(world, startPos.getX(), startPos.getY(), startPos.getZ(), Set.of(), 0, 0);

		VisionSelfHistoryObservation observation = VisionSelfHistoryObservation.fromPlayer(agent, xRaycasts, yRaycasts, 10, Math.PI/2, history);

		return new ResetTuple<>(observation, new HashMap<>());
	}

}
