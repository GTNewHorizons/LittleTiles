package com.creativemd.littletiles.common.bag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.creativemd.littletiles.common.material.LittleMaterialStack;
import com.creativemd.littletiles.common.material.LittleMaterialValuator;

/**
 * What a little bag holds: an amount of tiles per material, keyed by {@link LittleMaterialStack#getKey()}. Every
 * material is stored once, adding the same material twice merges the amounts.
 */
public class LittleBagStock {

    private final Map<String, LittleMaterialStack> materials = new LinkedHashMap<>();

    /** Adds the amount to the material that is already stored, or stores it as a new one. */
    public void add(LittleMaterialStack material) {
        LittleMaterialStack existing = materials.get(material.getKey());
        if (existing == null) {
            materials.put(material.getKey(), material);
        } else {
            existing.add(material.count);
        }
    }

    /** Adds everything the given item stack is worth, see {@link LittleMaterialValuator}. */
    public void addItemStack(ItemStack stack) {
        for (LittleMaterialStack material : LittleMaterialValuator.stacksOf(stack)) {
            add(material);
        }
    }

    public Collection<LittleMaterialStack> getMaterials() {
        return materials.values();
    }

    /** The contents in the order they are shown in the gui. */
    public List<LittleMaterialStack> getSortedMaterials() {
        List<LittleMaterialStack> sorted = new ArrayList<>(materials.values());
        sorted.sort(LittleMaterialStack.SORT_ORDER);
        return sorted;
    }

    public int getMaterialCount() {
        return materials.size();
    }

    /** Total amount of tiles, saturated so absurd amounts cannot turn into negative ones. */
    public int getTileCount() {
        long tiles = 0;
        for (LittleMaterialStack material : materials.values()) {
            tiles += material.count;
        }
        return (int) Math.min(tiles, Integer.MAX_VALUE);
    }
}
