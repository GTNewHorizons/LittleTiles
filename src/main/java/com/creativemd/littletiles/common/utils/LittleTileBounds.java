package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
import com.creativemd.littletiles.utils.ShiftHandler;

/**
 * What placing tiles needs to know about them, see {@link PlacementHelper#getPlacementBox}. Small next to the tiles
 * themselves, so it can be kept for an item with many of them.
 */
public class LittleTileBounds {

    public final LittleTileSize size;
    /** The min corner of all tiles. */
    public final LittleTileVec min;
    /** The box of every tile, null for a tile without one. */
    public final List<LittleTileBox> boxes = new ArrayList<>();
    /** The shift handlers of a single tile, none for several. */
    public final List<ShiftHandler> shiftHandlers = new ArrayList<>();

    public LittleTileBounds(ArrayList<LittleTilePreview> tiles) {
        size = PlacementHelper.getSize(tiles);
        min = PlacementHelper.getInternalOffset(tiles);
        for (LittleTilePreview tile : tiles) boxes.add(tile != null && tile.box != null ? tile.box.copy() : null);
        if (tiles.size() == 1) shiftHandlers.addAll(tiles.get(0).shifthandlers);
    }
}
