package com.creativemd.littletiles.common.utils;

import com.creativemd.littletiles.common.material.LittleMaterial;
import com.gtnewhorizons.postea.api.BlockNameAndMeta;
import com.gtnewhorizons.postea.api.BlockReplacementManager;

public class PosteaCompat {

    /** The material the block migrations registered to Postea turn the given block into, or null if none applies. */
    public LittleMaterial transformBlock(String blockName, int meta) {
        BlockNameAndMeta transformed = BlockReplacementManager.transformBlock(blockName, meta);
        return transformed != null ? new LittleMaterial(transformed.name, transformed.metadata) : null;
    }
}
