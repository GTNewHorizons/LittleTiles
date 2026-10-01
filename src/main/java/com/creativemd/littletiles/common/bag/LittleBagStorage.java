package com.creativemd.littletiles.common.bag;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.littletiles.LittleTilesConfig;
import com.creativemd.littletiles.common.material.LittleMaterialStack;

/**
 * Reads and writes the contents of a little bag from and to the nbt of its item stack.
 */
public class LittleBagStorage {

    private static final String TILES_TAG = "tiles";
    /**
     * Most tiles that are loaded from a bag, 100 stacks of blocks. This is no limit for what a bag may hold, it only
     * keeps corrupt content from creating an absurd amount of slots.
     */
    private static final int MAX_READ_TILES = 100 * 64 * LittleMaterialStack.TILES_PER_BLOCK;

    private final Supplier<ItemStack> bagGetter;
    private final Consumer<ItemStack> bagSetter;

    public LittleBagStorage(Supplier<ItemStack> bagGetter, Consumer<ItemStack> bagSetter) {
        this.bagGetter = bagGetter;
        this.bagSetter = bagSetter;
    }

    private NBTTagCompound getTag() {
        ItemStack bag = bagGetter.get();
        if (bag == null || bag.stackTagCompound == null) {
            return new NBTTagCompound();
        }

        return bag.stackTagCompound.getCompoundTag(TILES_TAG);
    }

    /**
     * The stored materials. Materials whose block does not exist anymore are dropped, they cannot be handed out as
     * items. Everything the bag holds is loaded, even above its limits. Only {@link #MAX_READ_TILES} and
     * {@link LittleTilesConfig#MAX_BAG_MATERIALS_LIMIT} guard against corrupt content, anything above them is deleted
     * on the next save.
     */
    public LittleBagStock read() {
        LittleBagStock stock = new LittleBagStock();
        NBTTagCompound tiles = getTag();
        int remainingTiles = MAX_READ_TILES;
        for (Object key : tiles.func_150296_c()) {
            LittleMaterialStack materialStack = LittleMaterialStack.readFromNBT(tiles.getCompoundTag((String) key));
            if (!materialStack.isStorable()) continue;

            // Emergency exit for oversized bags
            if (remainingTiles <= 0 || stock.getMaterialCount() >= LittleTilesConfig.MAX_BAG_MATERIALS_LIMIT) break;

            materialStack.count = Math.min(materialStack.count, remainingTiles);
            remainingTiles -= materialStack.count;
            stock.add(materialStack);
        }
        return stock;
    }

    public void write(LittleBagStock stock) {
        NBTTagCompound tiles = new NBTTagCompound();
        for (LittleMaterialStack materialStack : stock.getMaterials()) {
            if (materialStack.count > 0) {
                tiles.setTag(materialStack.material.getKey(), materialStack.writeToNBT());
            }
        }
        writeTag(tiles);
    }

    private void writeTag(NBTTagCompound nbt) {
        ItemStack bag = bagGetter.get();
        if (bag == null) return;

        if (bag.stackTagCompound == null) {
            bag.stackTagCompound = new NBTTagCompound();
        }
        bag.stackTagCompound.setTag(TILES_TAG, nbt);
        bagSetter.accept(bag);
    }
}
