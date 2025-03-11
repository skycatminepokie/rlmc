/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class EnvironmentExecutionSettings {
    protected int episodes;
    protected @Nullable String savePath;
    protected @Nullable String loadPath;
    protected @Nullable String tensorboardLogName;
    protected Map<String, Object> algorithmArgs = new HashMap<>();
    protected String algorithm;
    protected boolean training;
    protected @Nullable String tensorboardLogPath;

    public EnvironmentExecutionSettings(int episodes, String algorithm) {
        this.episodes = episodes;
        this.algorithm = algorithm;
    }

    public EnvironmentExecutionSettings(int episodes, String algorithm, Map<String, Object> algorithmArgs, boolean training, @Nullable String savePath, @Nullable String loadPath, @Nullable String tensorboardLogName, @Nullable String tensorboardLogPath) {
        this.episodes = episodes;
        this.savePath = savePath;
        this.loadPath = loadPath;
        this.tensorboardLogName = tensorboardLogName;
        this.algorithmArgs = algorithmArgs;
        this.algorithm = algorithm;
        this.training = training;
        this.tensorboardLogPath = tensorboardLogPath;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Map<String, Object> getAlgorithmArgs() {
        return algorithmArgs;
    }

    public int getEpisodes() {
        return episodes;
    }

    public @Nullable String getLoadPath() {
        return loadPath;
    }

    public EnvironmentExecutionSettings setLoadPath(String loadPath) {
        this.loadPath = loadPath;
        return this;
    }

    public @Nullable String getSavePath() {
        return savePath;
    }

    public EnvironmentExecutionSettings setSavePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    public @Nullable String getTensorboardLogName() {
        if (tensorboardLogName != null) {
            return tensorboardLogName;
        }
        if (savePath != null) {
            return savePath;
        }
        return null;
    }

    public EnvironmentExecutionSettings setTensorboardLogName(@Nullable String tensorboardLogName) {
        this.tensorboardLogName = tensorboardLogName;
        return this;
    }

    public @Nullable String getTensorboardLogPath() {
        return tensorboardLogPath;
    }

    public boolean isTraining() {
        return training;
    }

    public EnvironmentExecutionSettings setEntCoef(double entCoef) {
        algorithmArgs.put("ent_coef", entCoef);
        return this;
    }

    public EnvironmentExecutionSettings setGaeLambda(double gaeLambda) {
        algorithmArgs.put("gae_lambda", gaeLambda);
        return this;
    }

    public EnvironmentExecutionSettings setGamma(double gamma) {
        algorithmArgs.put("gamma", gamma);
        return this;
    }

    public EnvironmentExecutionSettings setLearningRate(double learningRate) {
        algorithmArgs.put("learning_rate", learningRate);
        return this;
    }

    public EnvironmentExecutionSettings setMaxGradNorm(double maxGradNorm) {
        algorithmArgs.put("max_grad_norm", maxGradNorm);
        return this;
    }

    public EnvironmentExecutionSettings setNSteps(int nSteps) {
        algorithmArgs.put("n_steps", nSteps);
        return this;
    }

    public EnvironmentExecutionSettings setVfCoef(double vfCoef) {
        algorithmArgs.put("vf_coef", vfCoef);
        return this;
    }


}
