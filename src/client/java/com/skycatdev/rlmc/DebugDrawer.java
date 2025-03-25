/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skycatdev.rlmc.network.DebugVector;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class DebugDrawer implements WorldRenderEvents.DebugRender, ClientTickEvents.EndTick {
    protected final Queue<DebugVector> vectors = new ConcurrentLinkedQueue<>();

    public void addVector(DebugVector debugVector) {
        vectors.add(debugVector);
    }

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        // "Yeah, don't try to update that code to 1.21.5 / Not until fapi rendering has something built in" - Qendolin
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        RenderSystem.applyModelViewMatrix();
        Camera camera = context.camera();
        Vec3d pos = camera.getPos();
        vectors.iterator().forEachRemaining((vec) -> addVectorVertices(vec, pos, buffer));
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void addVectorVertices(DebugVector vector, Vec3d cameraPos, VertexConsumer vertexConsumer) {
        switch (vector.mode()) {
            case POSITION -> {
                vertexConsumer.vertex((float) (vector.origin().x - cameraPos.x), (float) (vector.origin().y - cameraPos.y), (float) (vector.origin().z - cameraPos.z))
                        .color(vector.color());
                vertexConsumer.vertex((float) (vector.vector().x - cameraPos.x), (float) (vector.vector().y - cameraPos.y), (float) (vector.vector().z - cameraPos.z))
                        .color(vector.color());
            }
            case ROTATION -> {
                vertexConsumer.vertex((float) (vector.origin().x - cameraPos.x), (float) (vector.origin().y - cameraPos.y), (float) (vector.origin().z - cameraPos.z))
                        .color(vector.color());
                // (x,y,z) = (-sin(yaw)cos(pitch), sin(pitch), -cos(yaw)cos(pitch)) https://math.stackexchange.com/questions/2618527/converting-from-yaw-pitch-roll-to-vector
                double yaw = Math.toRadians(vector.vector().x);
                double pitch = Math.toRadians(vector.vector().y);
                float dist = vector.vector().z;
                Vec3d vecFromZero = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch));
                vecFromZero = vecFromZero.normalize().multiply(dist);
                Vector3f vec = vector.origin().add(vecFromZero.toVector3f());
                vertexConsumer.vertex((float) (vec.x - cameraPos.x), (float) (vec.y - cameraPos.y), (float) (vec.z - cameraPos.z))
                        .color(vector.color());
            }
        }
    }

    @Override
    public void onEndTick(MinecraftClient minecraftClient) {
        for (DebugVector vector : vectors) {
            vector.setLifetime(vector.lifetime() - 1);
        }
    }
}
