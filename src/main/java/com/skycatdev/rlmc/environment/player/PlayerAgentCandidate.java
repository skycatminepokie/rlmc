/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment.player;

import com.skycatdev.rlmc.environment.AgentCandidate;
import java.util.function.Consumer;

/**
 * Marks something as an agent. An agent cannot die normally, but can trigger a callback when killed.
 */
public interface PlayerAgentCandidate extends AgentCandidate {

    void rlmc$setKilledTrigger(Consumer<PlayerAgentCandidate> trigger);

    /**
     * Unmark this as an agent and kill it. Killed trigger will not be triggered.
     */
    void rlmc$forceKill();
}
