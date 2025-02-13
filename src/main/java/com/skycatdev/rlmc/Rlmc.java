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
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

public class Rlmc implements ModInitializer {
	public static final String MOD_ID = "rl-agents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final GatewayServer GATEWAY_SERVER = new GatewayServer();
	private static final List<Environment<?, ?>> ENVIRONMENTS = new ArrayList<>();

	static {
		new Thread(() -> GATEWAY_SERVER.start(false), "RLMC Python Gateway Server Thread").start();

	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(new CommandManager());
		GATEWAY_SERVER.addListener(new GatewayServerListener() {
			@Override
			public void connectionError(Exception e) {
				LOGGER.debug("Py4J Connection Error. Printing stack trace.", e);
			}

			@Override
			public void connectionStarted(Py4JServerConnection gatewayConnection) {
				LOGGER.debug("Py4J connection started");
			}

			@Override
			public void connectionStopped(Py4JServerConnection gatewayConnection) {
				LOGGER.debug("Py4J connection stopped");
			}

			@Override
			public void serverError(Exception e) {
				LOGGER.debug("Py4J Server Error. Print stack trace.", e);
			}

			@Override
			public void serverPostShutdown() {
				LOGGER.debug("Py4J server post shutdown");
			}

			@Override
			public void serverPreShutdown() {
				LOGGER.debug("Py4J server pre shutdown");
			}

			@Override
			public void serverStarted() {
				LOGGER.debug("Py4J server started");
			}

			@Override
			public void serverStopped() {
				LOGGER.debug("Py4J server stopped");
			}
		});
	}

	public static PythonEntrypoint getPythonEntrypoint() {
		return (PythonEntrypoint) GATEWAY_SERVER.getPythonServerEntryPoint(new Class[]{PythonEntrypoint.class});
	}

	public static List<Environment<?,?>> getEnvironments() {
		return ENVIRONMENTS;
	}

}