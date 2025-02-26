/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.function.Consumer;
import net.minecraft.entity.mob.MobEntity;

public interface MobAgentCandidate extends AgentCandidate {
    void rlmc$setAiCallback(Consumer<MobEntity> callback);
}
