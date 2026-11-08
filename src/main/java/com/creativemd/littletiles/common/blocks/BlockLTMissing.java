package com.creativemd.littletiles.common.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Stands in for the block of a tile whose block is no longer registered (e.g. the mod was removed). The tile keeps the
 * original block name and saves it back unchanged, so the tile comes back once the block exists again.
 */
public class BlockLTMissing extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon missingIcon;

    public BlockLTMissing(Material material) {
        super(material);
        setHardness(1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister registry) {
        // the atlas returns its "missingno" sprite for unknown names; the instance survives restitching
        missingIcon = ((TextureMap) registry).getAtlasSprite("missingno");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return missingIcon;
    }
}
