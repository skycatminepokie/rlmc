/* Licensed MIT 2025 */
package com.skycatdev.rlmc.network;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.IntFunction;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.function.ValueLists;
import org.joml.Vector3f;

public class DebugVector {
    public static final PacketCodec<ByteBuf, DebugVector> PACKET_CODEC = PacketCodec.tuple(PacketCodecs.VECTOR3F,
            DebugVector::origin,
            PacketCodecs.VECTOR3F,
            DebugVector::vector,
            Mode.PACKET_CODEC,
            DebugVector::mode,
            PacketCodecs.INTEGER,
            DebugVector::color,
            PacketCodecs.INTEGER,
            DebugVector::lifetime,
            DebugVector::new);
    protected final Vector3f origin;
    protected final Vector3f vector;
    protected final Mode mode;
    protected final int color;
    protected int lifetime;

    public DebugVector(Vector3f origin, Vector3f vector, Mode mode, int color, int lifetime) {
        this.origin = origin;
        this.vector = vector;
        this.mode = mode;
        this.color = color;
        this.lifetime = lifetime;
    }

    public int color() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DebugVector) obj;
        return Objects.equals(this.origin, that.origin) &&
               Objects.equals(this.vector, that.vector) &&
               Objects.equals(this.mode, that.mode) &&
               this.color == that.color &&
               this.lifetime == that.lifetime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, vector, mode, color, lifetime);
    }

    public int lifetime() {
        return lifetime;
    }

    public Mode mode() {
        return mode;
    }

    public Vector3f origin() {
        return origin;
    }

    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return "DebugVector[" +
               "origin=" + origin + ", " +
               "vector=" + vector + ", " +
               "mode=" + mode + ", " +
               "color=" + color + ", " +
               "lifetime=" + lifetime + ']';
    }

    public Vector3f vector() {
        return vector;
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
