/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import carpet.helpers.EntityPlayerActionPack;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java implementation of a <a href="https://github.com/Farama-Foundation/Gymnasium">Farama Foundation Gymnasium</a>
 * environment ({@code Env} in Python).
 */
public class Environment { // TODO: Generalize
	public Observation getObservation(){
		// TODO
		return null;
	};
	public StepTuple step(EntityPlayerActionPack action){
		// TODO
		return null;
	};
	public ResetTuple reset(int seed){
		// TODO
		return null;
	};

	/**
	 * Information returned from {@link Environment#step}
	 */
	public record ResetTuple(Observation observation, Info info) {
		public record Info() {

		}
	}

	/**
	 * Information returned from {@link Environment#step}.
	 */
	public record StepTuple(Observation observation, float reward, Info info) {
		public record Info() {
		}
	}

	public record Observation(List<BlockHitResult> blocks, List<EntityHitResult> entities, ServerPlayerEntity self, List<Observation> history) {
		public static Observation fromPlayer(ServerPlayerEntity player, int raycasts, double maxDistance, double fov, List<Observation> history) { // TODO: Test
			List<BlockHitResult> blocks = new ArrayList<>();
			List<EntityHitResult> entities = new ArrayList<>();
			double sqrtRaycasts = Math.sqrt(raycasts);
			double deltaAngle = fov / raycasts;
			for (double i = -sqrtRaycasts/2; i < sqrtRaycasts/2; i++) {
				for (double j = -sqrtRaycasts/2; j < sqrtRaycasts/2; j++) {
					Vec3d pos = player.getCameraPosVec(1);
					Vec3d rot = player.getRotationVec(1).add(i * deltaAngle, j * deltaAngle,0);
					Vec3d max = pos.add(rot.x * maxDistance, rot.y * maxDistance, rot.x * maxDistance);
					// Blocks (see Entity#raycast)
					BlockHitResult blockHitResult = player.getServerWorld().raycast(new RaycastContext(pos, max, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, player));
					blocks.add(blockHitResult);

					// Entities (see GameRenderer#findCrosshairTarget)
					Box box = player.getBoundingBox().stretch(rot.multiply(maxDistance)).expand(1,1,1);

					@Nullable EntityHitResult entityHitResult = ProjectileUtil.raycast(player, pos, max, box, entity -> !entity.isInvisibleTo(player), Math.pow(maxDistance,2));
					if (entityHitResult != null) {
						entities.add(entityHitResult);
					}
				}
			}
			return new Observation(blocks, entities, player, history);
		}
	}
}
