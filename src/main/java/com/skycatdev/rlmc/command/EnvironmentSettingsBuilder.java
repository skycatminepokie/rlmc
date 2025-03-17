/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.Contract;

public interface EnvironmentSettingsBuilder {
    default EnvironmentSettings rlmc$buildEnvironmentSettings() {
        return new EnvironmentSettings(rlmc$isUsingMonitor(), rlmc$getTimeLimit(), rlmc$getFrameStack());
    }

    @IntRange(from = 1) int rlmc$getFrameStack();

    @IntRange(from = 0) int rlmc$getTimeLimit();

    default boolean rlmc$isFrameStacking() {
        return rlmc$getFrameStack() > 1;
    }

    default boolean rlmc$hasTimeLimit() {
        return rlmc$getTimeLimit() > 0;
    };

    boolean rlmc$isUsingMonitor();

    /**
     * Remove frame stacking by setting it to 1.
     *
     * @return {@code this}
     */
    @Contract("->this")
    default EnvironmentSettingsBuilder rlmc$removeFrameStack() {
        rlmc$setFrameStack(1);
        return this;
    }

    /**
     * Remove the time limit by setting it to 0.
     *
     * @return {@code this}
     */
    @Contract("->this")
    default EnvironmentSettingsBuilder rlmc$removeTimeLimit() {
        rlmc$setTimeLimit(0);
        return this;
    }

    /**
     * Set how many frames to stack. 1 means no frame stacking.
     *
     * @param frameStack Number of frames to stack
     * @return {@code this}
     */
    @Contract("_->this")
    EnvironmentSettingsBuilder rlmc$setFrameStack(@IntRange(from = 1) int frameStack);

    /**
     * Set the time limit.
     *
     * @param timeLimit The number of steps to limit to. 0 means no limit.
     * @return {@code this}
     */
    @Contract("_->this")
    EnvironmentSettingsBuilder rlmc$setTimeLimit(@IntRange(from = 0) int timeLimit);

    /**
     * Set whether to wrap the environment in a Monitor
     *
     * @param useMonitor whether to wrap the environment in a Monitor
     * @return {@code this}
     */
    @Contract("_->this")
    EnvironmentSettingsBuilder rlmc$setUseMonitor(boolean useMonitor);

    /**
     * Wrap the environment in a Monitor
     *
     * @return {@code this}
     */
    @Contract("->this")
    default EnvironmentSettingsBuilder rlmc$useMonitor() {
        rlmc$setUseMonitor(true);
        return this;
    }

}
