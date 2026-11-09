package com.creativemd.littletiles.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The facades of one block. */
public final class LittleTileFacades {

    public final LittleTileFacade[] tiles;

    /** Answers of {@link #fills}, valid as long as this object since the tiles never change. Used from any thread. */
    private final Map<LittleTileFacade, Boolean> fills = new ConcurrentHashMap<>();

    public LittleTileFacades(LittleTileFacade[] tiles) {
        this.tiles = tiles;
    }

    /** Whether tiles of the area's block and meta fill its whole box. */
    public boolean fills(LittleTileFacade area) {
        return fills.computeIfAbsent(area, key -> LittleTileFacade.fills(tiles, key.block, key.meta, key.box));
    }
}
