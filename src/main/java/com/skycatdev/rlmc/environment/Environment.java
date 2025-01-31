/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A Java implementation of a <a href="https://github.com/Farama-Foundation/Gymnasium">Farama Foundation Gymnasium</a>
 * environment ({@code Env} in Python).
 * @param <A> Action type
 * @param <O> Observation type
 */
public abstract class Environment<A, O> {
	@SuppressWarnings("unused") // Used by java_environment_wrapper.py
	public abstract StepTuple<O> step(A action);
	@SuppressWarnings("unused") // Used by java_environment_wrapper.py
	public abstract ResetTuple<O> reset(@Nullable Integer seed, @Nullable Map<String, Object> options);

}
