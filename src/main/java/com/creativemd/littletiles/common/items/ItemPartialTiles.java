package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.common.material.LittleMaterialStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An amount of tiles of a single material which does not add up to a whole block. It stores which block and meta it is
 * made of and how many tiles it holds. These items do not stack, they are combined by putting them into a bag.
 * <p>
 * It is rendered like the block it is made of, but only as high as the amount of tiles it holds.
 */
public class ItemPartialTiles extends Item implements ITilesRenderer {

    /** Most tiles a single item can hold. Anything above that is a whole block. */
    public static final int MAX_TILES = LittleMaterialStack.TILES_PER_BLOCK;
    /** The height is rounded to sixteenths, like the grid a block is divided into. */
    public static final int HEIGHT_STEPS = 16;

    public ItemPartialTiles() {
        setMaxStackSize(1);
    }

    /** Creates the given amount of tiles of the given material, or null if the material cannot be stored. */
    public static ItemStack create(String blockName, int meta, int count) {
        LittleMaterialStack material = new LittleMaterialStack(blockName, meta, count);
        if (count > MAX_TILES || !material.isStorable()) return null;

        ItemStack stack = new ItemStack(LittleTiles.partialTiles);
        stack.stackTagCompound = material.writeToNBT();
        return stack;
    }

    public static LittleMaterialStack getMaterialStack(ItemStack stack) {
        return LittleMaterialStack
                .readFromNBT(stack.stackTagCompound == null ? new NBTTagCompound() : stack.stackTagCompound);
    }

    public static Block getBlock(ItemStack stack) {
        return getMaterialStack(stack).material.getBlock();
    }

    public static int getMeta(ItemStack stack) {
        return getMaterialStack(stack).material.meta;
    }

    public static int getTiles(ItemStack stack) {
        return getMaterialStack(stack).count;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack stack) {
        Block block = getBlock(stack);
        if (block == null) return super.getItemStackDisplayName(stack);

        return new ItemStack(block, 1, getMeta(stack)).getDisplayName() + " " + super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add(
                StatCollector
                        .translateToLocalFormatted("littletiles.partial_tiles.amount", getTiles(stack), MAX_TILES));
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTPartialTiles";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ArrayList<CubeObject> getRenderingCubes(ItemStack stack) {
        ArrayList<CubeObject> cubes = new ArrayList<>();
        LittleMaterialStack material = getMaterialStack(stack);
        Block block = material.material.getBlock();
        if (block == null) return cubes;

        cubes.add(new CubeObject(0, 0, 0, 1, getHeight(material.count), 1, block, material.material.meta));
        return cubes;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasBackground(ItemStack stack) {
        return false;
    }

    /** Height of the rendered block, scaled by the amount of tiles. */
    private static double getHeight(int tiles) {
        int steps = (int) Math.ceil((double) tiles / MAX_TILES * HEIGHT_STEPS);
        return (double) Math.max(1, Math.min(HEIGHT_STEPS, steps)) / HEIGHT_STEPS;
    }
}
