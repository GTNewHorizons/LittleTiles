package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Wraps an IBlockAccess so that the block of a little tile appears to be placed as a full block at its position.
 */
public class LittleTileBlockAccess implements IBlockAccess {

    private final IBlockAccess world;
    private final Block block;
    private final int meta;
    private final int posX;
    private final int posY;
    private final int posZ;

    public LittleTileBlockAccess(IBlockAccess world, Block block, int meta, int posX, int posY, int posZ) {
        this.world = world;
        this.block = block;
        this.meta = meta;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    private boolean isTilePos(int x, int y, int z) {
        return x == posX && y == posY && z == posZ;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (isTilePos(x, y, z)) return block;
        return Blocks.air;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int side) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, side);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (isTilePos(x, y, z)) return meta;
        return 0;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return world.isAirBlock(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return world.getBiomeGenForCoords(x, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getHeight() {
        return world.getHeight();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return world.extendedLevelsInChunkCache();
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return world.isSideSolid(x, y, z, side, _default);
    }
}
