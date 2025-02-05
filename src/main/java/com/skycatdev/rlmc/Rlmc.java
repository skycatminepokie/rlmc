/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.skycatdev.rlmc.command.CommandManager;
import com.skycatdev.rlmc.environment.Environment;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

public class Rlmc implements ModInitializer {
	public static final String MOD_ID = "rl-agents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GatewayServer GATEWAY_SERVER = new GatewayServer();
	public static final List<Environment<?, ?>> ENVIRONMENTS = new ArrayList<>();

	static {
		GATEWAY_SERVER.start(true);
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(new CommandManager());
	}

	public static PythonEntrypoint getPythonEntrypoint() {
		return (PythonEntrypoint) GATEWAY_SERVER.getPythonServerEntryPoint(new Class[]{PythonEntrypoint.class});
	}

	public static List<Environment<?,?>> getEnvironments() {
		return ENVIRONMENTS;
	}

}