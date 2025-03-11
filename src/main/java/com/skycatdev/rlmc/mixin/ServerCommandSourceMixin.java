/* Licensed MIT 2025 */
package com.skycatdev.rlmc.mixin;

import com.skycatdev.rlmc.command.EnvironmentExecutionSettingsBuilder;
import java.util.HashMap;
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

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setTraining() {
        rlmc$training = true;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setEvaluating() {
        rlmc$training = false;
        return this;
    }

    @Override
    public boolean rlmc$isTrainingOrDefault() {
        return rlmc$training;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setAlgorithmArg(String key, Object value) {
        rlmc$algorithmArgs.put(key, value);
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$clearAlgorithmArg(String key) {
        rlmc$algorithmArgs.remove(key);
        return this;
    }

    @Override
    public Map<String, Object> rlmc$getAlgorithmArgs() {
        return rlmc$algorithmArgs;
    }

    @Override
    public String rlmc$getAlgorithmOrDefault() {
        return rlmc$algorithm == null ? "PPO" : rlmc$algorithm;
    }

    @Override
    public int rlmc$getEpisodesOrDefault() {
        return rlmc$episodes;
    }

    @Override
    public @Nullable String rlmc$getLoadPath() {
        return rlmc$loadPath;
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
    public EnvironmentExecutionSettingsBuilder rlmc$setAlgorithm(String algorithm) {
        rlmc$algorithm = algorithm;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setEpisodes(int episodes) {
        rlmc$episodes = episodes;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setLoadPath(String loadPath) {
        rlmc$loadPath = loadPath;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setSavePath(String savePath) {
        rlmc$savePath = savePath;
        return this;
    }

    @Override
    public EnvironmentExecutionSettingsBuilder rlmc$setTensorboardLog(String logName, String logPath) {
        rlmc$tensorboardLogName = logName;
        rlmc$tensorboardLogPath = logPath;
        return this;
    }
}
