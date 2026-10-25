package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;
import java.util.Objects;
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
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

public class LittleTileBlock extends LittleTile {

    public Block block;
    public int meta;
    /** Name of the unregistered block this tile was saved with, {@link #block} is the placeholder then. **/
    public String missingBlockName;
    private static final ThreadLocal<IBlockAccessFake> blockAccessFakeThreadLocal = ThreadLocal
            .withInitial(IBlockAccessFake::new);

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

        nbt.setString(
                "block",
                missingBlockName != null ? missingBlockName : Block.blockRegistry.getNameForObject(block));
        nbt.setInteger("meta", meta);
    }

    @Override
    public void loadTileExtra(NBTTagCompound nbt) {
        setBlockByName(nbt.getString("block"));
        meta = nbt.getInteger("meta");
    }

    /** Unknown blocks become the missing placeholder, keeping the name so saving does not lose the tile. **/
    public void setBlockByName(String name) {
        block = Block.getBlockFromName(name);
        if (block == null || block instanceof BlockAir) {
            System.out.println("Found tile with missing block name=" + name + ", keeping it as missing tile");
            block = LittleTiles.missingBlock;
            missingBlockName = name;
        } else missingBlockName = null;
    }

    @Override
    public void copyExtra(LittleTile tile) {
        if (tile instanceof LittleTileBlock) {
            LittleTileBlock thisTile = (LittleTileBlock) tile;
            thisTile.block = block;
            thisTile.meta = meta;
            thisTile.missingBlockName = missingBlockName;
        }
    }

    @Override
    public ItemStack getDrop() {
        ItemStack stack = new ItemStack(LittleTiles.blockTile);
        stack.stackTagCompound = new NBTTagCompound();
        saveTileForItem(stack.stackTagCompound);
        boundingBox.getSize().writeToNBT("size", stack.stackTagCompound);
        return stack;
    }

    @Override
    public ArrayList<LittleTilesCubeObject> getRenderingCubes() {
        ArrayList<LittleTilesCubeObject> cubes = new ArrayList<>();
        LittleTileGeometryCache cache = getGeometryCache();
        // Captures this tile only. World rendering replaces this with the generation captured by the tile entity
        // together with list membership, before any cube (including an occluder) reads its geometry.
        long cutsGeneration = cache.captureCutsGeneration();
        // read once: chunk builds run this off-thread while the main thread can reassign the box
        LittleTileBox box = boundingBox;
        if (box != null) {
            LittleTilesCubeObject cube = box.getCube();
            cube.block = block;
            cube.meta = meta;
            cube.cutoutInfo = this.getCutoutInfo();
            cube.geometryCache = cache;
            cube.cutsGeneration = cutsGeneration;
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
        if (world != null) {
            // Pass the tile's own block and meta through so metadata/context-aware light
            // values are computed correctly.
            IBlockAccessFake blockAccessFake = blockAccessFakeThreadLocal.get();

            try {
                blockAccessFake.setWorld(world, x, y, z);
                blockAccessFake.setBlock(block, meta);
                return block.getLightValue(blockAccessFake, x, y, z);
            } finally {
                blockAccessFake.reset();
            }
        }
        return block.getLightValue();
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction,
            IPlantable plantable) {
        IBlockAccessFake blockAccessFake = blockAccessFakeThreadLocal.get();

        try {
            blockAccessFake.setWorld(world, x, y, z);
            blockAccessFake.setBlock(block, meta);
            return block.canSustainPlant(blockAccessFake, x, y, z, direction, plantable);
        } finally {
            blockAccessFake.reset();
        }
    }

    @Override
    public double getEnchantPowerBonus(World world, int x, int y, int z) {
        return block.getEnchantPowerBonus(world, x, y, z);
    }

    @Override
    public boolean canBeCombined(LittleTile tile) {
        if (super.canBeCombined(tile) && tile instanceof LittleTileBlock) {
            LittleTileBlock other = (LittleTileBlock) tile;
            return block == other.block && meta == other.meta
                    && Objects.equals(missingBlockName, other.missingBlockName);
        }
        return false;
    }

    @Override
    protected boolean canSawResize(ForgeDirection direction, EntityPlayer player) {
        return true;
    }

}
