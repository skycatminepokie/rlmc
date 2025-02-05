package com.skycatdev.rlmc.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skycatdev.rlmc.PythonEntrypoint;
import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.environment.SkybridgeEnvironment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.*;

public class CommandManager implements CommandRegistrationCallback {
	public static final DynamicCommandExceptionType NOT_ONE_AGENT_EXCEPTION_TYPE =
			new DynamicCommandExceptionType(numAgents -> () -> String.format("Expected exactly one player, got %s", numAgents));

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

	private static int makeSkybridgeEnvironment(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		Collection<GameProfile> agents = GameProfileArgumentType.getProfileArgument(context, "agent");
		if (agents.size() != 1) {
			throw NOT_ONE_AGENT_EXCEPTION_TYPE.create(agents.size());
		}
		Optional<GameProfile> optGameProfile = agents.stream().findFirst();
		assert optGameProfile.isPresent(); // TODO: Exception instead of assert

		ServerPlayerEntity agent = Objects.requireNonNull(context.getSource().getServer().getPlayerManager().getPlayer(optGameProfile.get().getId())); // TODO: Exception instead of requireNonNull
		BlockPos pos = BlockPosArgumentType.getLoadedBlockPos(context, "pos");
		int distance = IntegerArgumentType.getInteger(context, "distance");
		int historyLength = IntegerArgumentType.getInteger(context, "historyLength");

		SkybridgeEnvironment environment = new SkybridgeEnvironment(agent, pos, distance, historyLength);
		Rlmc.ENVIRONMENTS.add(environment);
		Rlmc.getPythonEntrypoint().connectEnvironment("skybridge", environment);
		return 0;
	}


}
