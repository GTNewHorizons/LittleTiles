package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.LittleTiles;

public class LittleTileBlock extends LittleTile {

    public Block block;
    public int meta;

    public LittleTileBlock(Block block, int meta) {
        super();
        this.block = block;
        this.meta = meta;
    }

    public LittleTileBlock(Block block) {
        this(block, 0);
    }

    public LittleTileBlock() {
        super();
    }

    @Override
    public void saveTileExtra(NBTTagCompound nbt) {

        nbt.setString("block", Block.blockRegistry.getNameForObject(block));
        nbt.setInteger("meta", meta);
    }

    @Override
    public void loadTileExtra(NBTTagCompound nbt) {
        block = Block.getBlockFromName(nbt.getString("block"));
        meta = nbt.getInteger("meta");
        if (block == null || block instanceof BlockAir)
            throw new IllegalArgumentException("Invalid block name! name=" + nbt.getString("block"));
    }

    @Override
    public void copyExtra(LittleTile tile) {
        if (tile instanceof LittleTileBlock) {
            LittleTileBlock thisTile = (LittleTileBlock) tile;
            thisTile.block = block;
            thisTile.meta = meta;
        }
    }

    @Override
    public ItemStack getDrop() {
        ItemStack stack = new ItemStack(LittleTiles.blockTile);
        stack.stackTagCompound = new NBTTagCompound();
        saveTile(stack.stackTagCompound);
        boundingBox.getSize().writeToNBT("size", stack.stackTagCompound);
        return stack;
    }

    @Override
    public ArrayList<LittleTilesCubeObject> getRenderingCubes() {
        ArrayList<LittleTilesCubeObject> cubes = new ArrayList<>();
        if (boundingBox != null) {
            LittleTilesCubeObject cube = boundingBox.getCube();
            cube.block = block;
            cube.meta = meta;
            cube.cutoutInfo = this.getCutoutInfo();
            cubes.add(cube);
        }
        return cubes;
    }

    @Override
    public void onPlaced(EntityPlayer player, ItemStack stack) {
        super.onPlaced(player, stack);
        try {
            block.onBlockPlacedBy(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord, player, stack);
            block.onPostBlockPlaced(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord, meta);
        } catch (Exception ignored) {

        }
    }

    @Override
    public SoundType getSound() {
        return block.stepSound;
    }

    @Override
    public IIcon getIcon(int side) {
        return block.getIcon(side, meta);
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        block.randomDisplayTick(world, x, y, z, random);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float moveX,
            float moveY, float moveZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, moveX, moveY, moveZ)) return true;
        return block.onBlockActivated(world, x, y, z, player, side, moveX, moveY, moveZ);
    }

    @Override
    public void place() {
        super.place();
        block.onBlockAdded(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return block.getLightValue();
    }

    @Override
    public double getEnchantPowerBonus(World world, int x, int y, int z) {
        return block.getEnchantPowerBonus(world, x, y, z);
    }

    @Override
    public boolean canBeCombined(LittleTile tile) {
        if (super.canBeCombined(tile) && tile instanceof LittleTileBlock) {
            return block == ((LittleTileBlock) tile).block && meta == ((LittleTileBlock) tile).meta;
        }
        return false;
    }

    @Override
    protected boolean canSawResize(ForgeDirection direction, EntityPlayer player) {
        return true;
    }

}
