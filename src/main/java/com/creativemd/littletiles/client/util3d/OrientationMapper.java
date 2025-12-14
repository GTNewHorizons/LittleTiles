package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;

public class OrientationMapper {

    private static final int NUM_ORIENTATIONS = 24;
    private static final List<Matrix3f> ORIENTATIONS = new ArrayList<>();
    private static final Map<String, Integer> LOOKUP = new HashMap<>();

    static {
        generateOrientations();
    }

    private static void generateOrientations() {
        int id = 0;
        for (int xRot = 0; xRot < 4; xRot++) {
            for (int yRot = 0; yRot < 4; yRot++) {
                for (int zRot = 0; zRot < 4; zRot++) {
                    Matrix3f matrix = new Matrix3f();
                    matrix.rotateX((float) Math.PI / 2 * xRot);
                    matrix.rotateY((float) Math.PI / 2 * yRot);
                    matrix.rotateZ((float) Math.PI / 2 * zRot);;

                    String key = toKey(matrix);
                    if (!LOOKUP.containsKey(key)) {
                        ORIENTATIONS.add(matrix);
                        LOOKUP.put(key, id++);
                    }
                }
            }
        }

        if (ORIENTATIONS.size() != NUM_ORIENTATIONS)
            throw new RuntimeException("Expected 24 orientations, got " + ORIENTATIONS.size());
    }

    /** Convert matrix to key string */
    private static String toKey(Matrix3f m) {
        return String.format(
                "%d%d%d%d%d%d%d%d%d",
                Math.round(m.m00),
                Math.round(m.m01),
                Math.round(m.m02),
                Math.round(m.m10),
                Math.round(m.m11),
                Math.round(m.m12),
                Math.round(m.m20),
                Math.round(m.m21),
                Math.round(m.m22));
    }

    /** Convert matrix to orientation ID */
    public static int toId(Matrix3f m) {
        String key = toKey(m);
        Integer id = LOOKUP.get(key);
        if (id == null) throw new IllegalArgumentException("Matrix not a valid orientation: " + m);
        return id;
    }

    /** Get orientation matrix from ID */
    public static Matrix3f fromId(int id) {
        if (id < 0 || id >= NUM_ORIENTATIONS) throw new IllegalArgumentException("Invalid orientation id: " + id);
        return new Matrix3f(ORIENTATIONS.get(id));
    }
}
