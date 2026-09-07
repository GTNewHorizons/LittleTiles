package com.creativemd.creativecore.client.block;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class IBlockAccessFake implements IBlockAccess {

    public IBlockAccess world;
    public Block block;
    public int meta;
    public TileEntity te;
    public int posX;
    public int posY;
    public int posZ;

    public IBlockAccessFake(IBlockAccess world) {
        this.world = world;
    }

    public IBlockAccessFake(IBlockAccess world, int x, int y, int z) {
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void setPos(int x, int y, int z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    private boolean isTilePos(int x, int y, int z) {
        return x == posX && y == posY && z == posZ;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (block != null && isTilePos(x, y, z)) return block;
        return world.getBlock(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        if (te != null && isTilePos(x, y, z)) return te;
        return world.getTileEntity(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int side) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, side);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        if (isTilePos(x, y, z)) return meta;
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return world.isBlockProvidingPowerTo(x, y, z, side);
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
