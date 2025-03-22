/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.skycatdev.rlmc.network.DrawVectorPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class RlmcClient implements ClientModInitializer {

	public static final DebugDrawing DEBUG_DRAWING = new DebugDrawing();

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DrawVectorPayload.PACKET_ID, (payload, context) -> {
			DEBUG_DRAWING.addVector(new DebugDrawing.DebugVector(payload.origin(), payload.vector(), payload.mode(), 0xFFFFFFFF));
		});
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(DEBUG_DRAWING);
	}
}