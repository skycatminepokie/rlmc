/* Licensed MIT 2025 */
package com.skycatdev.rlagents;

import com.skycatdev.rlagents.environment.Environment;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

public class RLAgents implements ModInitializer {
	public static final String MOD_ID = "rl-agents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GatewayServer GATEWAY_SERVER = new GatewayServer();
	public static final List<Environment<?, ?, ?, ?>> ENVIRONMENTS = new ArrayList<>();

	static {
		GATEWAY_SERVER.start();
	}

	@Override
	public void onInitialize() {

	}
}