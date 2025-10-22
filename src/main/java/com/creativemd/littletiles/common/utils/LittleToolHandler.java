package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class LittleToolHandler {

    private final ItemStack stack;

    public LittleToolHandler(ItemStack stack) {
        this.stack = stack;
    }

    private NBTTagCompound getTag(boolean set) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            if (set) {
                stack.setTagCompound(tag);
            }
        }
        return tag;
    }

    public void setBlock(Block block, int meta) {
        NBTTagCompound tag = getTag(true);
        tag.setInteger("block", Block.getIdFromBlock(block));
        tag.setInteger("meta", meta);
    }

    public Block getBlock() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("block")) {
            return Block.getBlockById(tag.getInteger("block"));
        }
        return Blocks.stone;
    }

    public int getMeta() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("meta")) {
            return tag.getInteger("meta");
        }
        return 0;
    }

    public ItemStack getStack() {
        return new ItemStack(getBlock(), 1, getMeta());
    }
}
