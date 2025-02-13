/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.function.Consumer;

/**
 * Marks something as an agent. An agent cannot die normally, but can trigger a callback when killed.
 */
public interface AgentCandidate {
    boolean rlmc$isAgent();
    void rlmc$setIsAgent(boolean isAgent);

    default void rlmc$markAsAgent() {
        rlmc$setIsAgent(true);
    }

    default void rlmc$unmarkAsAgent() {
        rlmc$setIsAgent(false);
    }

    void rlmc$setKilledTrigger(Consumer<AgentCandidate> trigger);

    /**
     * Unmark this as an agent and kill it. Killed trigger will not be triggered.
     */
    void rlmc$forceKill();
}
