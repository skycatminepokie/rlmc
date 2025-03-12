/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class CommandManager implements CommandRegistrationCallback {

    private static <E extends Environment<?, ?>> int evaluateEnvironment(ServerCommandSource source, @Nullable Future<E> environment, EnvironmentExecutionSettings ees) {
        // TODO: Use ees instead of parsing it out
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                String evalResults = Rlmc.getPythonEntrypoint().evaluate(environment.get(), ees.getEpisodes(), Objects.requireNonNull(ees.getLoadPath()));
                source.sendFeedback(() -> Text.of(evalResults), false); // TODO: Translate
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Evaluation had an error!", e);
            }
        }, "RLMC Evaluation Thread").start();
        return Command.SINGLE_SUCCESS;
    }

    private static CommandNode<ServerCommandSource> makeEvaluationSettingsNode(EnvironmentCommandExecutor executor) {
        var episodes = argument("episodes", IntegerArgumentType.integer(1))
                .build();
        var algorithm = argument("algorithm", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"PPO", "A2C"}, builder))
                .build();
        var loadPath = argument("loadPath", StringArgumentType.string())
                .executes(context -> {
                    EnvironmentExecutionSettings ees = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    ees.setLoadPath(StringArgumentType.getString(context, "loadPath"));
                    return executor.execute(context, ees);
                })
                .build();
        // spotless:off
        //@formatter:off
        episodes.addChild(algorithm);
            algorithm.addChild(loadPath);
        //@formatter:on
        // spotless:on
        return episodes;
    }

    private static int makeSkybridgeEnvironment(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
        int distance = IntegerArgumentType.getInteger(context, "distance");
        int historyLength = IntegerArgumentType.getInteger(context, "historyLength");
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = BasicPlayerEnvironment.createPlayerAgent(StringArgumentType.getString(context, "agent"), context.getSource().getServer(), Vec3d.of(pos), context.getSource().getWorld().getRegistryKey());
        if (agentFuture != null) {
            agentFuture.thenAcceptAsync((agent) -> {
                SkybridgeEnvironment environment = new SkybridgeEnvironment(agent, pos, distance, 3, 3);
                Rlmc.addEnvironment(environment);
                Rlmc.getPythonEntrypoint().connectEnvironment("skybridge", environment);
                new Thread(() -> Rlmc.getPythonEntrypoint().train(environment, 1000, "trained_skybridge_agent"), "RLMC Skybridge Training Thread").start();
            }, (runnable) -> new Thread(runnable).start());
            return Command.SINGLE_SUCCESS;
        }
        return -1;
    }

    private static <E extends Environment<?, ?>> int trainEnvironment(EnvironmentExecutionSettings environmentExecutionSettings, @Nullable Future<E> environment) {
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                Rlmc.getPythonEntrypoint().runKwargs(environment.get(), environmentExecutionSettings);
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Training environment had an error!", e);
            }
        }, "RLMC Training Thread").start();
        return Command.SINGLE_SUCCESS;
    }

    private static CommandNode<ServerCommandSource> withExecutionSettings(CommandNode<ServerCommandSource> base) {
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
        base.addChild(in);
        // spotless:on
        //@formatter:on
        return in;
    }

    private void appendTrainSkybridge(LiteralCommandNode<ServerCommandSource> create) {
        var skybridge = literal("skybridge")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var agent = argument("agent", GameProfileArgumentType.gameProfile())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var pos = argument("pos", BlockPosArgumentType.blockPos())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var distance = argument("distance", IntegerArgumentType.integer(1))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var historyLength = argument("historyLength", IntegerArgumentType.integer(0))
                .requires(source -> source.hasPermissionLevel(4))
                .executes(CommandManager::makeSkybridgeEnvironment)
                .build();
        // spotless:off
        //@formatter:off
        create.addChild(skybridge);
            skybridge.addChild(agent);
                agent.addChild(pos);
                    pos.addChild(distance);
                        distance.addChild(historyLength);
        //@formatter:on
        // spotless:on
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
                    @Nullable Future<FightEnemyEnvironment> environment1 = FightEnemyEnvironment.makeAndConnect(StringArgumentType.getString(context, "agent"), context.getSource().getServer(), entityType);
                    return trainEnvironment(((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$build(), environment1);
                })
                .build();
        var goNorth = literal("goNorth")
                .build();
        var goNorthAgent = argument("agent", StringArgumentType.word())
                .executes((context) -> {
                    MinecraftServer server = context.getSource().getServer();
                    String name = StringArgumentType.getString(context, "agent");
                    @Nullable Future<GoNorthEnvironment> environment1 = GoNorthEnvironment.makeAndConnect(name, server);
                    return trainEnvironment(((EnvironmentExecutionSettingsBuilder) context.getSource()).rlmc$build(), environment1);
                })
                .build();


        // spotless:off
        //@formatter:off
        // /environment <environment> <environment-specific settings> <mode> <mode-specific settings>
        appendTrainSkybridge(environment);
            var settings = withExecutionSettings(environment);
                settings.addChild(fightEnemy);
                    fightEnemy.addChild(fightEnemyAgent);
                        fightEnemyAgent.addChild(fightEnemyType);
                settings.addChild(goNorth);
                    goNorth.addChild(goNorthAgent);
        //@formatter:on
        // spotless:on

        dispatcher.getRoot().addChild(environment);
    }

}
