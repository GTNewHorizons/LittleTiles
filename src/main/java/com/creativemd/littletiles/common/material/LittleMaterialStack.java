package com.creativemd.littletiles.common.material;

import java.util.Comparator;

import net.minecraft.nbt.NBTTagCompound;

/**
 * An amount of a single material. {@link #count} is measured in tiles, not blocks.
 */
public class LittleMaterialStack {

    public static final int TILES_PER_BLOCK = 4096;

    /** Order of materials in a gui: by block name, then by meta. */
    public static final Comparator<LittleMaterialStack> SORT_ORDER = Comparator
            .comparing((LittleMaterialStack stack) -> stack.material.blockName)
            .thenComparingInt(stack -> stack.material.meta);

    private static final String COUNT_TAG = "count";

    public final LittleMaterial material;
    public int count;

    public LittleMaterialStack(LittleMaterial material, int count) {
        this.material = material;
        this.count = count;
    }

    /** Whether this holds an amount of a material that can actually be stored. */
    public boolean isStorable() {
        return count > 0 && material.isValid();
    }

    public int getBlocks() {
        return count / TILES_PER_BLOCK;
    }

    /** Tiles that don't add up to a whole block. */
    public int getRemainingTiles() {
        return count % TILES_PER_BLOCK;
    }

    /** Adds the given amount, saturating instead of overflowing, so absurd amounts cannot turn into negative ones. */
    public void add(int tiles) {
        count = (int) Math.min((long) count + tiles, Integer.MAX_VALUE);
    }

    public LittleMaterialStack copy() {
        return new LittleMaterialStack(material, count);
    }

    public static LittleMaterialStack readFromNBT(NBTTagCompound nbt) {
        return new LittleMaterialStack(LittleMaterial.readFromNBT(nbt), nbt.getInteger(COUNT_TAG));
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        material.writeToNBT(nbt);
        nbt.setInteger(COUNT_TAG, count);
        return nbt;
    }
}
