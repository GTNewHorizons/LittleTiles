package com.creativemd.littletiles.client.util3d;

import java.util.concurrent.TimeUnit;

public final class ProgressReporter {

    private final String name;
    private final long seed;
    private final int total;
    private final long start = System.nanoTime();
    private long lastReport;

    public ProgressReporter(String name, long seed, int total) {
        this.name = name;
        this.seed = seed;
        this.total = total;
        report(0);
    }

    public void update(int completed) {
        long now = System.nanoTime();
        if (completed == total || now - lastReport >= TimeUnit.SECONDS.toNanos(5)) {
            report(completed);
        }
    }

    private void report(int completed) {
        System.out.printf("%s: %d%% (%d/%d), seed=%d%n", name, completed * 100 / total, completed, total, seed);
        System.out.flush();
        lastReport = System.nanoTime();
        if (completed == total) {
            System.out
                    .printf("%s: took %d ms, seed=%d%n", name, TimeUnit.NANOSECONDS.toMillis(lastReport - start), seed);
            System.out.flush();
        }
    }
}
