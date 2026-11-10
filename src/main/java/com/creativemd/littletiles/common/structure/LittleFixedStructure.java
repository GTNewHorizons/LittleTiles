package com.creativemd.littletiles.common.structure;

import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.littletiles.common.gui.StructureConfigWidget;

public class LittleFixedStructure extends LittleStructure {

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {

    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {

    }

    @Override
    public LittleStructure parseStructure(StructureConfigWidget config) {
        return new LittleFixedStructure();
    }

}
