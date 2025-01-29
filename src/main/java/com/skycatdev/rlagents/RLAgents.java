package com.skycatdev.rlagents;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

import java.util.concurrent.Future;

public class RLAgents implements ModInitializer {
	public static final String MOD_ID = "rl-agents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GatewayServer GATEWAY_SERVER = new GatewayServer();

	static {
		GATEWAY_SERVER.start();
	}

	@Override
	public void onInitialize() {

	}
}