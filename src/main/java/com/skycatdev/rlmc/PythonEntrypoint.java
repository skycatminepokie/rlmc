/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.skycatdev.rlmc.environment.Environment;

public interface PythonEntrypoint {
    /**
     * Connect an environment with the Python side.
     * @param type The type of environment.
     * @param environment The environment.
     */
    void connectEnvironment(String type, Environment<?, ?> environment);
    /**
     * Train an agent for an environment.
     * @param environment The environment to train for.
     * @param episodes The number of episodes to train for
     * @param savePath The path to save to. Paths are handled by Python, so beware!
     */
    void train(Environment<?, ?> environment, int episodes, String savePath);
    /**
     * Train an agent for an environment.
     * @param environment The environment to train for.
     * @param episodes The number of episodes to train for
     * @param savePath The path to save to. Paths are handled by Python, so beware!
     * @param loadPath The path to load from. Paths are handled by Python, so beware!
     */
    void train(Environment<?, ?> environment, int episodes, String savePath, String loadPath);
}
