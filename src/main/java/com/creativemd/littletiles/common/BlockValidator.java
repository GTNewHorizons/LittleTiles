package com.creativemd.littletiles.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class BlockValidator {

    public static boolean isBlockValid(Block block) {
        if (block == Blocks.air) return false;
        if (new ItemStack(block).getItem() == null) return false;
        if (block.hasTileEntity(0)) return false;
        return block.isNormalCube() || block.isOpaqueCube()
                || block.renderAsNormalBlock()
                || block instanceof BlockGlass
                || block instanceof BlockStainedGlass;
    }
}
