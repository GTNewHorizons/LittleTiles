package com.creativemd.littletiles.common.placement;

import com.creativemd.littletiles.utils.PreviewTile;
import net.minecraft.util.ChunkCoordinates;

import java.util.ArrayList;

class PlacementEntry {

    public final ChunkCoordinates coord;
    public final ArrayList<PreviewTile> placeTiles;

    public PlacementEntry(ChunkCoordinates coord, ArrayList<PreviewTile> placeTiles) {
        this.coord = coord;
        this.placeTiles = placeTiles;
    }
}
