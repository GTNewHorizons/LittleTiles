package com.creativemd.littletiles.client.util3d;

import static com.creativemd.littletiles.client.util3d.MeshGeometry.area;
import static com.creativemd.littletiles.client.util3d.MeshGeometry.volume;
import static com.creativemd.littletiles.client.util3d.Message.check;

import java.util.Random;

import com.creativemd.littletiles.client.util3d.BlockSpace.Split;
import com.creativemd.littletiles.client.util3d.MeshGeometry.FaceFilter;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Checks on tiles and their meshes. Failures describe what went wrong; the sweep adds the trial's inputs.
 */
public final class MeshAssertions {

    /**
     * Area in square pixels that faces matching exactly may differ by after culling. Rounding points to a render grid
     * of 2^-17 blocks moves them by up to 1.1E-4 pixels, so faces meeting along an edge may overlap or leave a gap up
     * to twice that wide along it: about 500 pixels of edges at most for a tile filling the block. Far below anything
     * visible.
     */
    public static final double ROUNDING_AREA = 0.1;

    /**
     * Volume in cubic pixels that meshes of the same solid may differ by. Rounding points to a render grid of 2^-17
     * blocks moves them by up to 1.1E-4 pixels, so it changes the volume by up to that times the area of the faces:
     * about 4500 square pixels at most for a tile filling the block and its two halves.
     */
    public static final double ROUNDING_VOLUME = 0.5;

    private MeshAssertions() {}

    public static void assertNotEmpty(Tile tile) {
        check(!tile.isEmpty(), Message.of("no faces: ", tile));
    }

    public static void assertEmpty(Tile tile) {
        check(tile.isEmpty(), Message.of(tile.mesh().getTriangles().size(), " faces left: ", tile));
    }

    public static void assertHasTiltedFaces(Tile tile) {
        check(tile.hasTiltedFaces(), Message.of("no tilted faces: ", tile));
    }

    /** Checks that the game lets {@code candidate} be placed into a block holding {@code placed}. */
    public static void assertFit(Tile placed, Tile candidate) {
        assertFitsExactlyWhen(true, placed, candidate);
    }

    /** Checks that the game refuses to place {@code candidate} into a block holding {@code placed}. */
    public static void assertCollide(Tile placed, Tile candidate) {
        assertFitsExactlyWhen(false, placed, candidate);
    }

    /** Checks that the tiles collide, whichever of them is placed first. */
    public static void assertCollideEitherWay(Tile first, Tile second) {
        assertCollide(first, second);
        assertCollide(second, first);
    }

    public static void assertFitsExactlyWhen(boolean fits, Tile placed, Tile candidate) {
        check(
                placed.leavesRoomFor(candidate) == fits,
                Message.of(fits ? "false collision" : "missed collision", " placing ", candidate, " beside ", placed));
    }

    /** Checks that every edge is shared by two faces walking it in opposite directions. */
    public static void assertClosed(Tile tile) {
        String openEdge = MeshGeometry.findOpenEdge(tile.mesh());
        check(openEdge == null, Message.of(openEdge, " in ", tile));
    }

    /**
     * Checks that the tile's mesh is closed and encloses positive volume, no more than its box, and that clipping its
     * shape to both halves of the box, split at a random pixel plane, gives closed meshes adding up to it, up to what
     * rounding to the render grid changes. This catches meshes turned inside out and pieces the cuts lose or double.
     * The box must be longer than a pixel along some axis.
     */
    public static void assertClosedAndVolumesAddUp(Random random, Tile tile) {
        assertClosed(tile);
        double whole = volume(tile.mesh());
        check(
                whole > 0 && whole <= BlockSpace.volume(tile.box) + ROUNDING_VOLUME,
                Message.of("volume=", whole, " of ", tile));

        Split split = Split.random(random, tile.box);
        double halves = 0;
        for (LittleTileBox halfBox : split.halves()) {
            Tile half = tile.clippedTo(halfBox);
            // The shape can miss one half
            if (half.isEmpty()) continue;

            assertClosed(half);
            double halfVolume = volume(half.mesh());
            check(halfVolume > 0, Message.of("volume=", halfVolume, " of ", half));
            halves += halfVolume;
        }
        check(
                Math.abs(halves - whole) <= ROUNDING_VOLUME,
                Message.of("halves volume=", halves, " whole volume=", whole, " ", split));
    }

    /** Checks that culling against {@code occluder} hides the faces, up to slivers rounding leaves. */
    public static void assertHidden(String faces, Mesh3d mesh, Mesh3d occluder) {
        double visible = area(MeshGeometry.visiblePart(mesh, occluder));
        check(visible <= ROUNDING_AREA, Message.of(faces, " visible=", visible));
    }

    /** Checks that culling against {@code occluder} leaves {@code expected} square pixels visible, up to slivers. */
    public static void assertVisibleArea(String faces, Mesh3d mesh, Mesh3d occluder, double expected) {
        double visible = area(MeshGeometry.visiblePart(mesh, occluder));
        check(
                Math.abs(visible - expected) <= ROUNDING_AREA,
                Message.of(faces, " visible=", visible, " expected=", expected));
    }

    /** Checks that culling against {@code occluder} takes nothing but slivers away from the faces. */
    public static void assertStayVisible(String faces, Mesh3d mesh, Mesh3d occluder) {
        double culled = MeshGeometry.culledArea(mesh, occluder);
        check(culled <= ROUNDING_AREA, Message.of(faces, " culled=", culled));
    }

    /**
     * Checks that culling the tile against the occluder leaves {@code tiltedVisible} square pixels of its tilted faces
     * visible, up to slivers, and keeps its flat faces, which only lie next to faces of the occluder.
     */
    public static void assertCulled(Tile tile, Tile occluder, double tiltedVisible) {
        assertVisibleArea("tilted faces", FaceFilter.TILTED.of(tile.mesh()), occluder.mesh(), tiltedVisible);
        assertStayVisible("flat faces", FaceFilter.FLAT.of(tile.mesh()), occluder.mesh());
    }

    /** As {@link #assertCulled}, for a complement covering all tilted faces of the tile. */
    public static void assertCulledByComplement(Tile tile, Tile complement) {
        assertCulled(tile, complement, 0);
    }

    /**
     * Checks that two meshes meeting in a shared face, picked by {@code shared}, each cover the same area there and
     * hide the other's faces there up to slivers, and keep their other faces, which only meet along the seam.
     */
    public static void assertOnlySharedFacesHideEachOther(Mesh3d first, Mesh3d second, FaceFilter shared) {
        Mesh3d firstShared = shared.of(first), secondShared = shared.of(second);
        double firstArea = area(firstShared), secondArea = area(secondShared);
        check(firstArea > 0 && secondArea > 0, Message.of("no shared faces: ", firstArea, " and ", secondArea));
        check(
                Math.abs(firstArea - secondArea) <= ROUNDING_AREA,
                Message.of("shared face areas differ: ", firstArea, " and ", secondArea));
        assertHidden("first shared faces", firstShared, second);
        assertHidden("second shared faces", secondShared, first);
        assertStayVisible("first faces off the shared face", shared.notOf(first), second);
        assertStayVisible("second faces off the shared face", shared.notOf(second), first);
    }
}
