package com.creativemd.littletiles.common.utils;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.OrientationMapper;

public class LittleToolHandler {

    private final ItemStack stack;

    public LittleToolHandler(ItemStack stack) {
        this.stack = stack;
    }

    private NBTTagCompound getTag(boolean set) {
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
            return Block.getBlockById(tag.getInteger("block"));
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

    // Handles rotation for a cutout. Only 90 degrees and only one axis.
    public void handleRotation(ForgeDirection direction, NBTTagCompound old) {
        // Get rotation the user requested
        // Intentionally swapped Y and Z!
        float rotZ = (float) Math.PI / 2 * -direction.offsetY;
        float rotY = (float) Math.PI / 2 * -direction.offsetZ;
        Matrix3f rotation = new Matrix3f().rotateY(rotY).rotateZ(rotZ);

        // Get saved rotation
        int orientation = getOrientation();
        Matrix3f matrix = OrientationMapper.fromId(orientation);

        // Apply new rotation and save
        matrix = new Matrix3f(rotation).mul(matrix);
        orientation = OrientationMapper.toId(matrix);
        setOrientation(orientation);

        NBTTagCompound nbt = getTag(true);

        // Handle rotation for block-picked tiles. We need to rotate the cutout pos/size as well...
        if (nbt.hasKey("cutoutPosX") && old != null) {
            int cutoutSizeX = nbt.getInteger("cutoutSizeX");
            int cutoutSizeY = nbt.getInteger("cutoutSizeY");
            int cutoutSizeZ = nbt.getInteger("cutoutSizeZ");

            int cutoutPosX = nbt.getInteger("cutoutPosX");
            int cutoutPosY = nbt.getInteger("cutoutPosY");
            int cutoutPosZ = nbt.getInteger("cutoutPosZ");
            int sizex = old.getInteger("sizex");
            int sizey = old.getInteger("sizey");
            int sizez = old.getInteger("sizez");

            // Treat cutout pos + size as cuboid to rotate it properly
            int minX = cutoutPosX;
            int minY = cutoutPosY;
            int minZ = cutoutPosZ;
            int maxX = minX + cutoutSizeX - sizex;
            int maxY = minY + cutoutSizeY - sizey;
            int maxZ = minZ + cutoutSizeZ - sizez;

            // Rotate cuboid
            Vector3f v1 = rotation.transform(new Vector3f(minX, minY, minZ));
            Vector3f v2 = rotation.transform(new Vector3f(maxX, maxY, maxZ));

            // Save new start pos
            cutoutPosX = Math.round(Math.min(v1.x, v2.x));
            cutoutPosY = Math.round(Math.min(v1.y, v2.y));
            cutoutPosZ = Math.round(Math.min(v1.z, v2.z));
            nbt.setInteger("cutoutPosX", cutoutPosX);
            nbt.setInteger("cutoutPosY", cutoutPosY);
            nbt.setInteger("cutoutPosZ", cutoutPosZ);

            // Rotate size as well
            if (rotY != 0) {
                int temp = cutoutSizeX;
                cutoutSizeX = cutoutSizeZ;
                cutoutSizeZ = temp;
            }
            if (rotZ != 0) {
                int temp = cutoutSizeX;
                cutoutSizeX = cutoutSizeY;
                cutoutSizeY = temp;
            }

            nbt.setInteger("cutoutSizeX", cutoutSizeX);
            nbt.setInteger("cutoutSizeY", cutoutSizeY);
            nbt.setInteger("cutoutSizeZ", cutoutSizeZ);
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
