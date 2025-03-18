/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.skycatdev.rlmc.command.EnvironmentExecutionSettings;
import com.skycatdev.rlmc.environment.Environment;
import org.jetbrains.annotations.Nullable;

public interface PythonEntrypoint {
    /**
     * Connect an environment with the Python side.
     * @param type The type of environment.
     * @param environment The environment.
     */
    void connectEnvironment(String type, Environment<?, ?> environment);

    /**
     * Train or evaluate an agent.
     * @param environment The environment to evaluate in.
     * @param environmentExecutionSettings The settings to execute with.
     * @return Optionally a message when finished.
     */
    @Nullable String runKwargs(Environment<?, ?> environment, EnvironmentExecutionSettings environmentExecutionSettings);
}
