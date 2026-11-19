package com.creativemd.littletiles.common.placement;

import java.util.ArrayList;

import net.minecraft.util.ChunkCoordinates;

/** The tiles a {@link LittleTilePlacementPlan} places into one block. */
final class PlacementEntry extends BaseEntry {

    final ArrayList<Fragment> fragments;

    PlacementEntry(ChunkCoordinates coord, ArrayList<Fragment> fragments) {
        super(coord);
        this.fragments = fragments;
    }
}
