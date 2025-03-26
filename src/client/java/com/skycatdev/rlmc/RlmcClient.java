/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.skycatdev.rlmc.network.DrawVectorPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class RlmcClient implements ClientModInitializer {

	public static final DebugDrawer DEBUG_DRAWER = new DebugDrawer();

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DrawVectorPayload.PACKET_ID, (payload, context) -> {
			DEBUG_DRAWER.addVector(payload.vector());
		});
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(DEBUG_DRAWER);
		ClientTickEvents.END_CLIENT_TICK.register(DEBUG_DRAWER);
	}
}