/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.skycatdev.rlmc.command.EnvironmentExecutionSettingsBuilder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin implements EnvironmentExecutionSettingsBuilder {
    @Unique @Nullable protected String rlmc$savePath;
    @Unique @Nullable protected String rlmc$loadPath;
    @Unique @Nullable protected String rlmc$tensorboardLogName;
    @Unique @Nullable protected String rlmc$tensorboardLogPath;
    @Unique @Nullable protected String rlmc$algorithm;
    @Unique protected Map<String, Object> rlmc$algorithmArgs = new HashMap<>();
    @Unique protected int rlmc$episodes = 10_000;
    @Unique protected boolean rlmc$training = true;
    @Unique @Nullable protected Double rlmc$maxGradNorm;
    @Unique @Nullable protected Double rlmc$entCoef;
    @Unique @Nullable protected Integer rlmc$nSteps;
    @Unique @Nullable protected Double rlmc$gaeLambda;
    @Unique @Nullable protected Double rlmc$learningRate;
    @Unique @Nullable protected Double rlmc$gamma;
    @Unique @Nullable protected Double rlmc$vfCoef;
    @Unique protected List<Integer> rlmc$netArch = new LinkedList<>();

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$clearAlgorithmArg(String key) {
        rlmc$algorithmArgs.remove(key);
        return this;
    }

    @Override
    public String rlmc$getAlgorithmOrDefault() {
        return rlmc$algorithm == null ? "PPO" : rlmc$algorithm;
    }

    @Override
    public @Nullable Double rlmc$getEntCoef() {
        return rlmc$entCoef;
    }

    @Override
    public int rlmc$getEpisodesOrDefault() {
        return rlmc$episodes;
    }

    @Override
    public @Nullable Double rlmc$getGaeLambda() {
        return rlmc$gaeLambda;
    }

    @Override
    public @Nullable Double rlmc$getGamma() {
        return rlmc$gamma;
    }

    @Override
    public @Nullable Double rlmc$getLearningRate() {
        return rlmc$learningRate;
    }

    @Override
    public @Nullable String rlmc$getLoadPath() {
        return rlmc$loadPath;
    }

    @Override
    public @Nullable Double rlmc$getMaxGradNorm() {
        return rlmc$maxGradNorm;
    }

    @Override
    public @Nullable Integer rlmc$getNSteps() {
        return rlmc$nSteps;
    }

    @Override
    public @Nullable String rlmc$getSavePath() {
        return rlmc$savePath;
    }

    @Override
    public @Nullable String rlmc$getTensorboardLogName() {
        return rlmc$tensorboardLogName;
    }

    @Override
    public @Nullable String rlmc$getTensorboardLogPath() {
        return rlmc$tensorboardLogPath;
    }

    @Override
    public @Nullable Double rlmc$getVfCoef() {
        return rlmc$vfCoef;
    }

    @Override
    public boolean rlmc$isTrainingOrDefault() {
        return rlmc$training;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setAlgorithm(String algorithm) {
        rlmc$algorithm = algorithm;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setAlgorithmArg(String key, Object value) {
        rlmc$algorithmArgs.put(key, value);
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setEntCoef(double entCoef) {
        rlmc$entCoef = entCoef;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setEpisodes(int episodes) {
        rlmc$episodes = episodes;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setEvaluating() {
        rlmc$training = false;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setGaeLambda(double gaeLambda) {
        rlmc$gaeLambda = gaeLambda;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setGamma(double gamma) {
        rlmc$gamma = gamma;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setLearningRate(double learningRate) {
        rlmc$learningRate = learningRate;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setLoadPath(String loadPath) {
        rlmc$loadPath = loadPath;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setMaxGradNorm(double maxGradNorm) {
        rlmc$maxGradNorm = maxGradNorm;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setNSteps(int nSteps) {
        rlmc$nSteps = nSteps;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setSavePath(String savePath) {
        rlmc$savePath = savePath;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setTensorboardLog(String logPath, String logName) {
        rlmc$tensorboardLogName = logName;
        rlmc$tensorboardLogPath = logPath;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setTraining() {
        rlmc$training = true;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setVfCoef(double vfCoef) {
        rlmc$vfCoef = vfCoef;
        return this;
    }

    @Override
    public Integer @Nullable [] rlmc$getNetArch() {
        if (rlmc$netArch.isEmpty()) {
            return null;
        }
        return rlmc$netArch.toArray(Integer[]::new);
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$addNetLayer(int neurons) {
        rlmc$netArch.add(neurons);
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$clearNetArch() {
        rlmc$netArch.clear();
        return this;
    }
}
