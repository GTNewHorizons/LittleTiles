package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.client.rendering.FacePiece;
import com.creativemd.creativecore.client.rendering.IFaceClipper;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

/**
 * Collects covered areas of one box's faces and works out which parts of a face to draw.
 * <p>
 * All coordinates are in sixteenths of a block, matching the grid the boxes are placed on, so the clipping is exact
 * integer math.
 */
public class FaceClipper implements IFaceClipper {

    /** The box's own bounds, so a face only has to be identified by its side. */
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    @SuppressWarnings("unchecked")
    private final List<FacePiece>[] covers = new List[6];

    public FaceClipper(LittleTilesCubeObject cube) {
        this.minX = cube.gridMinX;
        this.minY = cube.gridMinY;
        this.minZ = cube.gridMinZ;
        this.maxX = cube.gridMaxX;
        this.maxY = cube.gridMaxY;
        this.maxZ = cube.gridMaxZ;
    }

    /** Marks the given area of a side as hidden, in the axes spanning that side's plane. */
    public void cover(ForgeDirection side, int minPlaneX, int minPlaneY, int maxPlaneX, int maxPlaneY) {
        List<FacePiece> sideCovers = covers[side.ordinal()];
        if (sideCovers == null) {
            sideCovers = new ArrayList<>();
            covers[side.ordinal()] = sideCovers;
        }
        sideCovers.add(new FacePiece(minPlaneX, maxPlaneX, minPlaneY, maxPlaneY));
    }

    @Override
    public List<FacePiece> getFacePieces(ForgeDirection side) {
        List<FacePiece> sideCovers = covers[side.ordinal()];
        if (sideCovers == null) {
            return null;
        }

        List<FacePiece> remaining = new ArrayList<>();
        remaining.add(face(side));
        boolean clipped = false;
        for (FacePiece cover : sideCovers) {
            List<FacePiece> newFaces = new ArrayList<>();
            for (FacePiece piece : remaining) {
                clipped |= cutFace(newFaces, piece, cover);
            }
            remaining = newFaces;
            if (remaining.isEmpty()) {
                break;
            }
        }
        return clipped ? remaining : null;
    }

    /** The whole area of one side, in that side's plane axes. */
    private FacePiece face(ForgeDirection side) {
        return switch (side) {
            case DOWN, UP -> new FacePiece(minX, maxX, minZ, maxZ);
            case NORTH, SOUTH -> new FacePiece(minX, maxX, minY, maxY);
            default -> new FacePiece(minZ, maxZ, minY, maxY);
        };
    }

    /**
     * Splits {@code piece} into up to four pieces around the part {@code cover} hides, appending them to
     * {@code result}. Returns whether anything was actually cut away.
     */
    private static boolean cutFace(List<FacePiece> result, FacePiece piece, FacePiece cover) {
        // Clamp the cover to the part of the piece it actually overlaps.
        int minPlaneX = Math.max(piece.minPlaneX, cover.minPlaneX);
        int minPlaneY = Math.max(piece.minPlaneY, cover.minPlaneY);
        int maxPlaneX = Math.min(piece.maxPlaneX, cover.maxPlaneX);
        int maxPlaneY = Math.min(piece.maxPlaneY, cover.maxPlaneY);

        // No positive-area overlap, so the cover does not hide any part of this piece.
        if (minPlaneX >= maxPlaneX || minPlaneY >= maxPlaneY) {
            result.add(piece);
            return false;
        }

        // Visible strip before the cover on the plane's Y axis.
        if (piece.minPlaneY < minPlaneY) {
            result.add(new FacePiece(piece.minPlaneX, piece.maxPlaneX, piece.minPlaneY, minPlaneY));
        }
        // Visible strip after the cover on the plane's Y axis.
        if (piece.maxPlaneY > maxPlaneY) {
            result.add(new FacePiece(piece.minPlaneX, piece.maxPlaneX, maxPlaneY, piece.maxPlaneY));
        }
        // Visible strip before the cover on the plane's X axis, limited to the cover's Y span.
        if (piece.minPlaneX < minPlaneX) {
            result.add(new FacePiece(piece.minPlaneX, minPlaneX, minPlaneY, maxPlaneY));
        }
        // Visible strip after the cover on the plane's X axis, limited to the cover's Y span.
        if (piece.maxPlaneX > maxPlaneX) {
            result.add(new FacePiece(maxPlaneX, piece.maxPlaneX, minPlaneY, maxPlaneY));
        }
        return true;
    }
}
