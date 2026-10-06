package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.Message.check;

/**
 * Checks on tiles and their meshes. Failures describe what went wrong; the sweep adds the trial's inputs.
 */
public final class MeshAssertions {

    private MeshAssertions() {}

    public static void assertNotEmpty(Tile tile) {
        check(!tile.isEmpty(), Message.of("no faces: ", tile));
    }

    /** Checks that the game lets {@code candidate} be placed into a block holding {@code placed}. */
    public static void assertFit(Tile placed, Tile candidate) {
        assertFitsExactlyWhen(true, placed, candidate);
    }

    public static void assertFitsExactlyWhen(boolean fits, Tile placed, Tile candidate) {
        check(
                placed.leavesRoomFor(candidate) == fits,
                Message.of(fits ? "false collision" : "missed collision", " placing ", candidate, " beside ", placed));
    }
}
