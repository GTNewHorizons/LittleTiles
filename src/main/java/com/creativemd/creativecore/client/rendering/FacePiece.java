package com.creativemd.creativecore.client.rendering;

/**
 * An axis aligned part of a block face, in the two axes spanning that face's plane.
 * <p>
 * Coordinates are in sixteenths of a block, so all clipping is exact integer math.
 */
public class FacePiece {

    public final int minPlaneX;
    public final int minPlaneY;
    public final int maxPlaneX;
    public final int maxPlaneY;

    public FacePiece(int minPlaneX, int maxPlaneX, int minPlaneY, int maxPlaneY) {
        this.minPlaneX = minPlaneX;
        this.maxPlaneX = maxPlaneX;
        this.minPlaneY = minPlaneY;
        this.maxPlaneY = maxPlaneY;
    }
}
