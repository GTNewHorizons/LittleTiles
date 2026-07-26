package com.creativemd.creativecore.client.rendering;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

public interface IFaceClipper {

    /**
     * The visible parts of the given side, or null if the whole face should be rendered. An empty list means the face
     * is fully hidden.
     */
    List<FacePiece> getFacePieces(ForgeDirection side);
}
