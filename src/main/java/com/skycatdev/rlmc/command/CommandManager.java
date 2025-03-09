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
import com.skycatdev.rlmc.EnvironmentExecutionSettings;
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

    private static CommandNode<ServerCommandSource> makeTrainingSettingsNode(EnvironmentCommandExecutor executor) {
        var episodes = argument("episodes", IntegerArgumentType.integer(1))
                .build();
        var algorithm = argument("algorithm", StringArgumentType.word())
                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"PPO", "A2C"}, builder))
                .executes(context -> {
                    EnvironmentExecutionSettings ts = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    return executor.execute(context, ts);
                })
                .build();
        var savePath = argument("savePath", StringArgumentType.string())
                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"none"}, builder))
                .executes(context -> {
                    EnvironmentExecutionSettings ts = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    String savePathArg = StringArgumentType.getString(context, "savePath");
                    if (!savePathArg.equals("none")) {
                        ts.setSavePath(savePathArg);
                    }
                    return executor.execute(context, ts);
                })
                .build();
        var loadPath = argument("loadPath", StringArgumentType.string())
                .executes(context -> {
                    EnvironmentExecutionSettings ts = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    String savePathArg = StringArgumentType.getString(context, "savePath");
                    if (!savePathArg.equals("none")) {
                        ts.setSavePath(savePathArg);
                    }
                    ts.setLoadPath(StringArgumentType.getString(context, "loadPath"));
                    return executor.execute(context, ts);
                })
                .build();
        var entCoef = argument("entCoef", DoubleArgumentType.doubleArg(0))
                .executes(context -> {
                    EnvironmentExecutionSettings ts = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    String savePathArg = StringArgumentType.getString(context, "savePath");
                    if (!savePathArg.equals("none")) {
                        ts.setSavePath(savePathArg);
                    }
                    ts.setEntCoef(DoubleArgumentType.getDouble(context, "entCoef"));
                    return executor.execute(context, ts);
                })
                .build();
        var learningRate = argument("learningRate", DoubleArgumentType.doubleArg(0))
                .executes(context -> {
                    EnvironmentExecutionSettings ts = new EnvironmentExecutionSettings(IntegerArgumentType.getInteger(context, "episodes"), StringArgumentType.getString(context, "algorithm"));
                    String savePathArg = StringArgumentType.getString(context, "savePath");
                    if (!savePathArg.equals("none")) {
                        ts.setSavePath(savePathArg);
                    }
                    ts.setEntCoef(DoubleArgumentType.getDouble(context, "entCoef"));
                    ts.setLearningRate(DoubleArgumentType.getDouble(context, "learningRate"));
                    return executor.execute(context, ts);
                })
                .build();
        //@formatter:off
        // spotless:off
        episodes.addChild(algorithm);
            algorithm.addChild(savePath);
                savePath.addChild(loadPath);
                savePath.addChild(entCoef);
                    entCoef.addChild(learningRate);
        // spotless:on
        //@formatter:on
        return episodes;
    }

    private static <E extends Environment<?, ?>> int trainEnvironment(EnvironmentExecutionSettings environmentExecutionSettings, @Nullable Future<E> environment) {
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                Rlmc.getPythonEntrypoint().trainKwargs(environment.get(), environmentExecutionSettings);
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Training environment had an error!", e);
            }
        }, "RLMC Training Thread").start();
        return Command.SINGLE_SUCCESS;
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
        var goNorth = literal("go_north")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var goNorthAgent = argument("agent", StringArgumentType.word())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var goNorthSettings = makeTrainingSettingsNode((context, environmentExecutionSettings) -> {
            MinecraftServer server = context.getSource().getServer();
            String name = StringArgumentType.getString(context, "agent");
            @Nullable Future<GoNorthEnvironment> environment1 = GoNorthEnvironment.makeAndConnect(name, server);
            return trainEnvironment(environmentExecutionSettings, environment1);
        });
        var fightEnemy = literal("enemy")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var fightEnemyAgent = argument("agent", StringArgumentType.word())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var fightEnemyEntityType = argument("entityType", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var fightEnemyTrain = literal("train")
                .build();
        var fightEnemyTrainSettings = makeTrainingSettingsNode(((context, environmentExecutionSettings) -> {
            //noinspection unchecked It's a command, let it fail
            EntityType<? extends MobEntity> entityType = Objects.requireNonNull((EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey()));
            @Nullable Future<FightEnemyEnvironment> environment1 = FightEnemyEnvironment.makeAndConnect(StringArgumentType.getString(context, "agent"), context.getSource().getServer(), entityType);
            return trainEnvironment(environmentExecutionSettings, environment1);
        }));
        var fightEnemyEvaluate = literal("evaluate")
                .build();
        var fightEnemyEvaluateSettings = makeEvaluationSettingsNode((context, environmentExecutionSettings) -> {
            //noinspection unchecked It's a command, let it fail
            EntityType<? extends MobEntity> entityType = Objects.requireNonNull((EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey()));
            @Nullable Future<FightEnemyEnvironment> environment1 = FightEnemyEnvironment.makeAndConnect(StringArgumentType.getString(context, "agent"), context.getSource().getServer(), entityType);
            return evaluateEnvironment(context.getSource(), environment1, environmentExecutionSettings);
        });


        // spotless:off
        //@formatter:off
        // /environment <environment> <environment-specific settings> <mode> <mode-specific settings>
        appendTrainSkybridge(environment);
        environment.addChild(fightEnemy);
            fightEnemy.addChild(fightEnemyAgent);
                fightEnemyAgent.addChild(fightEnemyEntityType);
                    fightEnemyEntityType.addChild(fightEnemyTrain);
                        fightEnemyTrain.addChild(fightEnemyTrainSettings);
                    fightEnemyEntityType.addChild(fightEnemyEvaluate);
                        fightEnemyEvaluate.addChild(fightEnemyEvaluateSettings);
        environment.addChild(goNorth);
            goNorth.addChild(goNorthAgent);
                goNorthAgent.addChild(goNorthSettings);
        //@formatter:on
        // spotless:on

        dispatcher.getRoot().addChild(environment);
    }

}
