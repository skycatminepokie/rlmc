/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.mojang.datafixers.util.Either;
import com.skycatdev.rlmc.Rlmc;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * A Java implementation of a <a href="https://github.com/Farama-Foundation/Gymnasium">Farama Foundation Gymnasium</a>
 * environment ({@code Env} in Python). See java_environment_wrapper.py. To create a new environment:<br>
 * 1. Extend this class<br>
 * 2. Choose an action and observation type<br>
 * 3. Implement methods<br>
 * 4. Create a matching class in Python (see java_environment_wrapper.py)
 *
 * @param <A> Action type
 * @param <O> Observation type
 */
public abstract class Environment<A, O> {
    private final Object[] initializedLock = new Object[]{};
    private final Object[] closedLock = new Object[]{};
    private final Object[] pausedLock = new Object[]{};
    private final Object[] taskLock = new Object[0];
    /**
     * What to do on the next post-tick. Do not access outside the server thread or during a tick.
     */
    private @Nullable FutureTask<?> postTick;
    /**
     * True when {@link Environment#close()} has been called at least once. Synchronize on {@link Environment#closedLock} first.
     */
    private boolean closed;
    /**
     * True when {@link Environment#pause()} has been called more recently than either {@link Environment#step} or {@link Environment#reset(Integer, Map)}.
     * Synchronize on {@link Environment#pausedLock} first.
     */
    private boolean paused;
    private @Nullable Either<Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>>, FutureTask<ResetTuple<O>>> task;

    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public void close() {
        synchronized (closedLock) {
            closed = true;
            Rlmc.removeEnvironment(this);
        }
    }

    /**
     * Used for logging. Should be reasonably unique, but human-readable. May be called many times, please don't make this computation-heavy.
     */
    public abstract String getUniqueEnvName();

    /**
     * Will be called at the beginning of a tick when a reset is requested. Should be blocking.
     *
     * @param seed    The seed to use for random actions.
     * @param options The options to use.
     * @return Reset information
     */
    protected abstract ResetTuple<O> innerReset(@Nullable Integer seed, @Nullable Map<String, Object> options);

    /**
     * This will be called when a step is requested. Should not block for very long - don't do tasks inside this method.
     *
     * @param action The action to take during this step.
     * @return A pair of tasks. The first will be executed before the tick, the second will be executed after the tick. These should be blocking.
     */
    protected abstract Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>> innerStep(A action);

    public boolean isClosed() {
        synchronized (closedLock) {
            return closed;
        }
    }

    public abstract boolean isIn(ServerWorld world);

    public boolean isPaused() {
        synchronized (pausedLock) {
            return paused;
        }
    }

    /**
     * Make another environment with compatible settings. For example, a player name may need to be different, but an entity type might be the same.
     * This is used for making vectorized environments.
     */
    @SuppressWarnings("unused") // Used by entrypoint.py
    public abstract Future<? extends Environment<A, O>> makeAnother();

    public void pause() {
        synchronized (pausedLock) {
            paused = true;
        }
    }

    /**
     * Tasks to be done after a tick. Usually should not be overridden.
     *
     * @see Environment#innerStep(A)
     * @see Environment#innerReset(Integer, Map)
     */
    public void postTick() {
        if (postTick != null) {
            postTick.run();
            postTick = null;
        }
    }

    /**
     * Tasks to be done before a tick. Usually should not be overridden. Only call if {@link Environment#waitingForTick()} is true.
     *
     * @see Environment#innerStep(A)
     * @see Environment#innerReset(Integer, Map)
     */
    public void preTick() {
        if (shouldTick()) { // Check if we are paused or closed or something
            synchronized (taskLock) {
                if (task == null) {
                    Rlmc.LOGGER.warn("Task was null in pre-tick, please report this!");
                } else {
                    var stepOpt = task.left();
                    if (stepOpt.isPresent()) { // If we're stepping
                        var step = stepOpt.get();
                        postTick = step.getRight(); // Remember the post-step tasks
                        if (step.getLeft() != null) {
                            step.getLeft().run(); // Do the pre-step tasks
                        }
                    } else {
                        var resetOpt = task.right();
                        assert resetOpt.isPresent() : "Sanity check failed - left was gone but so was right?"; // Guess I'm insane
                        postTick = resetOpt.get();
                    }
                    task = null;
                }
            }
        }
    }

    /**
     * Called to request a reset. Usually should not be overridden. Blocking.
     *
     * @param seed    The seed to use for random operations.
     * @param options The options to use.
     * @return Reset information.
     * @see Environment#innerReset(Integer, Map)
     */
    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public ResetTuple<O> reset(@Nullable Integer seed, @Nullable Map<String, Object> options) {
        unpause();
        Rlmc.LOGGER.debug("Resetting environment \"{}\" (reset called)", getUniqueEnvName());
        FutureTask<ResetTuple<O>> resetTask;
        synchronized (initializedLock) {
            synchronized (taskLock) {
                if (task == null) {
                    resetTask = new FutureTask<>(() -> innerReset(seed, options));
                    task = Either.right(resetTask);
                    Rlmc.LOGGER.debug("Environment \"{}\" initialized. Waiting for reset.", getUniqueEnvName());
                } else {
                    throw new EnvironmentException("Expected task to be null, but it wasn't. Did you call reset/step from two different threads?");
                }
            }
        }
        try {
            ResetTuple<O> ret = resetTask.get();
            Rlmc.LOGGER.debug("Environment \"{}\" reset received, returning.", getUniqueEnvName());
            return ret;
        } catch (InterruptedException | ExecutionException e) {
            throw new EnvironmentException(e);
        }
    }

    protected boolean shouldTick() {
        return !isClosed() && !isPaused();
    }

    /**
     * Request a step. Usually should not be overridden. Blocking.
     *
     * @param action The action to take during the step.
     * @return Step information.
     * @see Environment#innerStep(A)
     */
    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public StepTuple<O> step(A action) {
        FutureTask<StepTuple<O>> stepPostTick;
        synchronized (taskLock) {
            if (task == null) {
                Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>> innerStep = innerStep(action);
                stepPostTick = innerStep.getRight();
                task = Either.left(innerStep);
            } else {
                throw new EnvironmentException("Expected null task once synchronized in Environment#step. Did you call reset/step from two different threads?");
            }
        }
        try {
            return stepPostTick.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new EnvironmentException("There was a problem waiting for stepPostTick.", e);
        }
    }

    public void unpause() {
        synchronized (pausedLock) {
            paused = false;
        }
        Rlmc.LOGGER.debug("Environment \"{}\" unpaused.", getUniqueEnvName());
    }

    public boolean waitingForTick() {
        synchronized (taskLock) {
            return task != null;
        }
    }
}
