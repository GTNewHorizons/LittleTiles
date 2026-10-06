package com.creativemd.littletiles.client.util3d;

import java.util.Arrays;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;

/**
 * A failure message put together only when it is shown. The sweeps describe every input, and formatting the JOML
 * vectors in them costs far more than the checks that almost always pass.
 */
public final class Message {

    private final Object[] parts;

    private Message(Object[] parts) {
        this.parts = parts;
    }

    /**
     * The parts are joined by their {@code toString}, arrays by {@link Arrays#toString}, shapes by
     * {@link Shapes#describe}.
     */
    public static Message of(Object... parts) {
        return new Message(parts);
    }

    /** Throws an {@link AssertionError} with the message unless the condition holds. */
    public static void check(boolean condition, Object message) {
        if (!condition) throw new AssertionError(String.valueOf(message));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            builder.append(describe(part));
        }
        return builder.toString();
    }

    private static String describe(Object part) {
        if (part instanceof Object[]) return Arrays.toString((Object[]) part);
        if (part instanceof LittleTileCutoutInfo) return Shapes.describe((LittleTileCutoutInfo) part);
        return String.valueOf(part);
    }
}
