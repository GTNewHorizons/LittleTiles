package com.creativemd.littletiles.client.render;

import net.coderbot.iris.Iris;
import net.minecraft.block.Block;

public class AngelicaCompat {

    public void setShaderMaterialOverride(Block block, int meta) {
        Iris.setShaderMaterialOverride(block, meta);
    }

    public void resetShaderMaterialOverride() {
        Iris.resetShaderMaterialOverride();
    }
}
