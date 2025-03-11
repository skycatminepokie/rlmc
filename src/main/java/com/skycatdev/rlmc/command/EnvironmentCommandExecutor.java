/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

@FunctionalInterface
public interface EnvironmentCommandExecutor {
    int execute(CommandContext<ServerCommandSource> context, EnvironmentExecutionSettings environmentExecutionSettings) throws CommandSyntaxException;
}
