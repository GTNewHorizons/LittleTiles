package com.creativemd.littletiles.common.utils.small;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

public class LittleTileSize {

    public int sizeX;
    public int sizeY;
    public int sizeZ;

    public LittleTileSize(String name, NBTTagCompound nbt) {
        this.sizeX = nbt.getInteger(name + "x");
        this.sizeY = nbt.getInteger(name + "y");
        this.sizeZ = nbt.getInteger(name + "z");
    }

    public LittleTileSize(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public void set(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LittleTileSize)
            return sizeX == ((LittleTileSize) object).sizeX && sizeY == ((LittleTileSize) object).sizeY
                    && sizeZ == ((LittleTileSize) object).sizeZ;
        return super.equals(object);
    }

    public float getVolume() {
        return sizeX * sizeY * sizeZ;
    }

    /** Returns how the volume in percent to a size of a normal block */
    public float getPercentVolume() {
        return getVolume() / (16 * 16 * 16);
    }

    public LittleTileVec calculateInvertedCenter() {
        double x = sizeX / 2D;
        double y = sizeY / 2D;
        double z = sizeZ / 2D;
        return new LittleTileVec((int) (Math.ceil(x)), (int) (Math.ceil(y)), (int) (Math.ceil(z)));
    }

    public LittleTileVec calculateCenter() {
        double x = sizeX / 2D;
        double y = sizeY / 2D;
        double z = sizeZ / 2D;
        return new LittleTileVec((int) (Math.floor(x)), (int) (Math.floor(y)), (int) (Math.floor(z)));
    }

    public double getPosX() {
        return (double) sizeX / 16D;
    }

    public double getPosY() {
        return (double) sizeY / 16D;
    }

    public double getPosZ() {
        return (double) sizeZ / 16D;
    }

    public LittleTileSize copy() {
        return new LittleTileSize(sizeX, sizeY, sizeZ);
    }

    public void rotateSize(ForgeDirection direction) {
        switch (direction) {
            case UP:
            case DOWN:
                int tempY = sizeY;
                sizeY = sizeX;
                sizeX = tempY;
                break;
            case SOUTH:
            case NORTH:
                int tempZ = sizeZ;
                sizeZ = sizeX;
                sizeX = tempZ;
                break;
            default:
                break;
        }
    }

    public void writeToNBT(String name, NBTTagCompound nbt) {
        nbt.setInteger(name + "x", sizeX);
        nbt.setInteger(name + "y", sizeY);
        nbt.setInteger(name + "z", sizeZ);
    }

    @Override
    public String toString() {
        return "[" + sizeX + "," + sizeY + "," + sizeZ + "]";
    }

    public LittleTileSize max(LittleTileSize size) {
        this.sizeX = Math.max(this.sizeX, size.sizeX);
        this.sizeY = Math.max(this.sizeY, size.sizeY);
        this.sizeZ = Math.max(this.sizeZ, size.sizeZ);
        return this;
    }

}
