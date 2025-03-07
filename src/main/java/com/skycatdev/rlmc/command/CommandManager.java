/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.BasicPlayerEnvironment;
import com.skycatdev.rlmc.environment.FightEnemyEnvironment;
import com.skycatdev.rlmc.environment.SkybridgeEnvironment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
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

    private static int evaluateFightEnemyEnvironment(MinecraftServer server, String name, EntityType<? extends MobEntity> entityType, int episodes, String loadPath, ServerCommandSource source) {
        @Nullable Future<FightEnemyEnvironment> environment = FightEnemyEnvironment.makeAndConnect(name, server, entityType);
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                String evalResults = Rlmc.getPythonEntrypoint().evaluate(environment.get(), episodes, loadPath);
                source.sendFeedback(() -> Text.of(evalResults), false); // TODO: Translate
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Enemy evaluation environment had an error!", e);
            }
        }, "RLMC Enemy Evaluation Thread").start();
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

    private static int trainFightEnemyEnvironment(MinecraftServer server, String name, EntityType<? extends MobEntity> entityType, int episodes, String savePath, @Nullable String loadPath) throws CommandSyntaxException {
        @Nullable Future<FightEnemyEnvironment> environment = FightEnemyEnvironment.makeAndConnect(name, server, entityType);
        if (environment == null) {
            return -1;
        }
        new Thread(() -> {
            try {
                if (loadPath != null) {
                    Rlmc.getPythonEntrypoint().train(environment.get(), episodes, savePath, loadPath);
                } else {
                    Rlmc.getPythonEntrypoint().train(environment.get(), episodes, savePath);
                }
            } catch (InterruptedException | ExecutionException e) {
                Rlmc.LOGGER.error("Enemy training environment had an error!", e);
            }
        }, "RLMC Enemy Training Thread").start();
        return Command.SINGLE_SUCCESS;
    }

    private static int trainFightEnemyEnvironment(MinecraftServer server, String name, EntityType<? extends MobEntity> entityType, int episodes, String savePath) throws CommandSyntaxException {
        return trainFightEnemyEnvironment(server, name, entityType, episodes, savePath, null);
    }

    private void appendEvaluateFightEnemy(LiteralCommandNode<ServerCommandSource> evaluate, CommandRegistryAccess registryAccess) {
        var fightEnemy = literal("enemy")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var agent = argument("agent", StringArgumentType.word())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var entityType = argument("entityType", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var episodes = argument("episodes", IntegerArgumentType.integer(1))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        @SuppressWarnings("unchecked") // Just let it fail, it's a command
        var loadPath = argument("loadPath", StringArgumentType.string())
                .requires(source -> source.hasPermissionLevel(4))
                .executes((context) -> evaluateFightEnemyEnvironment(context.getSource().getServer(), StringArgumentType.getString(context, "agent"),
                        (EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey().getValue()),
                        IntegerArgumentType.getInteger(context, "episodes"),
                        StringArgumentType.getString(context, "loadPath"), context.getSource()))
                .build();
        // spotless:off
        //@formatter:off
        evaluate.addChild(fightEnemy);
            fightEnemy.addChild(agent);
                agent.addChild(entityType);
                    entityType.addChild(episodes);
                        episodes.addChild(loadPath);
        //@formatter:on
        // spotless:on
    }

    private void appendTrainFightEnemy(LiteralCommandNode<ServerCommandSource> create, CommandRegistryAccess registryAccess) {
        var fightEnemy = literal("enemy")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var agent = argument("agent", StringArgumentType.word())
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var entityType = argument("entityType", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var episodes = argument("episodes", IntegerArgumentType.integer(1))
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        @SuppressWarnings("unchecked") // Just let it fail, it's a command
        var savePath = argument("savePath", StringArgumentType.string())
                .requires(source -> source.hasPermissionLevel(4))
                .executes((context) -> trainFightEnemyEnvironment(context.getSource().getServer(),
                        StringArgumentType.getString(context, "agent"),
                        (EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey().getValue()),
                        IntegerArgumentType.getInteger(context, "episodes"),
                        StringArgumentType.getString(context, "savePath")))
                .build();
        @SuppressWarnings("unchecked") // Just let it fail, it's a command
        var loadPath = argument("loadPath", StringArgumentType.string())
                .requires(source -> source.hasPermissionLevel(4))
                .executes((context) -> trainFightEnemyEnvironment(context.getSource().getServer(), StringArgumentType.getString(context, "agent"),
                        (EntityType<? extends MobEntity>) Registries.ENTITY_TYPE.get(RegistryEntryReferenceArgumentType.getEntityType(context, "entityType").registryKey().getValue()),
                        IntegerArgumentType.getInteger(context, "episodes"),
                        StringArgumentType.getString(context, "savePath"), StringArgumentType.getString(context, "loadPath")))
                .build();
        // spotless:off
        //@formatter:off
        create.addChild(fightEnemy);
            fightEnemy.addChild(agent);
                agent.addChild(entityType);
                    entityType.addChild(episodes);
                        episodes.addChild(savePath);
                            savePath.addChild(loadPath);
        //@formatter:on
        // spotless:on
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
        var train = literal("train")
                .requires(source -> source.hasPermissionLevel(4))
                .build();
        var evaluate = literal("evaluate")
                .requires(source -> source.hasPermissionLevel(4))
                .build();

        // spotless:off
        //@formatter:off
        environment.addChild(train); // TODO: Use /environment <environment> <action> <arguments>
            appendTrainSkybridge(train);
            appendTrainFightEnemy(train, registryAccess);
        environment.addChild(evaluate);
            appendEvaluateFightEnemy(evaluate, registryAccess);
        //@formatter:on
        // spotless:on

        dispatcher.getRoot().addChild(environment);
    }


}
