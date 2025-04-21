package com.creativemd.creativecore.common.gui;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public abstract class SubGuiTileEntity extends SubGui {

    public TileEntity tileEntity;

    public SubGuiTileEntity(int width, int height, TileEntity tileEntity) {
        super(width, height);
        this.tileEntity = tileEntity;
    }

    public SubGuiTileEntity(TileEntity tileEntity) {
        super();
        this.tileEntity = tileEntity;
    }

    @Override
    public void readFromOpeningNBT(NBTTagCompound nbt) {
        tileEntity.readFromNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        tileEntity.readFromNBT(nbt);
    }

}
