package com.creativemd.littletiles.client.util3d;

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
                throw new AssertionError(
                        "seed=" + seed + " trial=" + index + current.describeInputs() + ": " + failure.getMessage(),
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

        private Trial(Random random, int index) {
            this.random = random;
            this.index = index;
        }

        public Random random() {
            return random;
        }

        /** Every orientation in turn from trial to trial, so the sweep covers each of them equally. */
        public int orientationInTurn() {
            return index % Shapes.ORIENTATIONS;
        }

        /** Records an input for the failure message, described only if the trial fails, and returns it. */
        public <T> T input(String name, T value) {
            inputs.add(" " + name + "=");
            inputs.add(value);
            return value;
        }

        private String describeInputs() {
            return Message.of(inputs.toArray()).toString();
        }
    }
}
