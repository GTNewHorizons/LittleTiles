package com.creativemd.littletiles.common.bag;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.littletiles.common.material.LittleMaterialStack;

/**
 * Reads and writes the contents of a little bag from and to the nbt of its item stack.
 */
public class LittleBagStorage {

    private static final String TILES_TAG = "tiles";

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
     * items.
     */
    public LittleBagStock read(int maxTiles, int maxMaterials) {
        LittleBagStock stock = new LittleBagStock();
        NBTTagCompound tiles = getTag();
        for (Object key : tiles.func_150296_c()) {
            LittleMaterialStack material = LittleMaterialStack.readFromNBT(tiles.getCompoundTag((String) key));
            if (!material.isStorable()) continue;

            int remainingTiles = maxTiles - stock.getTileCount();

            // Emergency exit for oversized bags
            if (remainingTiles <= 0 || stock.getMaterialCount() >= maxMaterials) break;

            material.count = Math.min(material.count, remainingTiles);
            stock.add(material);
        }
        return stock;
    }

    public void write(LittleBagStock stock) {
        NBTTagCompound tiles = new NBTTagCompound();
        for (LittleMaterialStack material : stock.getMaterials()) {
            if (material.count > 0) {
                tiles.setTag(material.getKey(), material.writeToNBT());
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
