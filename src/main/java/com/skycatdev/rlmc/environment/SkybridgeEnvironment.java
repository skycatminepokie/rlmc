/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.fakes.ServerPlayerInterface;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class SkybridgeEnvironment extends Environment<FutureActionPack, SkybridgeEnvironment.Observation> {
	protected ServerPlayerEntity agent;
	protected BlockPos startPos;
	protected ServerWorld world;
	protected int distance;
	protected List<FutureActionPack> history = new ArrayList<>();
	protected int historyLength;

	public SkybridgeEnvironment(ServerPlayerEntity agent, BlockPos startPos, int distance, int historyLength) {
		this.agent = agent;
		this.startPos = startPos;
		this.distance = distance;
		this.historyLength = historyLength;
		world = agent.getServerWorld();
	}

	@Override
	protected Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<Observation>>> innerStep(FutureActionPack action) {
		FutureTask<Boolean> preTick = new FutureTask<>(() -> {
			action.copyTo(((ServerPlayerInterface)agent).getActionPack());
			history.add(action);
			if (history.size() > historyLength) {
				history.removeFirst();
			}
			return true;
		});
		FutureTask<StepTuple<Observation>> postTick = new FutureTask<>(() -> {
			Observation observation = Observation.fromPlayer(agent, 100, 10, 180, history);

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
	protected ResetTuple<Observation> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
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

		Observation observation = Observation.fromPlayer(agent, 100, 10, 180, history);

		return new ResetTuple<>(observation, new HashMap<>());
	}

	public record Observation(List<BlockHitResult> blocks, List<@Nullable EntityHitResult> entities,
							  ServerPlayerEntity self, List<FutureActionPack> history) {
		public static Observation fromPlayer(ServerPlayerEntity player, int raycasts, double maxDistance, double fov, List<FutureActionPack> history) {
			List<BlockHitResult> blocks = new ArrayList<>();
			List<@Nullable EntityHitResult> entities = new ArrayList<>();
			double sqrtRaycasts = Math.sqrt(raycasts);
			double deltaAngle = fov / raycasts;
			for (double i = -sqrtRaycasts / 2; i < sqrtRaycasts / 2; i++) {
				for (double j = -sqrtRaycasts / 2; j < sqrtRaycasts / 2; j++) {
					Vec3d pos = player.getCameraPosVec(1);
					Vec3d rot = player.getRotationVec(1).add(i * deltaAngle, j * deltaAngle, 0);
					Vec3d max = pos.add(rot.x * maxDistance, rot.y * maxDistance, rot.x * maxDistance);
					// Blocks (see Entity#raycast)
					BlockHitResult blockHitResult = player.getServerWorld().raycast(new RaycastContext(pos, max, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, player));
					blocks.add(blockHitResult);

					// Entities (see GameRenderer#findCrosshairTarget)
					Box box = player.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1, 1, 1);

					@Nullable EntityHitResult entityHitResult = ProjectileUtil.raycast(player, pos, max, box, entity -> !entity.isInvisibleTo(player), Math.pow(maxDistance, 2));
					entities.add(entityHitResult);
				}
			}
			return new Observation(blocks, entities, player, history);
		}
	}
}
