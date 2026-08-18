package com.creativemd.littletiles.common.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

public class LittleTileCutoutInfo {

    public LittleTileShapeMode type;
    public Vector3i size;
    public Vector3i pos;
    public int orientation;
    public ForgeDirection faceStart = ForgeDirection.UNKNOWN;
    public ForgeDirection faceEnd = ForgeDirection.UNKNOWN;
    public int thickness;
    public boolean negX, negY, negZ;
    /**
     * The 8 corners of a {@link LittleTileShapeMode#DEFORMED_BOX}, relative to the tile box's min corner and indexed as
     * described by <code>Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT</code>. Null for every other shape.
     */
    public Vector3i[] corners;

    public LittleTileCutoutInfo() {
        size = new Vector3i();
    }

    public LittleTileCutoutInfo(LittleTileCutoutInfo other) {
        type = other.type;
        size = new Vector3i(other.size);
        pos = new Vector3i(other.pos);
        orientation = other.orientation;
        thickness = other.thickness;
        faceStart = other.faceStart;
        faceEnd = other.faceEnd;
        negX = other.negX;
        negY = other.negY;
        negZ = other.negZ;
        corners = copyCorners(other.corners);
    }

    public static Vector3i[] copyCorners(Vector3i[] corners) {
        if (corners == null) {
            return null;
        }
        Vector3i[] copy = new Vector3i[corners.length];
        for (int i = 0; i < corners.length; i++) {
            copy[i] = new Vector3i(corners[i]);
        }
        return copy;
    }

    public static LittleTileCutoutInfo fromItemStack(ItemStack stack, LittleTileBlockPos start,
            LittleTileBlockPos end) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        LittleTileShapeMode shape = handler.getShape();

        // A deformed box is built from the corners the player dragged, not from a start/end pair.
        if (shape == LittleTileShapeMode.BOX || shape == LittleTileShapeMode.DEFORMED_BOX) {
            return null;
        }

        LittleTileCutoutInfo info = new LittleTileCutoutInfo();
        info.type = shape;

        LittleTileBlockPos.Subtraction subtract = end.subtract(stack, start);
        info.size = new Vector3i(subtract.x, subtract.y, subtract.z);
        info.pos = new Vector3i();
        info.orientation = handler.getOrientation();
        LittleTileBlockPos.Comparison compare = end.compareTo(start);
        info.negX = !compare.biggerOrEqualX;
        info.negY = !compare.biggerOrEqualY;
        info.negZ = !compare.biggerOrEqualZ;
        info.thickness = 4;
        info.faceStart = start.getSide();
        info.faceEnd = end.getSide();

        if (shape == LittleTileShapeMode.PILLAR) {
            int differentAxis = 0;
            if (info.size.x > info.thickness) differentAxis++;
            if (info.size.y > info.thickness) differentAxis++;
            if (info.size.z > info.thickness) differentAxis++;

            // Prevent degenerate walls
            if (differentAxis <= 1 || info.faceStart == info.faceEnd) return null;
            info.orientation = 0;
        }

        return info;
    }

    public static LittleTileCutoutInfo loadFromNBT(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("cutoutType")) {
            return null;
        }
        LittleTileCutoutInfo cutoutInfo = new LittleTileCutoutInfo();
        int type = nbt.getByte("cutoutType");

        LittleTileShapeMode[] mode = LittleTileShapeMode.values();
        if (type < 0 || type >= mode.length) return null;

        cutoutInfo.type = mode[type];
        cutoutInfo.size.x = nbt.getInteger("cutoutSizeX");
        cutoutInfo.size.y = nbt.getInteger("cutoutSizeY");
        cutoutInfo.size.z = nbt.getInteger("cutoutSizeZ");
        int cutoutPosX = nbt.getInteger("cutoutPosX");
        int cutoutPosY = nbt.getInteger("cutoutPosY");
        int cutoutPosZ = nbt.getInteger("cutoutPosZ");
        cutoutInfo.pos = new Vector3i(cutoutPosX, cutoutPosY, cutoutPosZ);
        cutoutInfo.orientation = nbt.getByte("cutoutOrientation");
        cutoutInfo.thickness = nbt.getByte("cutoutThickness");
        cutoutInfo.faceStart = ForgeDirection.values()[nbt.getByte("cutoutFaceStart")];
        cutoutInfo.faceEnd = ForgeDirection.values()[nbt.getByte("cutoutFaceEnd")];
        cutoutInfo.negX = nbt.getBoolean("cutoutNegX");
        cutoutInfo.negY = nbt.getBoolean("cutoutNegY");
        cutoutInfo.negZ = nbt.getBoolean("cutoutNegZ");
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
        if (type == LittleTileShapeMode.PILLAR) {
            nbt.setByte("cutoutThickness", (byte) thickness);
            nbt.setByte("cutoutFaceStart", (byte) faceStart.ordinal());
            nbt.setByte("cutoutFaceEnd", (byte) faceEnd.ordinal());
            nbt.setBoolean("cutoutNegX", negX);
            nbt.setBoolean("cutoutNegY", negY);
            nbt.setBoolean("cutoutNegZ", negZ);
        }
    }
}
