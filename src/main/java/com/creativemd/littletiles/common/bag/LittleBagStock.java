package com.creativemd.littletiles.common.bag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.creativemd.littletiles.common.material.LittleMaterial;
import com.creativemd.littletiles.common.material.LittleMaterialStack;
import com.creativemd.littletiles.common.material.LittleMaterialValuator;

/**
 * What a little bag holds: an amount of tiles per material, keyed by {@link LittleMaterial}. Every material is stored
 * once, adding the same material twice merges the amounts.
 */
public class LittleBagStock {

    private final Map<LittleMaterial, LittleMaterialStack> materials = new LinkedHashMap<>();

    /**
     * Adds the amount to the material that is already stored, or stores it as a new one. The given stack is copied, as
     * the stored one keeps growing with every material that is added.
     */
    public void add(LittleMaterialStack materialStack) {
        LittleMaterialStack existing = materials.get(materialStack.material);
        if (existing == null) {
            materials.put(materialStack.material, materialStack.copy());
        } else {
            existing.add(materialStack.count);
        }
    }

    /** Adds everything the given item stack is worth, see {@link LittleMaterialValuator}. */
    public void addItemStack(ItemStack stack) {
        for (LittleMaterialStack materialStack : LittleMaterialValuator.stacksOf(stack)) {
            add(materialStack);
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
}
