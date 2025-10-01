package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class LittleToolHandler {

    private ItemStack stack;

    public LittleToolHandler(ItemStack stack) {
        this.stack = stack;
    }

    public Block getBlock() {
        return Blocks.stone;
    }

    public int getMeta() {
        return 0;
    }

    public ItemStack getStack() {
        return new ItemStack(getBlock(), 1, getMeta());
    }
}
