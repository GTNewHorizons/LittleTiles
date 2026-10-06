package com.creativemd.littletiles.common.placement;

import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.utils.PreviewTile;

/** A preview split down to one block, with the shape it carries there. */
final class Fragment {

    final PreviewTile tile;
    /** Null where the fragment is a plain box. */
    final LittleTileCutoutInfo cutout;

    Fragment(PreviewTile tile, LittleTileCutoutInfo cutout) {
        this.tile = tile;
        this.cutout = cutout;
    }
}
