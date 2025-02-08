/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

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
        boolean shouldRun;
        synchronized (initializedLock) {
            shouldRun = this.initialized;
        }
        if (shouldRun) {
            try {
                var tasks = Objects.requireNonNull(queue.poll(10, TimeUnit.HOURS)); // TODO: Wait time is for debug.
                postTick = tasks.getRight();
                if (tasks.getLeft() != null) {
                    tasks.getLeft().run();
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
        FutureTask<ResetTuple<O>> resetTask = new FutureTask<>(() -> innerReset(seed, options));
        try {
            synchronized (initializedLock) {
                initialized = true;
            }
            queue.put(new Pair<>(resetTask, null));
            return resetTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call request a step. Usually should not be overridden. Blocking.
     *
     * @param action The action to take during the step.
     * @return Step information.
     * @see Environment#step(A)
     */
    @SuppressWarnings("unused") // Used by java_environment_wrapper.py
    public StepTuple<O> step(A action) {
        Pair<@Nullable FutureTask<?>, FutureTask<StepTuple<O>>> innerStep = innerStep(action);
        FutureTask<StepTuple<O>> right = innerStep.getRight();
        Pair<FutureTask<?>, FutureTask<?>> castedInnerStep = new Pair<>(innerStep.getLeft() == null ? new FutureTask<>(() -> false) : innerStep.getLeft(), right);
        try {
            queue.put(castedInnerStep);
            // left.get(); // Guaranteed to happen first by the implementation
            return right.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
