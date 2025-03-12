/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import org.jetbrains.annotations.Nullable;

public class EnvironmentExecutionSettings {
    protected int episodes;
    protected @Nullable String savePath;
    protected @Nullable String loadPath;
    protected @Nullable String tensorboardLogName;
    protected String algorithm;
    protected boolean training;
    protected @Nullable String tensorboardLogPath;
    protected @Nullable Double gamma;
    protected @Nullable Double entCoef;
    protected @Nullable Double gaeLambda;
    protected @Nullable Double vfCoef;
    protected @Nullable Double maxGradNorm;
    protected @Nullable Double learningRate;
    protected @Nullable Integer nSteps;

    public EnvironmentExecutionSettings(int episodes, String algorithm, boolean training, @Nullable String savePath, @Nullable String loadPath, @Nullable String tensorboardLogName, @Nullable String tensorboardLogPath, @Nullable Double gamma, @Nullable Double entCoef, @Nullable Double learningRate, @Nullable Double gaeLambda, @Nullable Double vfCoef, @Nullable Double maxGradNorm, @Nullable Integer nSteps) {
        this.episodes = episodes;
        this.savePath = savePath;
        this.loadPath = loadPath;
        this.tensorboardLogName = tensorboardLogName;
        this.algorithm = algorithm;
        this.training = training;
        this.tensorboardLogPath = tensorboardLogPath;
        this.gamma = gamma;
        this.entCoef = entCoef;
        this.gaeLambda = gaeLambda;
        this.vfCoef = vfCoef;
        this.maxGradNorm = maxGradNorm;
        this.learningRate = learningRate;
        this.nSteps = nSteps;
    }

    public EnvironmentExecutionSettings(int episodes, String algorithm) {
        this.episodes = episodes;
        this.algorithm = algorithm;
    }

    public EnvironmentExecutionSettings(int episodes, String algorithm, boolean training, @Nullable String savePath, @Nullable String loadPath, @Nullable String tensorboardLogName, @Nullable String tensorboardLogPath) {
        this.episodes = episodes;
        this.savePath = savePath;
        this.loadPath = loadPath;
        this.tensorboardLogName = tensorboardLogName;
        this.algorithm = algorithm;
        this.training = training;
        this.tensorboardLogPath = tensorboardLogPath;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public @Nullable Double getEntCoef() {
        return entCoef;
    }

    public EnvironmentExecutionSettings setEntCoef(double entCoef) {
        this.entCoef = entCoef;
        return this;
    }

    public int getEpisodes() {
        return episodes;
    }

    public @Nullable Double getGaeLambda() {
        return gaeLambda;
    }

    public EnvironmentExecutionSettings setGaeLambda(double gaeLambda) {
        this.gaeLambda = gaeLambda;
        return this;
    }

    public @Nullable Double getGamma() {
        return gamma;
    }

    public EnvironmentExecutionSettings setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    public @Nullable Double getLearningRate() {
        return learningRate;
    }

    public EnvironmentExecutionSettings setLearningRate(double learningRate) {
        this.learningRate = learningRate;
        return this;
    }

    public @Nullable String getLoadPath() {
        return loadPath;
    }

    public EnvironmentExecutionSettings setLoadPath(String loadPath) {
        this.loadPath = loadPath;
        return this;
    }

    public @Nullable Double getMaxGradNorm() {
        return maxGradNorm;
    }

    public EnvironmentExecutionSettings setMaxGradNorm(double maxGradNorm) {
        this.maxGradNorm = maxGradNorm;
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

    public @Nullable Double getVfCoef() {
        return vfCoef;
    }

    public EnvironmentExecutionSettings setVfCoef(double vfCoef) {
        this.vfCoef = vfCoef;
        return this;
    }

    public @Nullable Integer getNSteps() {
        return nSteps;
    }

    public boolean isTraining() {
        return training;
    }

    public EnvironmentExecutionSettings setNSteps(int nSteps) {
        this.nSteps = nSteps;
        return this;
    }


}
