/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class TrainingSettings {
    protected int episodes;
    protected @Nullable String savePath;
    protected @Nullable String loadPath;
    protected @Nullable String tensorboardLogName;
    protected Map<String, Object> algorithmArgs = new HashMap<>();
    protected String algorithm;

    public TrainingSettings(int episodes, String algorithm) {
        this.episodes = episodes;
        this.algorithm = algorithm;
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

    public TrainingSettings setLoadPath(String loadPath) {
        this.loadPath = loadPath;
        return this;
    }

    public @Nullable String getSavePath() {
        return savePath;
    }

    public TrainingSettings setSavePath(String savePath) {
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

    public TrainingSettings setTensorboardLogName(@Nullable String tensorboardLogName) {
        this.tensorboardLogName = tensorboardLogName;
        return this;
    }

    public TrainingSettings setEntCoef(double entCoef) {
        algorithmArgs.put("ent_coef", entCoef);
        return this;
    }

    public TrainingSettings setGaeLambda(double gaeLambda) {
        algorithmArgs.put("gae_lambda", gaeLambda);
        return this;
    }

    public TrainingSettings setGamma(double gamma) {
        algorithmArgs.put("gamma", gamma);
        return this;
    }

    public TrainingSettings setLearningRate(double learningRate) {
        algorithmArgs.put("learning_rate", learningRate);
        return this;
    }

    public TrainingSettings setMaxGradNorm(double maxGradNorm) {
        algorithmArgs.put("max_grad_norm", maxGradNorm);
        return this;
    }

    public TrainingSettings setNSteps(int nSteps) {
        algorithmArgs.put("n_steps", nSteps);
        return this;
    }

    public TrainingSettings setVfCoef(double vfCoef) {
        algorithmArgs.put("vf_coef", vfCoef);
        return this;
    }


}
