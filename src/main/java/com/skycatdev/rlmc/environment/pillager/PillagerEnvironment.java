/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment.pillager;

import com.mojang.datafixers.util.Either;
import com.skycatdev.rlmc.NameGenerator;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.SpreadEntitiesHelper;
import com.skycatdev.rlmc.command.EnvironmentSettings;
import com.skycatdev.rlmc.environment.*;
import com.skycatdev.rlmc.mixin.MobEntityAccessor;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

public class PillagerEnvironment extends WorldEnvironment<PillagerEnvironment.Action, PillagerEnvironment.Observation> {
    protected String name;
    @Nullable protected PillagerEntity pillager;
    @Nullable protected IronGolemEntity golem;
    @Nullable protected PillagerGoal goal;

    public PillagerEnvironment(EnvironmentSettings environmentSettings, MinecraftServer server) {
        super(environmentSettings, server);
        this.name = NameGenerator.newName();
    }

    public static Future<PillagerEnvironment> makeAndConnect(EnvironmentSettings environmentSettings, MinecraftServer server) {
        Rlmc.LOGGER.debug("Creating pillager env");
        PillagerEnvironment environment = new PillagerEnvironment(environmentSettings, server);
        return new FutureTask<>(() -> {
            Rlmc.addEnvironment(environment);
            Rlmc.getPythonEntrypoint().connectEnvironment("pillager", environment);
            Rlmc.LOGGER.debug("Connected fight enemy env \"{}\"", environment.getUniqueEnvName());
            return environment;
        });
    }

    @Override
    protected ChunkGenerator getChunkGenerator() {
        return server.getOverworld().getChunkManager().getChunkGenerator();
    }

    @Override
    public String getUniqueEnvName() {
        return name;
    }

    @Override
    protected ResetTuple<Observation> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        if (pillager != null) {
            pillager.discard();
        }
        if (golem != null) {
            golem.discard();
        }
        deleteCurrentWorld();

        // Set up
        @Nullable Pair<BlockPos, BlockPos> spread = SpreadEntitiesHelper.getSpreadLocations(getWorld(),
                getWorld().getSpawnPos(),
                new Vec3i(100, 20, 100),
                new Vec3i(3, 0, 3),
                new Vec3i(10, 5, 10),
                getWorld().getRandom());
        if (spread == null) {
            throw new EnvironmentException(String.format("Couldn't find spread positions for env %s", getUniqueEnvName()));
        }

        pillager = EntityType.PILLAGER.spawn(getWorld(), spread.getLeft(), SpawnReason.COMMAND);
        if (pillager == null) {
            throw new EnvironmentException(String.format("Tried to spawn a pillager for env %s, but it didn't work.", getUniqueEnvName()));
        }
        pillager.setCustomName(Text.of(name));
        pillager.setCustomNameVisible(true);
        ((AgentCandidate) pillager).rlmc$markAsAgent();
        ((DamageTracked) pillager).rlmc$startTrackingDamage();
        GoalSelector goalSelector = ((MobEntityAccessor) pillager).getGoalSelector();
        goalSelector.getGoals().forEach(goalSelector::remove);
        this.goal = new PillagerGoal(pillager);
        goalSelector.add(0, goal);

        golem = EntityType.IRON_GOLEM.spawn(getWorld(), spread.getRight(), SpawnReason.COMMAND);
        if (golem == null) {
            throw new EnvironmentException(String.format("Tried to spawn a golem for env %s, but it didn't work.", getUniqueEnvName()));
        }
        ((AgentCandidate) golem).rlmc$markAsAgent();
        ((DamageTracked) golem).rlmc$startTrackingDamage();


