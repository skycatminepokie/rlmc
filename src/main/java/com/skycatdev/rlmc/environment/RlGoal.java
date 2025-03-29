/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.mojang.datafixers.util.Either;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class RlGoal extends Goal {
    protected MobEntity agent;
    @Nullable private Function<MobEntity, @Nullable Vec3d> movementFunction;
    @Nullable private Function<MobEntity, @Nullable Either<Entity, Vec3d>> lookFunction;
    @Nullable private Function<MobEntity, Boolean> jumpFunction;
    @Nullable private Function<MobEntity, @Nullable LivingEntity> targetFunction;
    @Nullable private Consumer<MobEntity> tickCallback;
    /**
     * When initialized, this goal will always try to be running. All fields are guaranteed to be non-null.
     */
    private boolean initialized;

    public RlGoal(MobEntity agent) {
        this.agent = agent;
        this.initialized = false;
        setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.JUMP, Control.TARGET));
    }

    public void init(Function<MobEntity, Vec3d> movementFunction,
                     Function<MobEntity, Either<Entity, Vec3d>> lookFunction,
                     Function<MobEntity, Boolean> jumpFunction,
                     Function<MobEntity, LivingEntity> targetFunction,
                     Consumer<MobEntity> tickCallback){
        this.movementFunction = movementFunction;
        this.lookFunction = lookFunction;
        this.jumpFunction = jumpFunction;
        this.targetFunction = targetFunction;
        this.tickCallback = tickCallback;
        initialized = true;
    }

    @Override
    public boolean canStart() {
        return initialized;
    }

    @Override
    public boolean canStop() {
        return !initialized;
    }

    @Override
    public void tick() {
        if (!initialized) return;
        assert movementFunction != null &&
               lookFunction != null &&
               jumpFunction != null &&
               targetFunction != null &&
               tickCallback != null;

        @Nullable Vec3d moveTo = movementFunction.apply(agent);
        if (moveTo != null) {
            agent.getMoveControl().moveTo(moveTo.getX(), moveTo.getY(), moveTo.getZ(), agent.speed);
        }

        @Nullable Either<Entity, Vec3d> lookAt = lookFunction.apply(agent);
        if (lookAt != null) {
            lookAt.ifLeft(entity -> agent.getLookControl().lookAt(entity));
            lookAt.ifRight(lookVec -> agent.getLookControl().lookAt(lookVec));
        }

        if (jumpFunction.apply(agent)) {
            agent.getJumpControl().setActive();;
        }

        agent.setTarget(targetFunction.apply(agent));

        tickCallback.accept(agent);
    }
}
