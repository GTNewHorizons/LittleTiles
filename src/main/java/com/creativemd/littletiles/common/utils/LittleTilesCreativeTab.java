package com.creativemd.littletiles.common.utils;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.creativemd.littletiles.LittleTiles;

public class LittleTilesCreativeTab extends CreativeTabs {

    public LittleTilesCreativeTab(String label) {
        super(label);
    }

    @Override
    public Item getTabIconItem() {
        return LittleTiles.chisel;
    }
}
