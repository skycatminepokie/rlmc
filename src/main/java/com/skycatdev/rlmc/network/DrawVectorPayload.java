/* Licensed MIT 2025 */
package com.skycatdev.rlmc.network;

import com.skycatdev.rlmc.Rlmc;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.ValueLists;
import org.joml.Vector3f;

public record DrawVectorPayload(Vector3f origin, Vector3f vector, Mode mode) implements CustomPayload {
    public static final Identifier ID = Identifier.of(Rlmc.MOD_ID, "draw_vector");
    public static final CustomPayload.Id<DrawVectorPayload> PACKET_ID = new CustomPayload.Id<>(ID);
    public static final PacketCodec<RegistryByteBuf, DrawVectorPayload> CODEC = PacketCodec.tuple(PacketCodecs.VECTOR3F, DrawVectorPayload::origin, PacketCodecs.VECTOR3F, DrawVectorPayload::vector, Mode.PACKET_CODEC, DrawVectorPayload::mode, DrawVectorPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }


    public enum Mode {
        POSITION(0),
        ROTATION(1);

        public static final IntFunction<Mode> INDEX_TO_VALUE = ValueLists.createIdToValueFunction(Mode::getIndex, values(), ValueLists.OutOfBoundsHandling.ZERO);
        public static final PacketCodec<ByteBuf, Mode> PACKET_CODEC = PacketCodecs.indexed(Mode.INDEX_TO_VALUE, Mode::getIndex);
        private final int index;

        Mode(int index) {
            this.index = index;
        }

        private int getIndex() {
            return index;
        }
    }
}
