/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface EnvironmentExecutionSettingsBuilder {
    @Contract("_,_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setAlgorithmArg(String key, Object value);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$clearAlgorithmArg(String key);

    Map<String, Object> rlmc$getAlgorithmArgs();

    String rlmc$getAlgorithmOrDefault();

    int rlmc$getEpisodesOrDefault();

    @Nullable String rlmc$getLoadPath();

    @Nullable String rlmc$getSavePath();

    /**
     * @return Non-null if {@link EnvironmentExecutionSettingsBuilder#rlmc$getTensorboardLogPath()} is non-null.
     */
    @Nullable String rlmc$getTensorboardLogName();

    /**
     * @return Non-null if {@link EnvironmentExecutionSettingsBuilder#rlmc$getTensorboardLogName()} is non-null.
     */
    @Nullable String rlmc$getTensorboardLogPath();

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setAlgorithm(String algorithm);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setEpisodes(int episodes);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setLoadPath(String loadPath);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setSavePath(String savePath);

    @Contract("_,_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setTensorboardLog(String logName, String logPath);
    default EnvironmentExecutionSettings rlmc$build() {
        return new EnvironmentExecutionSettings(rlmc$getEpisodesOrDefault(), rlmc$getAlgorithmOrDefault(), rlmc$getAlgorithmArgs(), rlmc$isTrainingOrDefault(), rlmc$getSavePath(), rlmc$getLoadPath(), rlmc$getTensorboardLogName(), rlmc$getTensorboardLogPath());
    }
    EnvironmentExecutionSettingsBuilder rlmc$setTraining();
    EnvironmentExecutionSettingsBuilder rlmc$setEvaluating();

    /**
     * @return {@code true} if in training mode, {@code false} if in evaluation mode
     */
    boolean rlmc$isTrainingOrDefault();
}
