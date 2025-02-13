/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.function.Consumer;

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
}
