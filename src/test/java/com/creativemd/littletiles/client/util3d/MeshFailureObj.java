package com.creativemd.littletiles.client.util3d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import com.creativemd.creativecore.lib.Vector3d;

/** Writes the meshes involved in a failed sweep to one OBJ, with one group per mesh. */
final class MeshFailureObj {

    /** Where the OBJ files go, set by the build; the working directory's build folder otherwise. */
    private static final String DIRECTORY_PROPERTY = "littletiles.meshFailureDir";

    /** A named mesh, built only when written, since building it may be what failed. */
    static final class Group {

        final String name;
        private final Supplier<Mesh3d> mesh;

        Group(String name, Supplier<Mesh3d> mesh) {
            this.name = name;
            this.mesh = mesh;
        }

        Group(String name, Tile tile) {
            this(name, tile::mesh);
        }
    }

    private MeshFailureObj() {}

    static Path dump(String sweep, long seed, int trial, List<Group> groups) throws IOException {
        Path directory = Paths.get(System.getProperty(DIRECTORY_PROPERTY, "build/test-mesh-dumps")).toAbsolutePath();
        Files.createDirectories(directory);
        String prefix = label(sweep) + "-seed-" + seed + "-trial-" + trial + "-";
        Path file = Files.createTempFile(directory, prefix, ".obj");
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            line(writer, "# Failed mesh sweep: " + sweep + ", seed=" + seed + ", trial=" + trial);
            int nextVertex = 1;
            for (Group group : groups) {
                Mesh3d mesh;
                try {
                    mesh = group.mesh.get();
                } catch (RuntimeException | AssertionError failure) {
                    // Keep the meshes that do build
                    line(writer, "# " + group.name + " not built: " + failure);
                    continue;
                }

                line(writer, "o " + label(group.name));
                for (Triangle3d triangle : mesh.getTriangles()) {
                    vertex(writer, triangle.getP1());
                    vertex(writer, triangle.getP2());
                    vertex(writer, triangle.getP3());
                    line(writer, "f " + nextVertex + " " + (nextVertex + 1) + " " + (nextVertex + 2));
                    nextVertex += 3;
                }
            }
        }
        return file;
    }

    private static String label(String name) {
        return name.replaceAll("[^A-Za-z0-9_-]+", "_");
    }

    private static void vertex(BufferedWriter writer, Vector3d point) throws IOException {
        line(writer, "v " + point.x + " " + point.y + " " + point.z);
    }

    private static void line(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }
}
