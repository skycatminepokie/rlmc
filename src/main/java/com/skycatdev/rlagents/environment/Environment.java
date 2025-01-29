/* Licensed MIT 2025 */
package com.skycatdev.rlagents.environment;

/**
 * A Java implementation of a <a href="https://github.com/Farama-Foundation/Gymnasium">Farama Foundation Gymnasium</a>
 * environment ({@code Env} in Python).
 * @param <O> Observation type.
 * @param <A> Action type.
 * @param <R> Reward type.
 * @param <I> Info type.
 */
public abstract class Environment<O, A, R extends Number, I> {
	public abstract O getObservation();
	public abstract StepInfo<O, R, I> step(A action);
	public abstract ResetInfo<O, I> reset(int seed);

	/**
	 * Information returned from {@link Environment#step}
	 */
	public record ResetInfo<O, I>(O observation, I info) {
	}

	/**
	 * Information returned from {@link Environment#step}.
	 * @param <O> Observation type
	 * @param <R> Reward type
	 * @param <I> Info type
	 */
	public record StepInfo<O, R extends Number, I>(O observation, R reward, I info) {
	}
}
