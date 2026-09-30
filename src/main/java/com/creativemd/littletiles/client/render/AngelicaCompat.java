package com.creativemd.littletiles.client.render;

import net.coderbot.iris.Iris;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;

import com.gtnewhorizons.angelica.rendering.StateAwareTessellator;

public class AngelicaCompat {

    public void setShaderMaterialOverride(Block block, int meta) {
        Iris.setShaderMaterialOverride(block, meta);
    }

    public void resetShaderMaterialOverride() {
        Iris.resetShaderMaterialOverride();
    }

    public void beginAmbientOcclusion(Tessellator tessellator) {
        if (tessellator instanceof StateAwareTessellator) {
            ((StateAwareTessellator) tessellator).angelica$setAppliedAo(true);
        }
    }

    public void endAmbientOcclusion(Tessellator tessellator) {
        if (tessellator instanceof StateAwareTessellator) {
            ((StateAwareTessellator) tessellator).angelica$setAppliedAo(false);
        }
    }
}
