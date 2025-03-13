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
    protected Integer @Nullable [] netArch;
    protected @Nullable Integer batchSize;

    public EnvironmentExecutionSettings(int episodes, String algorithm, boolean training, @Nullable String savePath, @Nullable String loadPath, @Nullable String tensorboardLogName, @Nullable String tensorboardLogPath, @Nullable Double gamma, @Nullable Double entCoef, @Nullable Double learningRate, @Nullable Double gaeLambda, @Nullable Double vfCoef, @Nullable Double maxGradNorm, @Nullable Integer nSteps, Integer @Nullable [] netArch, @Nullable Integer batchSize) {
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
        this.netArch = netArch;
        this.batchSize = batchSize;
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

    @SuppressWarnings("unused") // Used by entrypoint.py
    public String getAlgorithm() {
        return algorithm;
    }

    public @Nullable Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(@Nullable Integer batchSize) {
        this.batchSize = batchSize;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
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

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable Double getGaeLambda() {
        return gaeLambda;
    }

    public EnvironmentExecutionSettings setGaeLambda(double gaeLambda) {
        this.gaeLambda = gaeLambda;
        return this;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable Double getGamma() {
        return gamma;
    }

    public EnvironmentExecutionSettings setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
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

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable Double getMaxGradNorm() {
        return maxGradNorm;
    }

    public EnvironmentExecutionSettings setMaxGradNorm(double maxGradNorm) {
        this.maxGradNorm = maxGradNorm;
        return this;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable Integer getNSteps() {
        return nSteps;
    }

    public EnvironmentExecutionSettings setNSteps(int nSteps) {
        this.nSteps = nSteps;
        return this;
    }

    public Integer @Nullable [] getNetArch() {
        return netArch;
    }

    public void setNetArch(Integer @Nullable [] netArch) {
        this.netArch = netArch;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable String getSavePath() {
        return savePath;
    }

    public EnvironmentExecutionSettings setSavePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
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

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable String getTensorboardLogPath() {
        return tensorboardLogPath;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public @Nullable Double getVfCoef() {
        return vfCoef;
    }

    public EnvironmentExecutionSettings setVfCoef(double vfCoef) {
        this.vfCoef = vfCoef;
        return this;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public boolean isTraining() {
        return training;
    }

}
