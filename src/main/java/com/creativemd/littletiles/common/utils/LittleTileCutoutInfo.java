package com.creativemd.littletiles.common.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.joml.Vector3i;

public class LittleTileCutoutInfo {

    public LittleTileShapeMode type;
    public Vector3i size;
    public Vector3i pos;
    public int orientation;

    public LittleTileCutoutInfo() {
        size = new Vector3i();
    }

    public LittleTileCutoutInfo(LittleTileCutoutInfo other) {
        type = other.type;
        size = new Vector3i(other.size);
        pos = new Vector3i(other.pos);
        orientation = other.orientation;
    }

    public static LittleTileCutoutInfo fromItemStack(ItemStack stack, LittleTileBlockPos start,
            LittleTileBlockPos end) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        LittleTileShapeMode shape = handler.getShape();

        if (shape == LittleTileShapeMode.BOX) {
            return null;
        }

        LittleTileCutoutInfo info = new LittleTileCutoutInfo();
        info.type = shape;

        LittleTileBlockPos.Subtraction subtract = end.subtract(stack, start);
        info.size = new Vector3i(subtract.x, subtract.y, subtract.z);
        info.pos = new Vector3i();
        info.orientation = handler.getOrientation();

        return info;
    }

    public static LittleTileCutoutInfo loadFromNBT(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("cutoutType")) {
            return null;
        }
        LittleTileCutoutInfo cutoutInfo = new LittleTileCutoutInfo();
        int type = nbt.getByte("cutoutType");
        cutoutInfo.type = LittleTileShapeMode.values()[type];
        cutoutInfo.size.x = nbt.getInteger("cutoutSizeX");
        cutoutInfo.size.y = nbt.getInteger("cutoutSizeY");
        cutoutInfo.size.z = nbt.getInteger("cutoutSizeZ");
        int cutoutPosX = nbt.getInteger("cutoutPosX");
        int cutoutPosY = nbt.getInteger("cutoutPosY");
        int cutoutPosZ = nbt.getInteger("cutoutPosZ");
        cutoutInfo.pos = new Vector3i(cutoutPosX, cutoutPosY, cutoutPosZ);
        cutoutInfo.orientation = nbt.getByte("cutoutOrientation");
        return cutoutInfo;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setByte("cutoutType", (byte) type.ordinal());
        nbt.setInteger("cutoutSizeX", size.x);
        nbt.setInteger("cutoutSizeY", size.y);
        nbt.setInteger("cutoutSizeZ", size.z);
        nbt.setInteger("cutoutPosX", pos.x);
        nbt.setInteger("cutoutPosY", pos.y);
        nbt.setInteger("cutoutPosZ", pos.z);
        nbt.setByte("cutoutOrientation", (byte) orientation);
    }
}
