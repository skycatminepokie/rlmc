/* Licensed MIT 2025 */
package com.skycatdev.rlmc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skycatdev.rlmc.network.DrawVectorPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class DebugDrawing implements WorldRenderEvents.DebugRender {

    public void addVector(DebugVector debugVector) {

    }

    public DebugVector addVector(Vector3f origin, Vector3f vector, DrawVectorPayload.Mode mode) {
        DebugVector debugVector = new DebugVector(origin, vector, mode, 0xFFFFFFFF);
        addVector(debugVector);
        return debugVector;
    }

    @Override
    public void beforeDebugRender(WorldRenderContext context) {
        // "Yeah, don't try to update that code to 1.21.5 / Not until fapi rendering has something built in" - Qendolin
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        RenderSystem.applyModelViewMatrix();
        Camera camera = context.camera();
        Vec3d pos = camera.getPos();
        buffer.vertex((float) (0 - pos.getX()), (float) (0 - pos.getY()), (float) (0 - pos.getZ())).color(0xFFFFFFFF);
        buffer.vertex((float) (10 - pos.getX()), (float) (10 - pos.getY()), (float) (10 - pos.getZ())).color(0xFFFFFFFF);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void addVectorVertices(DebugVector vector, Vec3d cameraPos, VertexConsumer vertexConsumer) {
        switch (vector.mode) {
            case POSITION -> {
                vertexConsumer.vertex((float) (vector.origin.x - cameraPos.x), (float) (vector.origin.y - cameraPos.y), (float) (vector.origin.z - cameraPos.z))
                        .color(vector.color);
                vertexConsumer.vertex((float) (vector.vector.x - cameraPos.x), (float) (vector.vector.y - cameraPos.y), (float) (vector.vector.z - cameraPos.z))
                        .color(vector.color);
            }
            case ROTATION -> {
                // TODO
            }
        }
    }

    public record DebugVector(Vector3f origin, Vector3f vector, DrawVectorPayload.Mode mode, int color) {
    }
}
