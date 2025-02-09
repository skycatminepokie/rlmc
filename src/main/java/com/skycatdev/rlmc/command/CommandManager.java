/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import static net.minecraft.server.command.CommandManager.*;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.FightSkeletonEnvironment;
import com.skycatdev.rlmc.environment.SkybridgeEnvironment;
import java.util.Collection;
import java.util.Optional;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CommandManager implements CommandRegistrationCallback {
	public static final DynamicCommandExceptionType NOT_ONE_AGENT_EXCEPTION_TYPE =
			new DynamicCommandExceptionType(numAgents -> () -> String.format("Expected exactly one player, got %s", numAgents));
	public static final DynamicCommandExceptionType AGENT_NOT_FOUND_EXCEPTION_TYPE =
			new DynamicCommandExceptionType(agentName -> () -> String.format("The agent %s is not online!", agentName));

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
		var agent = argument("agent", GameProfileArgumentType.gameProfile())
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
		ServerPlayerEntity agent = getOneAgent(context, "agent");
		BlockPos agentPos = BlockPosArgumentType.getLoadedBlockPos(context, context.getSource().getWorld(), "agent_pos");
		BlockPos skeletonPos = BlockPosArgumentType.getLoadedBlockPos(context, context.getSource().getWorld(), "skeleton_pos");
		int historyLength = IntegerArgumentType.getInteger(context, "history_length");
		FightSkeletonEnvironment environment = new FightSkeletonEnvironment(agent, agentPos, skeletonPos, historyLength);
		Rlmc.getPythonEntrypoint().connectEnvironment("fight_skeleton", environment);
		new Thread(() -> Rlmc.getPythonEntrypoint().train(environment), "Skeleton fight training thread").start();

		return Command.SINGLE_SUCCESS;
	}

	private static ServerPlayerEntity getOneAgent(CommandContext<ServerCommandSource> context, String agentParameterName) throws CommandSyntaxException {
		Collection<GameProfile> agents = GameProfileArgumentType.getProfileArgument(context, agentParameterName);
		if (agents.size() != 1) {
			throw NOT_ONE_AGENT_EXCEPTION_TYPE.create(agents.size());
		}
		Optional<GameProfile> optGameProfile = agents.stream().findFirst();
		// assert optGameProfile.isPresent();

		@Nullable ServerPlayerEntity agent = context.getSource().getServer().getPlayerManager().getPlayer(optGameProfile.get().getId());
		if (agent == null) {
			throw AGENT_NOT_FOUND_EXCEPTION_TYPE.create(optGameProfile.get().getName());
		}
		return agent;
	}

	private static int makeSkybridgeEnvironment(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity agent = getOneAgent(context, "agent");
		BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
		int distance = IntegerArgumentType.getInteger(context, "distance");
		int historyLength = IntegerArgumentType.getInteger(context, "historyLength");

		SkybridgeEnvironment environment = new SkybridgeEnvironment(agent, pos, distance, historyLength, 3, 3);
		Rlmc.getEnvironments().add(environment);
		Rlmc.getPythonEntrypoint().connectEnvironment("skybridge", environment);
		new Thread(() -> Rlmc.getPythonEntrypoint().train(environment)).start();
		return Command.SINGLE_SUCCESS;
	}


}
