/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.*;
import com.skycatdev.rlmc.environment.player.FightEnemyEnvironment;
import com.skycatdev.rlmc.environment.player.GoNorthEnvironment;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.command.argument.RegistryKeyArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class CommandManager implements CommandRegistrationCallback {

    private static <E extends Environment<?, ?>> int trainEnvironment(EnvironmentExecutionSettings environmentExecutionSettings, @Nullable Future<E> environment, Consumer<@Nullable String> resultConsumer) {
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                resultConsumer.accept(Rlmc.getPythonEntrypoint().runKwargs(environment.get(), environmentExecutionSettings));
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Training environment had an error!", e);
            }
        }, "RLMC Training Thread").start();
        return Command.SINGLE_SUCCESS;
    }

    private static CommandNode<ServerCommandSource> withEnvironmentAndExecutionSettings(CommandNode<ServerCommandSource> base) {
        var training = literal("training")
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setTraining())
                .build();
        var evaluating = literal("evaluating")
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setEvaluating())
                .build();
        var episodes = literal("episodes")
                .build();
        var episodesArg = argument("episodes", IntegerArgumentType.integer(1))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setEpisodes(IntegerArgumentType.getInteger(context, "episodes")))
                .build();
        var algorithm = literal("algorithm")
                .build();
        var algorithmArg = argument("algorithm", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"PPO", "A2C"}, builder))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setAlgorithm(StringArgumentType.getString(context, "algorithm")))
                .build();
        var save = literal("save")
                .build();
        var savePath = argument("savePath", StringArgumentType.string())
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setSavePath(StringArgumentType.getString(context, "savePath")))
                .build();
        var load = literal("load")
                .build();
        var loadPath = argument("loadPath", StringArgumentType.string())
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setLoadPath(StringArgumentType.getString(context, "loadPath")))
                .build();
        var entCoef = literal("entCoef")
                .build();
        var entCoefArg = argument("entCoef", DoubleArgumentType.doubleArg(0))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setEntCoef(DoubleArgumentType.getDouble(context, "entCoef")))
                .build();
        var learningRate = literal("learningRate")
                .build();
        var learningRateArg = argument("learningRate", DoubleArgumentType.doubleArg(0))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setLearningRate(DoubleArgumentType.getDouble(context, "learningRate")))
                .build();
        var logPath = literal("log")
                .build();
        var logPathArg = argument("logPath", StringArgumentType.string())
                .build();
        var logNameArg = argument("logName", StringArgumentType.string())
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$setTensorboardLog(StringArgumentType.getString(context, "logPath"), StringArgumentType.getString(context, "logName")))
                .build();
        var gamma = literal("gamma")
                .build();
        var gammaArg = argument("gamma", DoubleArgumentType.doubleArg(0))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder)context.getSource()).rlmc$setGamma(DoubleArgumentType.getDouble(context, "gamma")))
                .build();
        var netArch = literal("netArch")
                .build();
        var netArchBuild = literal("build")
                .build();
        var layer = argument("layer", IntegerArgumentType.integer(0))
                .redirect(netArchBuild, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder)context.getSource()).rlmc$addNetLayer(IntegerArgumentType.getInteger(context, "layer")))
                .build();
        var netArchDone = literal("done")
                .redirect(base)
                .build();
        var netArchDefault = literal("default")
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder)context.getSource()).rlmc$clearNetArch())
                .build();
        var batchSize = literal("batchSize")
                .build();
        var batchSizeArg = argument("batchSize", IntegerArgumentType.integer(1))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder)context.getSource()).rlmc$setBatchSize(IntegerArgumentType.getInteger(context, "batchSize")))
                .build();
        var vfCoef = literal("vfCoef")
                .build();
        var vfCoefArg = argument("vfCoef", DoubleArgumentType.doubleArg(0))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentExecutionSettingsBuilder)context.getSource()).rlmc$setVfCoef(DoubleArgumentType.getDouble(context, "vfCoef")))
                .build();
        var monitor = literal("monitor")
                .build();
        var monitorArg = argument("monitor", BoolArgumentType.bool())
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentSettingsBuilder)context.getSource()).rlmc$setUseMonitor(BoolArgumentType.getBool(context, "monitor")))
                .build();
        var frameStack = literal("frameStack")
                .build();
        var frameStackRemove = literal("remove")
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentSettingsBuilder)context.getSource()).rlmc$removeFrameStack())
                .build();
        var frameStackArg = argument("frameStack", IntegerArgumentType.integer(1))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentSettingsBuilder)context.getSource()).rlmc$setFrameStack(IntegerArgumentType.getInteger(context, "frameStack")))
                .build();
        var timeLimit = literal("timeLimit")
                .build();
        var timeLimitRemove = literal("remove")
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentSettingsBuilder)context.getSource()).rlmc$removeTimeLimit())
                .build();
        var timeLimitArg = argument("timeLimit", IntegerArgumentType.integer(0))
                .redirect(base, context -> (ServerCommandSource) ((EnvironmentSettingsBuilder)context.getSource()).rlmc$setTimeLimit(IntegerArgumentType.getInteger(context, "timeLimit")))
                .build();
        var in = literal("in")
                .build();
        //@formatter:off
        // spotless:off
        base.addChild(training);
        base.addChild(evaluating);
        base.addChild(episodes);
            episodes.addChild(episodesArg);
        base.addChild(algorithm);
            algorithm.addChild(algorithmArg);
        base.addChild(save);
            save.addChild(savePath);
        base.addChild(load);
            load.addChild(loadPath);
        base.addChild(entCoef);
            entCoef.addChild(entCoefArg);
        base.addChild(learningRate);
            learningRate.addChild(learningRateArg);
        base.addChild(logPath);
            logPath.addChild(logPathArg);
                logPathArg.addChild(logNameArg);
        base.addChild(gamma);
            gamma.addChild(gammaArg);
        base.addChild(netArch);
            netArch.addChild(netArchBuild);
                netArchBuild.addChild(layer);
                netArchBuild.addChild(netArchDone);
            netArch.addChild(netArchDefault);
        base.addChild(batchSize);
            batchSize.addChild(batchSizeArg);
        base.addChild(vfCoef);
            vfCoef.addChild(vfCoefArg);
        base.addChild(monitor);
            monitor.addChild(monitorArg);
        base.addChild(frameStack);
            frameStack.addChild(frameStackArg);
            frameStack.addChild(frameStackRemove);
        base.addChild(timeLimit);
            timeLimit.addChild(timeLimitArg);
            timeLimit.addChild(timeLimitRemove);
        base.addChild(in);
        // spotless:on
        //@formatter:on
        return in;
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment registrationEnvironment) {
        var environment = literal("environment")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var fightEnemy = literal("enemy")
                .build();
        var fightEnemyAgent = argument("agent", StringArgumentType.word())
                .build();
        var fightEnemyType = argument("entityType", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                .executes(context -> {
                    //noinspection unchecked It's a command, let it fail
                    EntityType<? extends MobEntity> entityType = Objects.requireNonNull((EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey()));
                    ServerCommandSource source = context.getSource();
                    @Nullable Future<FightEnemyEnvironment> environment1 = FightEnemyEnvironment.makeAndConnect(((EnvironmentSettingsBuilder) source).rlmc$buildEnvironmentSettings(), StringArgumentType.getString(context, "agent"), source.getServer(), entityType, null);
                    return trainEnvironment(((EnvironmentExecutionSettingsBuilder) source).rlmc$build(), Objects.requireNonNull(environment1), (message) -> {
                        if (message != null) {
                            source.sendFeedback(() -> Text.literal(message), false);
                        }
                    });
                })
                .build();
        var fightEnemyStructure = argument("structure", RegistryKeyArgumentType.registryKey(RegistryKeys.STRUCTURE))
                .executes(context -> {
                    //noinspection unchecked It's a command, let it fail
                    EntityType<? extends MobEntity> entityType = Objects.requireNonNull((EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey()));
                    ServerCommandSource source = context.getSource();
                    //noinspection OptionalGetWithoutIsPresent
                    @Nullable Future<FightEnemyEnvironment> environment1 = FightEnemyEnvironment.makeAndConnect(((EnvironmentSettingsBuilder) source).rlmc$buildEnvironmentSettings(), StringArgumentType.getString(context, "agent"), source.getServer(), entityType, RegistryKeyArgumentType.getStructureEntry(context, "structure").getKey().get().getValue());
                    return trainEnvironment(((EnvironmentExecutionSettingsBuilder) source).rlmc$build(), Objects.requireNonNull(environment1), (message) -> {
                        if (message != null) {
                            source.sendFeedback(() -> Text.literal(message), false);
                        }
                    });
                })
                .build();
        var goNorth = literal("goNorth")
                .build();
        var goNorthAgent = argument("agent", StringArgumentType.word())
                .executes((context) -> {
                    ServerCommandSource source = context.getSource();
                    MinecraftServer server = source.getServer();
                    String name = StringArgumentType.getString(context, "agent");
                    @Nullable Future<GoNorthEnvironment> environment1 = GoNorthEnvironment.makeAndConnect(((EnvironmentSettingsBuilder) source).rlmc$buildEnvironmentSettings(), name, server);
                    return trainEnvironment(((EnvironmentExecutionSettingsBuilder) source).rlmc$build(), environment1, (message) -> {
                        if (message != null) {
                            source.sendFeedback(() -> Text.literal(message), false);
                        }
                    });
                })
                .build();


        // spotless:off
        //@formatter:off
        // /environment <environment> <environment-specific settings> <mode> <mode-specific settings>
        var settings = withEnvironmentAndExecutionSettings(environment);
            settings.addChild(fightEnemy);
                fightEnemy.addChild(fightEnemyAgent);
                    fightEnemyAgent.addChild(fightEnemyType);
                        fightEnemyAgent.addChild(fightEnemyStructure);
            settings.addChild(goNorth);
                goNorth.addChild(goNorthAgent);
        //@formatter:on
        // spotless:on

        dispatcher.getRoot().addChild(environment);
    }

}
