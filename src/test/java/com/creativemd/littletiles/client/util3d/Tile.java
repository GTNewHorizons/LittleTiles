package com.creativemd.littletiles.client.util3d;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * A tile in the block: a box, clipping a shape or plain. Its mesh is built once, when first asked for.
 */
public final class Tile {

    public final LittleTileBox box;
    /** The shape as the game stores it, positioned relative to the box minimum, or null for a plain box. */
    public final LittleTileCutoutInfo shape;
    private Mesh3d mesh;

    private Tile(LittleTileBox box, LittleTileCutoutInfo shape) {
        this.box = box;
        this.shape = shape;
    }

    /** The shape, positioned relative to the block minimum, clipped to the whole block. */
    public static Tile of(LittleTileCutoutInfo shape) {
        return of(shape, BlockSpace.BLOCK);
    }

    /** The shape, positioned relative to the block minimum, clipped to the box. */
    public static Tile of(LittleTileCutoutInfo shape, LittleTileBox box) {
        LittleTileCutoutInfo stored = new LittleTileCutoutInfo(shape);
        stored.pos = new Vector3i(shape.pos).sub(BlockSpace.min(box));
        return new Tile(box, stored);
    }

    public static Tile plainBox(LittleTileBox box) {
        return new Tile(box, null);
    }

    public boolean isPlainBox() {
        return shape == null;
    }

    /** The shape, positioned relative to the block minimum. */
    public LittleTileCutoutInfo shapeInBlock() {
        LittleTileCutoutInfo inBlock = new LittleTileCutoutInfo(shape);
        inBlock.pos = new Vector3i(shape.pos).add(BlockSpace.min(box));
        return inBlock;
    }

    /** The same shape, where it is in the block, clipped to another box. */
    public Tile clippedTo(LittleTileBox other) {
        return of(shapeInBlock(), other);
    }

    /** The tile with the block turned about its center, see {@link Shapes#turned}. */
    public Tile turned(int turn) {
        return of(Shapes.turned(shapeInBlock(), turn), Shapes.turned(box, turn));
    }

    /** The faces as rendering builds them, for a plain box the faces culling builds. Callers must not change it. */
    public Mesh3d mesh() {
        if (mesh == null) mesh = buildMesh();
        return mesh;
    }

    /** Whether nothing of the shape lies in the box. */
    public boolean isEmpty() {
        return mesh().getTriangles().isEmpty();
    }

    /**
     * The tiles with shapes of {@code kind} filling the rest of this shape's bounds, in the same box, see
     * {@link Shapes#COMPLEMENTS_OF_UNTURNED}.
     */
    public List<Tile> complements(LittleTileShapeMode kind) {
        List<Tile> complements = new ArrayList<>();
        for (LittleTileCutoutInfo complement : Shapes.complementsOf(shapeInBlock(), kind)) {
            complements.add(of(complement, box));
        }
        return complements;
    }

    /** Whether the game lets {@code candidate} be placed into a block holding this tile. */
    public boolean leavesRoomFor(Tile candidate) {
        try {
            LittleTileBlock placed = new LittleTileBlock();
            placed.boundingBox = box.copy();
            placed.setCutoutInfo(shape);
            TileEntityLittleTiles block = new TileEntityLittleTiles();
            block.getTiles().add(placed);
            return block.isSpaceForLittleTile(candidate.box.copy(), candidate.shape);
        } catch (RuntimeException exception) {
            throw new AssertionError("placing " + candidate + " beside " + this, exception);
        }
    }

    private Mesh3d buildMesh() {
        if (isPlainBox()) return MeshGeometry.boxFaces(box);
        try {
            return Mesh3dUtil.meshFromTile(box, shape);
        } catch (RuntimeException exception) {
            throw new AssertionError("building " + this, exception);
        }
    }

    @Override
    public String toString() {
        if (isPlainBox()) return "box " + box;
        return Shapes.describe(shape) + " in " + box;
    }
}
