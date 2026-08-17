package com.creativemd.littletiles.common.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

import com.creativemd.littletiles.common.utils.small.LittleTileBox;

public class LittleTileCutoutInfo {

    public LittleTileShapeMode type;
    public Vector3i size;
    public Vector3i pos;
    public int orientation;
    public ForgeDirection faceStart = ForgeDirection.UNKNOWN;
    public ForgeDirection faceEnd = ForgeDirection.UNKNOWN;
    public int thickness;
    public boolean negX, negY, negZ;

    // Only meaningful for TRIANGLE: the 4 raw vertices, relative to the tile box's own min corner.
    public Vector3i triV1 = new Vector3i();
    public Vector3i triV2 = new Vector3i();
    public Vector3i triV3 = new Vector3i();
    public Vector3i triV4 = new Vector3i();

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
        triV1 = new Vector3i(other.triV1);
        triV2 = new Vector3i(other.triV2);
        triV3 = new Vector3i(other.triV3);
        triV4 = new Vector3i(other.triV4);
    }

    public static LittleTileCutoutInfo fromItemStack(ItemStack stack, LittleTileBlockPos start,
            LittleTileBlockPos end) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        LittleTileShapeMode shape = handler.getShape();

        if (shape == LittleTileShapeMode.BOX || shape == LittleTileShapeMode.TRIANGLE) {
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
        cutoutInfo.type = LittleTileShapeMode.values()[type];
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
        if (cutoutInfo.type == LittleTileShapeMode.TRIANGLE) {
            cutoutInfo.triV1 = readVec(nbt, "cutoutTriV1");
            cutoutInfo.triV2 = readVec(nbt, "cutoutTriV2");
            cutoutInfo.triV3 = readVec(nbt, "cutoutTriV3");
            cutoutInfo.triV4 = readVec(nbt, "cutoutTriV4");
        }
        return cutoutInfo;
    }

    private static Vector3i readVec(NBTTagCompound nbt, String key) {
        return new Vector3i(nbt.getInteger(key + "X"), nbt.getInteger(key + "Y"), nbt.getInteger(key + "Z"));
    }

    private static void writeVec(NBTTagCompound nbt, String key, Vector3i vec) {
        nbt.setInteger(key + "X", vec.x);
        nbt.setInteger(key + "Y", vec.y);
        nbt.setInteger(key + "Z", vec.z);
    }

    /** Builds the tile box that exactly bounds 4 raw vertex offsets (relative to some common anchor). */
    public static LittleTileBox boxFromTriangleVertices(Vector3i v1, Vector3i v2, Vector3i v3, Vector3i v4) {
        int minX = Math.min(Math.min(v1.x, v2.x), Math.min(v3.x, v4.x));
        int minY = Math.min(Math.min(v1.y, v2.y), Math.min(v3.y, v4.y));
        int minZ = Math.min(Math.min(v1.z, v2.z), Math.min(v3.z, v4.z));
        int maxX = Math.max(Math.max(v1.x, v2.x), Math.max(v3.x, v4.x));
        int maxY = Math.max(Math.max(v1.y, v2.y), Math.max(v3.y, v4.y));
        int maxZ = Math.max(Math.max(v1.z, v2.z), Math.max(v3.z, v4.z));
        return new LittleTileBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Builds a TRIANGLE cutout from 4 raw vertex offsets and the box (from {@link #boxFromTriangleVertices}) they fit. */
    public static LittleTileCutoutInfo fromTriangleVertices(LittleTileBox box, Vector3i v1, Vector3i v2, Vector3i v3,
            Vector3i v4) {
        LittleTileCutoutInfo info = new LittleTileCutoutInfo();
        info.type = LittleTileShapeMode.TRIANGLE;
        info.size = new Vector3i(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
        info.pos = new Vector3i();
        info.orientation = 0;
        info.triV1 = new Vector3i(v1.x - box.minX, v1.y - box.minY, v1.z - box.minZ);
        info.triV2 = new Vector3i(v2.x - box.minX, v2.y - box.minY, v2.z - box.minZ);
        info.triV3 = new Vector3i(v3.x - box.minX, v3.y - box.minY, v3.z - box.minZ);
        info.triV4 = new Vector3i(v4.x - box.minX, v4.y - box.minY, v4.z - box.minZ);
        return info;
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
        if (type == LittleTileShapeMode.TRIANGLE) {
            writeVec(nbt, "cutoutTriV1", triV1);
            writeVec(nbt, "cutoutTriV2", triV2);
            writeVec(nbt, "cutoutTriV3", triV3);
            writeVec(nbt, "cutoutTriV4", triV4);
        }
    }
}
