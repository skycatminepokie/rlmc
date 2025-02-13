/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import static net.minecraft.server.command.CommandManager.*;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.AgentCandidate;
import com.skycatdev.rlmc.environment.FightSkeletonEnvironment;
import com.skycatdev.rlmc.environment.SkybridgeEnvironment;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CommandManager implements CommandRegistrationCallback {

	@Override
	public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment registrationEnvironment) {
		var environment = literal("environment")
				.requires(source -> source.hasPermissionLevel(4))
				.build();
		var create = literal("create")
				.requires(source -> source.hasPermissionLevel(4))
				.build();

		// spotless:off
		environment.addChild(create);
			appendSkybridge(create);
			appendFightSkeleton(create);
		// spotless:on

		dispatcher.getRoot().addChild(environment);
	}

	private void appendSkybridge(LiteralCommandNode<ServerCommandSource> create) {
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
		create.addChild(skybridge);
			skybridge.addChild(agent);
				agent.addChild(pos);
					pos.addChild(distance);
						distance.addChild(historyLength);
		// spotless:on
	}

	private void appendFightSkeleton(LiteralCommandNode<ServerCommandSource> create) {
		var fightSkeleton = literal("skeleton")
				.requires(source -> source.hasPermissionLevel(4))
				.build();
		var agent = argument("agent", StringArgumentType.word())
				.requires(source -> source.hasPermissionLevel(4))
				.build();
		var agentPos = argument("agent_pos", BlockPosArgumentType.blockPos())
				.requires(source -> source.hasPermissionLevel(4))
				.build();
		var skeletonPos = argument("skeleton_pos", BlockPosArgumentType.blockPos())
				.requires(source -> source.hasPermissionLevel(4))
				.build();
		var historyLength = argument("history_length", IntegerArgumentType.integer(0))
				.requires(source -> source.hasPermissionLevel(4))
				.executes(CommandManager::makeFightSkeletonEnvironment)
				.build();
		// spotless:off
		create.addChild(fightSkeleton);
			fightSkeleton.addChild(agent);
				agent.addChild(agentPos);
					agentPos.addChild(skeletonPos);
						skeletonPos.addChild(historyLength);
		// spotless:on
	}

	private static int makeFightSkeletonEnvironment(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "agent");
		BlockPos agentPos = BlockPosArgumentType.getLoadedBlockPos(context, context.getSource().getWorld(), "agent_pos");
		BlockPos skeletonPos = BlockPosArgumentType.getLoadedBlockPos(context, context.getSource().getWorld(), "skeleton_pos");
		int historyLength = IntegerArgumentType.getInteger(context, "history_length");
        @Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(name, context.getSource().getServer(), Vec3d.of(agentPos), context.getSource().getWorld().getRegistryKey());
		if (agentFuture != null) {
			agentFuture.thenAcceptAsync((agent) -> {
				FightSkeletonEnvironment environment = new FightSkeletonEnvironment(agent, context.getSource().getWorld(), agentPos, skeletonPos, historyLength);
				Rlmc.getEnvironments().add(environment);
				Rlmc.getPythonEntrypoint().connectEnvironment("fight_skeleton", environment);
				new Thread(() -> Rlmc.getPythonEntrypoint().train(environment), "RLMC Skeleton Training Thread");
			});
			return Command.SINGLE_SUCCESS;
		}
		return -1;
	}

    private static @Nullable CompletableFuture<ServerPlayerEntity> createPlayerAgent(String name, MinecraftServer server, Vec3d pos, RegistryKey<World> world) {
        return createPlayerAgent(name, server, pos, 0.0, 0.0, world, GameMode.SURVIVAL, false);
    }

    private static @Nullable CompletableFuture<ServerPlayerEntity> createPlayerAgent(String name, MinecraftServer server, Vec3d pos, double yaw, double pitch, RegistryKey<World> world, GameMode gamemode, boolean flying) {
		if (EntityPlayerMPFake.createFake(name, server, pos, yaw, pitch, world, gamemode, flying)) {
			CompletableFuture<ServerPlayerEntity> future = new CompletableFuture<>();
			new Thread(() -> {
					@Nullable ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
					while (player == null) {
						Thread.yield();
						player = server.getPlayerManager().getPlayer(name);
					}
                	((AgentCandidate) player).rlmc$markAsAgent();
					future.complete(player);
			}, "RLMC Create Player Agent Thread").start();
			return future;
		} else {
			return null;
		}
	}

	private static int makeSkybridgeEnvironment(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
		int distance = IntegerArgumentType.getInteger(context, "distance");
		int historyLength = IntegerArgumentType.getInteger(context, "historyLength");
		@Nullable CompletableFuture<ServerPlayerEntity> agentFuture = createPlayerAgent(StringArgumentType.getString(context, "agent"), context.getSource().getServer(), Vec3d.of(pos), context.getSource().getWorld().getRegistryKey());
		if (agentFuture != null) {
			agentFuture.thenAcceptAsync((agent) -> {
				SkybridgeEnvironment environment = new SkybridgeEnvironment(agent, pos, distance, historyLength, 3, 3);
				Rlmc.getEnvironments().add(environment);
				Rlmc.getPythonEntrypoint().connectEnvironment("skybridge", environment);
				new Thread(() -> Rlmc.getPythonEntrypoint().train(environment), "RLMC Skybridge Training Thread").start();
			});
			return Command.SINGLE_SUCCESS;
		}
		return -1;
	}


}
