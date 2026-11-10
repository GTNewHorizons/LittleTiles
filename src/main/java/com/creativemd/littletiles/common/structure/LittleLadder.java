package com.creativemd.littletiles.common.structure;

import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.littletiles.common.gui.StructureConfigWidget;

public class LittleLadder extends LittleStructure {

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {

    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {

    }

    @Override
    public LittleStructure parseStructure(StructureConfigWidget config) {
        return new LittleLadder();
    }

    @Override
    public boolean isLadder() {
        return true;
    }

}
