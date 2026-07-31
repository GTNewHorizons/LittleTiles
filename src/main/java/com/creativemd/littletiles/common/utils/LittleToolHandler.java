package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.creativecore.common.utils.Rotation;
import com.creativemd.creativecore.common.utils.RotationUtils;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.OrientationMapper;
import com.creativemd.littletiles.client.util3d.Plane3d;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

public class LittleToolHandler {

    private final ItemStack stack;
    private final NBTTagCompound rawNbt;

    public LittleToolHandler(ItemStack stack) {
        this.stack = stack;
        this.rawNbt = null;
    }

    public LittleToolHandler(NBTTagCompound nbt) {
        this.stack = null;
        this.rawNbt = nbt;
    }

    private NBTTagCompound getTag(boolean set) {
        if (rawNbt != null) {
            return rawNbt;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            if (set) {
                stack.setTagCompound(tag);
            }
        }
        return tag;
    }

    public void setBlock(Block block, int meta) {
        NBTTagCompound tag = getTag(true);
        tag.setInteger("block", Block.getIdFromBlock(block));
        tag.setInteger("meta", meta);
    }

    public Block getBlock() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("block")) {
            Block ret = Block.getBlockById(tag.getInteger("block"));
            if (ret != null && ret != Blocks.air) {
                return ret;
            }
        }
        return Blocks.stone;
    }

    public int getMeta() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("meta")) {
            return tag.getInteger("meta");
        }
        return 0;
    }

    public ItemStack getStack() {
        return new ItemStack(getBlock(), 1, getMeta());
    }

    public int getGrid() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("grid")) {
            return tag.getByte("grid");
        }
        return 1;
    }

    public void setGrid(int grid) {
        NBTTagCompound tag = getTag(true);
        tag.setByte("grid", (byte) grid);
    }

    public LittleTileShapeMode getShape() {
        NBTTagCompound tag = getTag(false);
        LittleTileCutoutInfo cutout = LittleTileCutoutInfo.loadFromNBT(tag);
        if (cutout != null) {
            return cutout.type;
        }
        int shape = 0;
        if (tag.hasKey("shape")) {
            shape = tag.getByte("shape");
        }
        return LittleTileShapeMode.values()[shape];
    }

    public void setShape(int shape) {
        NBTTagCompound tag = getTag(true);
        tag.setByte("shape", (byte) shape);
    }

    public int getOrientation() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("cutoutOrientation")) {
            return tag.getByte("cutoutOrientation");
        }
        if (tag.hasKey("orientation")) {
            return tag.getByte("orientation");
        }
        return 0;
    }

    public void setOrientation(int orientation) {
        NBTTagCompound tag = getTag(true);
        if (tag.hasKey("cutoutOrientation")) {
            tag.setByte("cutoutOrientation", (byte) orientation);
        } else {
            tag.setByte("orientation", (byte) orientation);
        }
    }

    public LittleTilePlaceMode getPlaceMode() {
        NBTTagCompound tag = getTag(false);
        if (tag.hasKey("placeMode")) {
            return LittleTilePlaceMode.fromOrdinal(tag.getByte("placeMode"));
        }
        return LittleTilePlaceMode.NORMAL;
    }

    public void setPlaceMode(int mode) {
        NBTTagCompound tag = getTag(true);
        tag.setByte("placeMode", (byte) LittleTilePlaceMode.fromOrdinal(mode).ordinal());
    }

    public static ForgeDirection getDirectionForNormal(Vector3d normal) {
        Plane3d ret = null;
        double biggestDot = -1;
        for (Plane3d plane : Plane3d.planes) {
            double dot = plane.getNormal().dot(normal);
            if (dot > biggestDot) {
                biggestDot = dot;
                ret = plane;
            }
        }
        return ret.getDirection();
    }

    private static Vector3f toVector3f(Vec3 vec) {
        return new Vector3f((float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord);
    }

    /**
     * The tile boxes are rotated by CreativeCore ({@link RotationUtils#applyVectorRotation}). The cutout lives in the
     * very same coordinate space, so its rotation has to use exactly that convention. Building the matrix from
     * hand-written angles instead got the sign wrong for {@link ForgeDirection#UP}/{@link ForgeDirection#DOWN}, which
     * rotated the cutout against its box. Deriving the matrix from the images of the unit axes cannot drift apart.
     */
    private static Matrix3f getRotationMatrix(ForgeDirection direction) {
        Rotation rotation = Rotation.getRotationByDirection(direction);
        return new Matrix3f(
                toVector3f(RotationUtils.applyVectorRotation(Vec3.createVectorHelper(1, 0, 0), rotation)),
                toVector3f(RotationUtils.applyVectorRotation(Vec3.createVectorHelper(0, 1, 0), rotation)),
                toVector3f(RotationUtils.applyVectorRotation(Vec3.createVectorHelper(0, 0, 1), rotation)));
    }

    /**
     * Handles rotation for a cutout. Only 90 degrees and only one axis.
     *
     * @param oldSize size of the tile before it got rotated, null if unknown. Without it the cutout keeps its position
     *                and only the orientation is updated, which is all a cutout that still covers its whole tile needs.
     */
    public void handleRotation(ForgeDirection direction, LittleTileSize oldSize) {
        // Get rotation the user requested, in the same convention the tile boxes are rotated with
        Matrix3f rotation = getRotationMatrix(direction);

        // Get saved rotation
        int orientation = getOrientation();
        Matrix3f matrix = OrientationMapper.fromId(orientation);

        // Apply new rotation and save
        matrix = new Matrix3f(orientationRotation).mul(matrix);
        orientation = OrientationMapper.toId(matrix);
        setOrientation(orientation);

        NBTTagCompound nbt = getTag(true);

        // Handle rotation for block-picked tiles. We need to rotate the cutout pos/size as well...
        if (nbt.hasKey("cutoutPosX") && oldSize != null) {
            int cutoutSizeX = nbt.getInteger("cutoutSizeX");
            int cutoutSizeY = nbt.getInteger("cutoutSizeY");
            int cutoutSizeZ = nbt.getInteger("cutoutSizeZ");

            int cutoutPosX = nbt.getInteger("cutoutPosX");
            int cutoutPosY = nbt.getInteger("cutoutPosY");
            int cutoutPosZ = nbt.getInteger("cutoutPosZ");

            // Treat cutout pos + size as cuboid to rotate it properly
            int minX = cutoutPosX;
            int minY = cutoutPosY;
            int minZ = cutoutPosZ;
            int maxX = minX + cutoutSizeX - oldSize.sizeX;
            int maxY = minY + cutoutSizeY - oldSize.sizeY;
            int maxZ = minZ + cutoutSizeZ - oldSize.sizeZ;

            // Rotate cuboid
            Vector3f v1 = positionRotation.transform(new Vector3f(minX, minY, minZ));
            Vector3f v2 = positionRotation.transform(new Vector3f(maxX, maxY, maxZ));

            // Save new start pos
            cutoutPosX = Math.round(Math.min(v1.x, v2.x));
            cutoutPosY = Math.round(Math.min(v1.y, v2.y));
            cutoutPosZ = Math.round(Math.min(v1.z, v2.z));
            nbt.setInteger("cutoutPosX", cutoutPosX);
            nbt.setInteger("cutoutPosY", cutoutPosY);
            nbt.setInteger("cutoutPosZ", cutoutPosZ);

            // Rotate size as well
            if (direction.offsetZ != 0) {
                int temp = cutoutSizeX;
                cutoutSizeX = cutoutSizeZ;
                cutoutSizeZ = temp;
            }
            if (direction.offsetY != 0) {
                int temp = cutoutSizeX;
                cutoutSizeX = cutoutSizeY;
                cutoutSizeY = temp;
            }

            nbt.setInteger("cutoutSizeX", cutoutSizeX);
            nbt.setInteger("cutoutSizeY", cutoutSizeY);
            nbt.setInteger("cutoutSizeZ", cutoutSizeZ);

            boolean negX = nbt.getBoolean("cutoutNegX");
            boolean negY = nbt.getBoolean("cutoutNegY");
            boolean negZ = nbt.getBoolean("cutoutNegZ");

            Vector3f negVec = new Vector3f(negX ? -1 : 1, negY ? -1 : 1, negZ ? -1 : 1);
            negVec = orientationRotation.transform(negVec);
            negX = negVec.x < 0;
            negY = negVec.y < 0;
            negZ = negVec.z < 0;

            nbt.setBoolean("cutoutNegX", negX);
            nbt.setBoolean("cutoutNegY", negY);
            nbt.setBoolean("cutoutNegZ", negZ);

            int faceStartI = nbt.getByte("cutoutFaceStart");
            int faceEndI = nbt.getByte("cutoutFaceEnd");
            Vector3d faceStart = Plane3d.planes[faceStartI].getNormal();
            Vector3d faceEnd = Plane3d.planes[faceEndI].getNormal();

            faceStart = new Vector3d(orientationRotation.transform(faceStart.toVector3f()));
            faceEnd = new Vector3d(orientationRotation.transform(faceEnd.toVector3f()));

            faceStartI = getDirectionForNormal(faceStart).ordinal();
            faceEndI = getDirectionForNormal(faceEnd).ordinal();
            nbt.setByte("cutoutFaceStart", (byte) faceStartI);
            nbt.setByte("cutoutFaceEnd", (byte) faceEndI);
        }
    }

    public Vector3i getTileOriginal() {
        NBTTagCompound nbt = getTag(false);
        if (!nbt.hasKey("cutoutPosX")) {
            return new Vector3i();
        }
        int cutoutPosX = nbt.getInteger("cutoutPosX");
        int cutoutPosY = nbt.getInteger("cutoutPosY");
        int cutoutPosZ = nbt.getInteger("cutoutPosZ");
        return new Vector3i(cutoutPosX, cutoutPosY, cutoutPosZ);
    }

    public Vector3d getTileSize() {
        NBTTagCompound nbt = getTag(false);
        if (!nbt.hasKey("cutoutSizeX")) {
            return null;
        }
        int cutoutSizeX = nbt.getInteger("cutoutSizeX");
        int cutoutSizeY = nbt.getInteger("cutoutSizeY");
        int cutoutSizeZ = nbt.getInteger("cutoutSizeZ");
        return new Vector3d(cutoutSizeX / 16.0, cutoutSizeY / 16.0, cutoutSizeZ / 16.0);
    }
}
