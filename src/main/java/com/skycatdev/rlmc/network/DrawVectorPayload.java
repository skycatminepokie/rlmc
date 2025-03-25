/* Licensed MIT 2025 */
package com.skycatdev.rlmc.network;

import com.skycatdev.rlmc.Rlmc;
import com.skycatdev.rlmc.network.drawing.DebugVector;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DrawVectorPayload(DebugVector vector) implements CustomPayload {
    public static final Identifier ID = Identifier.of(Rlmc.MOD_ID, "draw_vector");
    public static final CustomPayload.Id<DrawVectorPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<ByteBuf, DrawVectorPayload> CODEC = DebugVector.PACKET_CODEC.xmap(DrawVectorPayload::new, DrawVectorPayload::vector);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
