/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

public interface AgentCandidate {
    boolean rlmc$isAgent();

    default void rlmc$markAsAgent() {
        rlmc$setIsAgent(true);
    }

    void rlmc$setIsAgent(boolean isAgent);

    default void rlmc$unmarkAsAgent() {
        rlmc$setIsAgent(false);
    }
}
