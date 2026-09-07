package com.creativemd.creativecore.client.rendering;

import java.util.Collections;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * A clipper that reports every side as fully hidden, so a block's renderer emits no geometry at all. Used to run a
 * block's own rendering handler purely for the tessellator state it sets up (brightness in particular), without
 * drawing anything.
 */
public class EmptyFaceClipper implements IFaceClipper {

    public static final EmptyFaceClipper INSTANCE = new EmptyFaceClipper();

    private EmptyFaceClipper() {}

    @Override
    public List<FacePiece> getFacePieces(ForgeDirection side) {
        return Collections.emptyList();
    }
}
