package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;

public class OrientationMapper {

    /** The 24 proper rotations plus their 24 mirrored counterparts, covering every flipped/rotated cube orientation. */
    private static final int NUM_ORIENTATIONS = 48;
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
                    matrix.rotateZ((float) Math.PI / 2 * zRot);
                    snapToCubeOrientation(matrix);

                    String key = toKey(matrix);
                    if (!LOOKUP.containsKey(key)) {
                        ORIENTATIONS.add(matrix);
                        LOOKUP.put(key, id++);
                    }
                }
            }
        }

        // Mirror every proper rotation found above to also cover the 24 improper (reflected) orientations, giving the
        // full 48-element cube symmetry group needed to represent flipped meshes.
        Matrix3f mirror = new Matrix3f(-1, 0, 0, 0, 1, 0, 0, 0, 1);
        int properCount = ORIENTATIONS.size();
        for (int i = 0; i < properCount; i++) {
            Matrix3f mirrored = new Matrix3f(mirror).mul(ORIENTATIONS.get(i));
            snapToCubeOrientation(mirrored);

            String key = toKey(mirrored);
            if (!LOOKUP.containsKey(key)) {
                ORIENTATIONS.add(mirrored);
                LOOKUP.put(key, id++);
            }
        }

        if (ORIENTATIONS.size() != NUM_ORIENTATIONS)
            throw new RuntimeException("Expected " + NUM_ORIENTATIONS + " orientations, got " + ORIENTATIONS.size());
    }

    /**
     * These matrices only describe quarter-turn cube orientations, so every component is exactly -1, 0 or 1.
     * Trigonometric construction leaves small float errors which otherwise make touching mesh faces appear to be on
     * different planes.
     */
    private static void snapToCubeOrientation(Matrix3f matrix) {
        matrix.m00 = Math.round(matrix.m00);
        matrix.m01 = Math.round(matrix.m01);
        matrix.m02 = Math.round(matrix.m02);
        matrix.m10 = Math.round(matrix.m10);
        matrix.m11 = Math.round(matrix.m11);
        matrix.m12 = Math.round(matrix.m12);
        matrix.m20 = Math.round(matrix.m20);
        matrix.m21 = Math.round(matrix.m21);
        matrix.m22 = Math.round(matrix.m22);
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
