package com.creativemd.littletiles.common.material;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.items.ItemPartialTiles;

/**
 * The kinds of items that are worth an amount of material. Everything else has no material value, especially tools and
 * recipes: those describe tiles without holding any, so counting them would create material out of nothing.
 */
public enum LittleTileItemType {

    /** A whole block, worth {@link LittleMaterialStack#TILES_PER_BLOCK} tiles. */
    BLOCK,
    /** An amount of tiles which does not add up to a whole block, see {@link ItemPartialTiles}. */
    PARTIAL_TILE,
    /** A single little tile, worth as many tiles as its size. */
    TILE,
    /** Several little tiles, which may form a structure. */
    STRUCTURE;

    /** The kind of the given stack, or null if it is worth no material. */
    public static LittleTileItemType detectItemType(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return null;

        Item item = stack.getItem();
        if (item instanceof ItemPartialTiles) {
            return PARTIAL_TILE;
        }
        if (item == Item.getItemFromBlock(LittleTiles.blockTile)) {
            return TILE;
        }
        if (item == LittleTiles.multiTiles) {
            return STRUCTURE;
        }
        if (BlockValidator.isBlockValid(Block.getBlockFromItem(item))) {
            return BLOCK;
        }
        return null;
    }

    /** Whether the item is made of little tiles, which have to be decomposed before they can be stored. */
    public boolean isLittleTile() {
        return this == TILE || this == STRUCTURE;
    }
}
