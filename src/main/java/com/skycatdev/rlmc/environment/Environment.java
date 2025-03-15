/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import com.skycatdev.rlmc.Rlmc;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * A Java implementation of a <a href="https://github.com/Farama-Foundation/Gymnasium">Farama Foundation Gymnasium</a>
 * environment ({@code Env} in Python). See java_environment_wrapper.py. To create a new environment:<br>
 * 1. Extend this class<br>
 * 2. Choose an action and observation type<br>
 * 3. Implement {@link Environment#innerStep(A)} and {@link Environment#innerReset(Integer, Map)}<br>
 * 4. Create a matching class in Python (see java_environment_wrapper.py)
 *
 * @param <A> Action type
 * @param <O> Observation type
 */
public abstract class Environment<A, O> {
    private final Object[] initializedLock = new Object[]{};
    private final Object[] closedLock = new Object[]{};
    private final Object[] pausedLock = new Object[]{};
    /**
     * Queue of things to do around a tick. Left is before tick, right is after tick. Left will always be finished before right.
     */
    protected SynchronousQueue<Pair<@Nullable FutureTask<?>, @Nullable FutureTask<?>>> queue = new SynchronousQueue<>();
    /**
     * What to do on the next post-tick. Do not access outside the server thread or during a tick.
     */
    protected @Nullable FutureTask<?> postTick;
    /**
     * True when {@link Environment#reset(Integer, Map)} has been called at least once. Synchronize on {@link Environment#initializedLock} first.
     */
    private boolean initialized;
    /**
     * True when {@link Environment#close()} has been called at least once. Synchronize on {@link Environment#closedLock} first.
     */
    private boolean closed;
    /**
     * True when {@link Environment#pause()} has been called more recently than either {@link Environment#step} or {@link Environment#reset(Integer, Map)}.
     * Synchronize on {@link Environment#pausedLock} first.
     */
    private boolean paused;

    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public void close() {
        synchronized (closedLock) {
            closed = true;
            Rlmc.removeEnvironment(this);
        }
        new Thread(() -> {
            try {
                queue.offer(new Pair<>(null, null), 1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // We're just trying to flush it
            }
            queue.drainTo(new ArrayList<>());
        }, "RLMC Closing thread").start();
    }

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

    public boolean isPaused() {
        synchronized (pausedLock) {
            return paused;
        }
    }

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
     * Tasks to be done before a tick. Usually should not be overridden.
     *
     * @see Environment#innerStep(A)
     * @see Environment#innerReset(Integer, Map)
     */
    public void preTick() {
        if (shouldTick()) {
            Rlmc.LOGGER.debug("Preparing to pre-tick environment \"{}\"", getUniqueEnvName());
            try {
                @Nullable var tasks = queue.poll(1, TimeUnit.MINUTES); // TODO Wait time is for debug, it probably shouldn't be this long
                if (tasks == null) {
                    throw new EnvironmentException("Expected non-null tasks, got null. This could be because Python shut down.");
                }
                postTick = tasks.getRight();
                Rlmc.LOGGER.debug("Pre-ticking environment \"{}\"", getUniqueEnvName());
                if (tasks.getLeft() != null) {
                    tasks.getLeft().run();
                    Rlmc.LOGGER.debug("Pre-ticked environment \"{}\"", getUniqueEnvName());
                } else {
                    Rlmc.LOGGER.debug("Skipping pre-tick for environment \"{}\", task was null.", getUniqueEnvName());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
        Rlmc.LOGGER.debug("Resetting environment \"{}\" (reset called)", getUniqueEnvName());
        unpause();
        FutureTask<ResetTuple<O>> resetTask = new FutureTask<>(() -> {
            Rlmc.LOGGER.debug("Resetting environment \"{}\" (innerReset called)", getUniqueEnvName());
            return innerReset(seed, options);
        });
        try {
            synchronized (initializedLock) {
                initialized = true;
            }
            Rlmc.LOGGER.debug("Environment \"{}\" marked as initialized. Waiting for reset.", getUniqueEnvName());
            queue.put(new Pair<>(resetTask, null));
            Rlmc.LOGGER.debug("Environment \"{}\" reset received, returning.", getUniqueEnvName());
            return resetTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used for logging. Should be reasonably unique, but human-readable. May be called many times, please don't make this computation-heavy.
     */
    public abstract String getUniqueEnvName();

    public boolean shouldTick() {
        boolean shouldRun;
        synchronized (initializedLock) {
            shouldRun = initialized;
        }
        synchronized (closedLock) {
            shouldRun = shouldRun && !closed;
        }
        synchronized (pausedLock) {
            shouldRun = shouldRun && !paused;
        }
        return shouldRun;
    }

    /**
     * Request a step. Usually should not be overridden. Blocking.
     *
     * @param action The action to take during the step.
     * @return Step information.
     * @see Environment#step(A)
     */
    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public StepTuple<O> step(A action) {
        unpause();
        Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>> innerStep = innerStep(action);
        FutureTask<StepTuple<O>> postTick = innerStep.getRight();
        Pair<FutureTask<?>, FutureTask<?>> castedInnerStep = new Pair<>(innerStep.getLeft() == null ? new FutureTask<>(() -> false) : innerStep.getLeft(), postTick);
        try {
            Rlmc.LOGGER.debug("Queueing step for environment \"{}\"", getUniqueEnvName());
            queue.put(castedInnerStep);
            // left.get(); // Guaranteed to happen first by the implementation
            return postTick.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract boolean isIn(ServerWorld world);

    public void unpause() {
        synchronized (pausedLock) {
            paused = false;
        }
        Rlmc.LOGGER.debug("Environment \"{}\" unpaused.", getUniqueEnvName());
    }

    /**
     * Make another environment with compatible settings. For example, a player name may need to be different, but an entity type might be the same.
     * This is used for making vectorized environments.
     */
    public abstract Future<? extends Environment<A, O>> makeAnother();
}
