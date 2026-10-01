package com.creativemd.littletiles.common.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.items.ItemPartialTiles;
import com.creativemd.littletiles.common.utils.LittleTilePreview;

/**
 * Tells how much material an item stack is worth. Little tiles are decomposed into the materials they are made of,
 * where every tile is worth as many tiles as its size.
 */
public class LittleMaterialValuator {

    private LittleMaterialValuator() {}

    /** The materials the given stack is worth, or empty if it is worth nothing that can be stored. */
    public static List<LittleMaterialStack> stacksOf(ItemStack stack) {
        LittleTileItemType type = LittleTileItemType.detectItemType(stack);
        if (type == null) return Collections.emptyList();

        if (type.isLittleTile()) {
            return stacksOfLittleTiles(stack);
        }
        if (type == LittleTileItemType.PARTIAL_TILE) {
            return singletonIfStorable(ItemPartialTiles.getMaterialStack(stack), ItemPartialTiles.MAX_TILES);
        }

        if (type == LittleTileItemType.BLOCK) {
            long count = (long) stack.stackSize * LittleMaterialStack.TILES_PER_BLOCK;
            if (count > Integer.MAX_VALUE) return Collections.emptyList();
            return singletonIfStorable(
                    new LittleMaterialStack(
                            new LittleMaterial(Block.getBlockFromItem(stack.getItem()), stack.getItemDamage()),
                            (int) count),
                    Integer.MAX_VALUE);
        }

        return Collections.emptyList();
    }

    private static List<LittleMaterialStack> stacksOfLittleTiles(ItemStack stack) {
        List<LittleTilePreview> previews = ((ILittleTile) stack.getItem()).getLittlePreview(stack);
        if (previews == null || previews.isEmpty()) return Collections.emptyList();

        List<LittleMaterialStack> materials = new ArrayList<>();
        for (LittleTilePreview preview : previews) {
            if (preview == null || preview.nbt == null || preview.size == null) return Collections.emptyList();

            long count = (long) preview.size.getVolume() * stack.stackSize;
            if (count <= 0 || count > Integer.MAX_VALUE) return Collections.emptyList();

            LittleMaterialStack material = new LittleMaterialStack(
                    preview.nbt.getString("block"),
                    preview.nbt.getInteger("meta"),
                    (int) count);
            if (!material.isStorable()) return Collections.emptyList();
            materials.add(material);
        }
        return materials;
    }

    private static List<LittleMaterialStack> singletonIfStorable(LittleMaterialStack material, int maxCount) {
        return material.isStorable() && material.count <= maxCount ? Collections.singletonList(material)
                : Collections.emptyList();
    }

    /** Amount of tiles the given stack is worth, counting only what can actually be stored. */
    public static int tilesOf(ItemStack stack) {
        long tiles = 0;
        for (LittleMaterialStack material : stacksOf(stack)) {
            tiles += material.count;
        }
        return (int) Math.min(tiles, Integer.MAX_VALUE);
    }
}
