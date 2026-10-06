package com.creativemd.littletiles.client.util3d;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.Assume;

/**
 * Runs an opt-in sweep: many trials drawn from a fresh seed. A failure names the seed, the trial and the inputs the
 * trial recorded.
 */
public final class Sweep {

    public static final int DEFAULT_TRIALS = 50_000;

    private static final String RUN_ALL_OPTION = "TEST_PERMUTATIONS_ALL";

    private static boolean meshesLoaded;

    private Sweep() {}

    /** @param option the environment variable listed in build.gradle that enables the sweep */
    public static void run(String option, String name, Consumer<Trial> trial) {
        run(option, name, DEFAULT_TRIALS, trial);
    }

    public static void run(String option, String name, int trials, Consumer<Trial> trial) {
        requireOption(option);
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);
        ProgressReporter progress = new ProgressReporter(name, seed, trials);
        for (int index = 0; index < trials; index++) {
            Trial current = new Trial(random, index);
            try {
                trial.accept(current);
            } catch (AssertionError | RuntimeException failure) {
                String dump = current.dumpFailure(name, seed, failure);
                throw new AssertionError(
                        "seed=" + seed
                                + " trial="
                                + index
                                + current.describeInputs()
                                + ": "
                                + failure.getMessage()
                                + dump,
                        failure);
            }
            progress.update(index + 1);
        }
    }

    private static synchronized void requireOption(String option) {
        Assume.assumeTrue(
                "Set " + option + "=1 or " + RUN_ALL_OPTION + "=1 to run this sweep",
                "1".equals(System.getenv(option)) || "1".equals(System.getenv(RUN_ALL_OPTION)));
        if (!meshesLoaded) {
            Mesh3dUtil.initializeMeshes();
            meshesLoaded = true;
        }
    }

    /** One trial of a sweep. */
    public static final class Trial {

        private final Random random;
        private final int index;
        private final List<Object> inputs = new ArrayList<>();
        private final List<MeshFailureObj.Group> meshes = new ArrayList<>();

        private Trial(Random random, int index) {
            this.random = random;
            this.index = index;
        }

        public Random random() {
            return random;
        }

        /** Which of {@code cases} to take, in turn from trial to trial, so the sweep covers each of them equally. */
        public int inTurn(int cases) {
            return index % cases;
        }

        /** Every orientation in turn, see {@link #inTurn}. */
        public int orientationInTurn() {
            return inTurn(Shapes.ORIENTATIONS);
        }

        /** Records an input for the failure message, described only if the trial fails, and returns it. */
        public <T> T input(String name, T value) {
            inputs.add(" " + name + "=");
            inputs.add(value);
            if (value instanceof Tile) show(name, (Tile) value);
            return value;
        }

        /** Writes the tile out with the trial's meshes when it fails, replacing a mesh of the same name. */
        public void show(String name, Tile tile) {
            show(new MeshFailureObj.Group(name, tile));
        }

        /** Writes the mesh out with the trial's meshes when it fails, replacing a mesh of the same name. */
        public void show(String name, Mesh3d mesh) {
            show(new MeshFailureObj.Group(name, () -> mesh));
        }

        private void show(MeshFailureObj.Group group) {
            meshes.removeIf(other -> other.name.equals(group.name));
            meshes.add(group);
        }

        /** Writes the meshes the failure shows, or else the trial's, to an OBJ, and says where. */
        private String dumpFailure(String sweep, long seed, Throwable failure) {
            List<MeshFailureObj.Group> toDump = meshes;
            if (failure instanceof MeshAssertions.MeshFailure) toDump = ((MeshAssertions.MeshFailure) failure).meshes;
            if (toDump.isEmpty()) return "";
            try {
                return "\nOBJ: " + MeshFailureObj.dump(sweep, seed, index, toDump);
            } catch (IOException dumpFailure) {
                return "\nOBJ dump failed: " + dumpFailure;
            }
        }

        private String describeInputs() {
            return Message.of(inputs.toArray()).toString();
        }
    }
}
