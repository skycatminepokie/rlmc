/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface EnvironmentExecutionSettingsBuilder {
    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$addNetLayer(int neurons);

    default EnvironmentExecutionSettings rlmc$build() {
        return new EnvironmentExecutionSettings(rlmc$getEpisodesOrDefault(), rlmc$getAlgorithmOrDefault(), rlmc$isTrainingOrDefault(), rlmc$getSavePath(), rlmc$getLoadPath(), rlmc$getTensorboardLogName(), rlmc$getTensorboardLogPath(), rlmc$getGamma(), rlmc$getEntCoef(), rlmc$getLearningRate(), rlmc$getGaeLambda(), rlmc$getVfCoef(), rlmc$getMaxGradNorm(), rlmc$getNSteps(), rlmc$getNetArch(), rlmc$getBatchSize());
    }

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$clearAlgorithmArg(String key);

    @Contract("->this")
    EnvironmentExecutionSettingsBuilder rlmc$clearNetArch();

    String rlmc$getAlgorithmOrDefault();

    @Nullable Integer rlmc$getBatchSize();

    @Nullable Double rlmc$getEntCoef();

    int rlmc$getEpisodesOrDefault();

    @Nullable Double rlmc$getGaeLambda();

    @Nullable Double rlmc$getGamma();

    @Nullable Double rlmc$getLearningRate();

    @Nullable String rlmc$getLoadPath();

    @Nullable Double rlmc$getMaxGradNorm();

    @Nullable Integer rlmc$getNSteps();

    Integer @Nullable [] rlmc$getNetArch();

    @Nullable String rlmc$getSavePath();

    /**
     * @return Non-null if {@link EnvironmentExecutionSettingsBuilder#rlmc$getTensorboardLogPath()} is non-null.
     */
    @Nullable String rlmc$getTensorboardLogName();

    /**
     * @return Non-null if {@link EnvironmentExecutionSettingsBuilder#rlmc$getTensorboardLogName()} is non-null.
     */
    @Nullable String rlmc$getTensorboardLogPath();

    @Nullable Double rlmc$getVfCoef();

    /**
     * @return {@code true} if in training mode, {@code false} if in evaluation mode
     */
    boolean rlmc$isTrainingOrDefault();

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setAlgorithm(String algorithm);

    @Contract("_,_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setAlgorithmArg(String key, Object value);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setBatchSize(int batchSize);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setEntCoef(double entCoef);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setEpisodes(int episodes);

    @Contract("->this")
    EnvironmentExecutionSettingsBuilder rlmc$setEvaluating();

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setGaeLambda(double gaeLambda);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setGamma(double gamma);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setLearningRate(double learningRate);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setLoadPath(String loadPath);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setMaxGradNorm(double maxGradNorm);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setNSteps(int nSteps);

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setSavePath(String savePath);

    @Contract("_,_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setTensorboardLog(String logPath, String logName);

    @Contract("->this")
    EnvironmentExecutionSettingsBuilder rlmc$setTraining();

    @Contract("_->this")
    EnvironmentExecutionSettingsBuilder rlmc$setVfCoef(double vfCoef);
}