        return new ResetTuple<>(new Observation(goal.getState()), Map.of());
    }

    @Override
    protected Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<Observation>>> innerStep(Action action) {
        @Nullable FutureTask<?> preStep = new FutureTask<>(() -> {
            if (goal == null) throw new EnvironmentException();
            return goal.setNext(action.movement(), action.look(), action.jump(), action.target(), action.attack());
        });
        FutureTask<StepTuple<Observation>> postStep = new FutureTask<>(() -> {
            if (goal == null) throw new EnvironmentException();
            assert pillager != null;
            assert golem != null;
            float reward = ((DamageTracked) pillager).rlmc$getDamageTaken() - ((DamageTracked) golem).rlmc$getDamageTaken();
            return new StepTuple<>(new Observation(goal.getState()), reward, pillager.isDead() || golem.isDead(), false, Map.of());
        });
        return new Pair<>(preStep, postStep);
    }

    @Override
    public Future<Future<? extends Environment<Action, Observation>>> makeAnother() {
        Rlmc.LOGGER.trace("Making another PillagerEnvironment...");
        FutureTask<Future<? extends Environment<Action, Observation>>> futureTask = new FutureTask<>(() ->
                Objects.requireNonNull(PillagerEnvironment.makeAndConnect(settings, server)));
        Rlmc.runBeforeNextTick(futureTask);
        return futureTask;
    }

    public record Observation(PillagerGoal.CrossbowState crossbowState) {

    }

    public record Action(@Nullable Vec3d movement, @Nullable Either<Entity, Vec3d> look, boolean jump,
                         @Nullable LivingEntity target, PillagerGoal.CrossbowAttack attack) {

    }

    public static class PillagerGoal extends Goal {
        protected PillagerEntity agent;
        @Nullable protected Vec3d move;
        @Nullable protected Either<Entity, Vec3d> look;
        protected boolean jump;
        @Nullable protected LivingEntity target;
        protected CrossbowAttack attack;
        protected CrossbowState state;
        protected boolean ready;

        public PillagerGoal(PillagerEntity agent) {
            super();
            this.agent = agent;
            this.jump = false;
            this.attack = CrossbowAttack.NOOP;
            this.ready = false;
            this.state = CrossbowState.NOT_CHARGED;
            setControls(EnumSet.allOf(Control.class));
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public boolean canStop() {
            return false;
        }

        public CrossbowState getState() {
            return state;
        }

        /**
         * Set the next thing this goal should do.
         *
         * @param move   Where to move
         * @param look   Where to look or what to look at
         * @param jump   Whether to jump
         * @param target What to target
         * @return {@code false} if already set (nothing happens), {@code true} otherwise.
         */
        public boolean setNext(@Nullable Vec3d move, @Nullable Either<Entity, Vec3d> look, boolean jump, @Nullable LivingEntity target, CrossbowAttack attack) {
            if (ready) return false;
            this.move = move;
            this.look = look;
            this.jump = jump;
            this.target = target;
            this.attack = attack;
            ready = true;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return true;
        }

        @Override
        public void tick() {
            if (!ready) {
                throw new EnvironmentException("Tried to tick goal when not ready.");
            }
            super.tick();
            if (move != null) {
                agent.getMoveControl().moveTo(move.getX(), move.getY(), move.getZ(), 1.0f); // TODO: Account for crossbow charging
            }
            if (look != null) {
                look.ifLeft(entity -> agent.getLookControl().lookAt(entity));
                look.ifRight(vec -> agent.getLookControl().lookAt(vec));
            }
            if (jump) {
                agent.getJumpControl().setActive();
            }
            if (target != null) {
                agent.setTarget(target);
            }

            if (state == CrossbowState.CHARGING) {
                if (agent.getItemUseTime() >= CrossbowItem.getPullTime(agent.getActiveItem(), agent)) {
                    agent.stopUsingItem();
                    state = CrossbowState.CHARGED;
                    agent.setCharging(false);
                }
            }

            if (attack == CrossbowAttack.CHARGE) {
                if (!agent.isCharging()) {
                    agent.setCharging(true);
                    agent.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(agent, Items.CROSSBOW));
                    state = CrossbowState.CHARGING;
                }
            } else if (attack == CrossbowAttack.SHOOT && state == CrossbowState.CHARGED) {
                agent.shootAt(agent.getTarget(), 1.0f); // Note: Target doesn't seem to matter as of 1.21.1
            }

            ready = false;
        }

        public enum CrossbowState {
            NOT_CHARGED, CHARGING, CHARGED
        }

        public enum CrossbowAttack {
            CHARGE, SHOOT, NOOP
        }

    }

}
