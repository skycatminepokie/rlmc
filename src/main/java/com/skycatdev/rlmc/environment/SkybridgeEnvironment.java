/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.fakes.ServerPlayerInterface;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
	protected SynchronousQueue<Runnable> preStepQueue = new SynchronousQueue<>();
	protected SynchronousQueue<FutureTask<StepTuple<Observation>>> postStepQueue = new SynchronousQueue<>();
	/**
	 * True when {@link SkybridgeEnvironment#reset(Integer, Map)} has been called at least once. Synchronize on {@link SkybridgeEnvironment#initializedLock} first.
	 */
	protected boolean initialized;
	protected final Object[] initializedLock = new Object[] {};
	/**
	 * A future representing a function call to {@link SkybridgeEnvironment#reset(Integer, Map)}. Always synchronize on {@link SkybridgeEnvironment#resetTaskLock} first.
	 */
	protected @Nullable FutureTask<ResetTuple<Observation>> resetTask;
	protected final Object[] resetTaskLock = new Object[]{};

	public SkybridgeEnvironment(ServerPlayerEntity agent, BlockPos startPos, int distance, int historyLength) {
		this.agent = agent;
		this.startPos = startPos;
		this.distance = distance;
		this.historyLength = historyLength;
		world = agent.getServerWorld();
	}

	public ServerPlayerEntity getAgent() {
		return agent;
	}

	@Override
	public ResetTuple<Observation> reset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
		FutureTask<ResetTuple<Observation>> postTick = new FutureTask<>(() -> {
			agent.getInventory().clear();
			for (BlockPos pos : BlockPos.iterateOutwards(startPos, 100, 10, 100)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
			world.setBlockState(startPos.down(), Blocks.STONE.getDefaultState());
			for (int i = 0; i < 36; i++) {
				agent.getInventory().insertStack(new ItemStack(Items.STONE, 64));
			}
			history.clear();

			Observation observation = Observation.fromPlayer(agent, 100, 10, 180, history);

			return new ResetTuple<>(observation, new HashMap<>());
		});
		synchronized (resetTaskLock) {
			resetTask = postTick;
		}
        try {
			return postTick.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public StepTuple<Observation> step(FutureActionPack action) {
		FutureTask<StepTuple<Observation>> postTick = new FutureTask<>(() -> {
			Observation observation = Observation.fromPlayer(agent, 100, 10, 180, history);

			int reward = startPos.getX() - agent.getBlockX();
			boolean terminated = startPos.getX() - agent.getBlockX() >= distance;
			boolean truncated = agent.getServerWorld() != world || agent.isDead() || agent.getBlockY() < startPos.getY() + 1;

			return new StepTuple<>(observation, reward, terminated, truncated, new HashMap<>());
		});
        try {
			preStepQueue.put(() -> {
				action.copyTo(((ServerPlayerInterface)agent).getActionPack());
				history.add(action);
				if (history.size() > historyLength) {
					history.removeFirst();
				}
			});
			postStepQueue.put(postTick);
            return postTick.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public void preTick() {
		boolean shouldRun;
		synchronized (initializedLock) {
			shouldRun = initialized;
		}
		if (shouldRun) {
			Objects.requireNonNull(preStepQueue.poll()).run();
		}
	}

	@Override
	public void postTick() {
		boolean shouldRun;
		synchronized (initializedLock) {
			shouldRun = initialized;
		}
		if (shouldRun) {
			Objects.requireNonNull(postStepQueue.poll()).run();
		}
		synchronized (resetTaskLock) {
			if (resetTask != null) {
				resetTask.run();
				synchronized (initializedLock) {
					initialized = true;
				}
			}
		}
	}

	public record Observation(List<BlockHitResult> blocks, List<@Nullable EntityHitResult> entities,
							  ServerPlayerEntity self, List<FutureActionPack> history) {
		public static Observation fromPlayer(ServerPlayerEntity player, int raycasts, double maxDistance, double fov, List<FutureActionPack> history) { // TODO: Test
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
