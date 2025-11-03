package com.creativemd.littletiles.common.utils.small;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.Rotation;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

public class LittleTileBox {

    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public LittleTileBox(LittleTileVec center, LittleTileSize size, boolean doCenter) {
        if (doCenter) {
            LittleTileVec offset = size.calculateCenter();
            minX = center.x - offset.x;
            minY = center.y - offset.y;
            minZ = center.z - offset.z;
        } else {
            minX = center.x;
            minY = center.y;
            minZ = center.z;
        }
        maxX = minX + size.sizeX;
        maxY = minY + size.sizeY;
        maxZ = minZ + size.sizeZ;
    }

    public LittleTileBox(String name, NBTTagCompound nbt) {
        if (nbt.getTag(name + "minX") instanceof NBTTagByte) {
            set(
                    nbt.getByte(name + "minX"),
                    nbt.getByte(name + "minY"),
                    nbt.getByte(name + "minZ"),
                    nbt.getByte(name + "maxX"),
                    nbt.getByte(name + "maxY"),
                    nbt.getByte(name + "maxZ"));
            writeToNBT(name, nbt);
        } else set(
                nbt.getInteger(name + "minX"),
                nbt.getInteger(name + "minY"),
                nbt.getInteger(name + "minZ"),
                nbt.getInteger(name + "maxX"),
                nbt.getInteger(name + "maxY"),
                nbt.getInteger(name + "maxZ"));
    }

    public LittleTileBox(CubeObject cube) {
        this(
                (int) (cube.minX * 16),
                (int) (cube.minY * 16),
                (int) (cube.minZ * 16),
                (int) (cube.maxX * 16),
                (int) (cube.maxY * 16),
                (int) (cube.maxZ * 16));
    }

    public LittleTileBox(AxisAlignedBB box) {
        this(
                (int) (box.minX * 16),
                (int) (box.minY * 16),
                (int) (box.minZ * 16),
                (int) (box.maxX * 16),
                (int) (box.maxY * 16),
                (int) (box.maxZ * 16));
    }

    public LittleTileBox(LittleTileVec min, LittleTileVec max) {
        this(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public LittleTileBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AxisAlignedBB getBox() {
        return AxisAlignedBB.getBoundingBox(minX / 16D, minY / 16D, minZ / 16D, maxX / 16D, maxY / 16D, maxZ / 16D);
    }

    public LittleTilesCubeObject getCube() {
        return new LittleTilesCubeObject(minX / 16D, minY / 16D, minZ / 16D, maxX / 16D, maxY / 16D, maxZ / 16D);
    }

    public void writeToNBT(String name, NBTTagCompound nbt) {
        nbt.setInteger(name + "minX", minX);
        nbt.setInteger(name + "minY", minY);
        nbt.setInteger(name + "minZ", minZ);
        nbt.setInteger(name + "maxX", maxX);
        nbt.setInteger(name + "maxY", maxY);
        nbt.setInteger(name + "maxZ", maxZ);
    }

    public Vec3 getSizeD() {
        return Vec3.createVectorHelper((maxX - minX) / 16D, (maxY - minY) / 16D, (maxZ - minZ) / 16D);
    }

    public LittleTileSize getSize() {
        return new LittleTileSize(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public LittleTileBox copy() {
        return new LittleTileBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean isValidBox() {
        return maxX > minX && maxY > minY && maxZ > minZ;
    }

    public void set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean needsMultipleBlocks() {
        int x = minX / 16;
        int y = minY / 16;
        int z = minZ / 16;

        return maxX - x * 16 <= LittleTile.maxPos && maxY - y * 16 <= LittleTile.maxPos
                && maxZ - z * 16 <= LittleTile.maxPos;
    }

    public LittleTileBox combineBoxes(LittleTileBox box) {
        boolean x = this.minX == box.minX && this.maxX == box.maxX;
        boolean y = this.minY == box.minY && this.maxY == box.maxY;
        boolean z = this.minZ == box.minZ && this.maxZ == box.maxZ;

        if (x && y && z) {
            return this;
        }
        if (x && y) {
            if (this.minZ == box.maxZ) return new LittleTileBox(minX, minY, box.minZ, maxX, maxY, maxZ);
            else if (this.maxZ == box.minZ) return new LittleTileBox(minX, minY, minZ, maxX, maxY, box.maxZ);
        }
        if (x && z) {
            if (this.minY == box.maxY) return new LittleTileBox(minX, box.minY, minZ, maxX, maxY, maxZ);
            else if (this.maxY == box.minY) return new LittleTileBox(minX, minY, minZ, maxX, box.maxY, maxZ);
        }
        if (y && z) {
            if (this.minX == box.maxX) return new LittleTileBox(box.minX, minY, minZ, maxX, maxY, maxZ);
            else if (this.maxX == box.minX) return new LittleTileBox(minX, minY, minZ, box.maxX, maxY, maxZ);
        }
        return null;
    }

    public void addOffset(LittleTileVec vec) {
        minX += vec.x;
        minY += vec.y;
        minZ += vec.z;
        maxX += vec.x;
        maxY += vec.y;
        maxZ += vec.z;
    }

    public void subOffset(LittleTileVec vec) {
        minX -= vec.x;
        minY -= vec.y;
        minZ -= vec.z;
        maxX -= vec.x;
        maxY -= vec.y;
        maxZ -= vec.z;
    }

    public void assignCube(CubeObject cube) {
        this.minX = (int) (cube.minX * 16);
        this.minY = (int) (cube.minY * 16);
        this.minZ = (int) (cube.minZ * 16);
        this.maxX = (int) (cube.maxX * 16);
        this.maxY = (int) (cube.maxY * 16);
        this.maxZ = (int) (cube.maxZ * 16);
    }

    public LittleTileVec getMinVec() {
        return new LittleTileVec(minX, minY, minZ);
    }

    public void rotateBoxWithCenter(Rotation direction, Vec3 center) {
        CubeObject cube = this.getCube();
        cube = CubeObject.rotateCube(cube, direction, center);
        this.minX = (int) (cube.minX * 16);
        this.minY = (int) (cube.minY * 16);
        this.minZ = (int) (cube.minZ * 16);
        this.maxX = (int) (cube.maxX * 16);
        this.maxY = (int) (cube.maxY * 16);
        this.maxZ = (int) (cube.maxZ * 16);
    }

    public void flipBox(ForgeDirection direction) {
        switch (direction) {
            case EAST:
            case WEST:
                minX = -minX;
                maxX = -maxX;
                break;
            case UP:
            case DOWN:
                minY = -minY;
                maxY = -maxY;
                break;
            case SOUTH:
            case NORTH:
                minZ = -minZ;
                maxZ = -maxZ;
                break;
            default:
                break;
        }

        resort();
    }

    public void flipBoxWithCenter(ForgeDirection direction, LittleTileVec center) {
        if (center == null) center = new LittleTileVec(8, 8, 8);
        subOffset(center);
        flipBox(direction);
        addOffset(center);
    }

    public void rotateBox(ForgeDirection direction) {
        CubeObject cube = this.getCube();
        cube = CubeObject.rotateCube(cube, direction);
        assignCube(cube);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof LittleTileBox)
            return minX == ((LittleTileBox) object).minX && minY == ((LittleTileBox) object).minY
                    && minZ == ((LittleTileBox) object).minZ
                    && maxX == ((LittleTileBox) object).maxX
                    && maxY == ((LittleTileBox) object).maxY
                    && maxZ == ((LittleTileBox) object).maxZ;
        return super.equals(object);
    }

    @Override
    public String toString() {
        return "[" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + "]";
    }

    public LittleTileVec getNearstedPointTo(LittleTileVec vec) {
        int x = minX;
        if (vec.x >= minX || vec.x <= maxX) x = vec.x;
        if (Math.abs(minX - x) > Math.abs(maxX - x)) x = maxX;

        int y = minY;
        if (vec.y >= minY || vec.y <= maxY) y = vec.y;
        if (Math.abs(minY - y) > Math.abs(maxY - y)) y = maxY;

        int z = minZ;
        if (vec.z >= minZ || vec.z <= maxZ) z = vec.z;
        if (Math.abs(minZ - z) > Math.abs(maxZ - z)) z = maxZ;

        return new LittleTileVec(x, y, z);
    }

    public double distanceTo(LittleTileVec vec) {
        return this.getNearstedPointTo(vec).distanceTo(vec);
    }

    public boolean intersectsWith(LittleTileBox box) {
        return box.maxX > this.minX && box.minX < this.maxX
                && (box.maxY > this.minY && box.minY < this.maxY && box.maxZ > this.minZ && box.minZ < this.maxZ);
    }

    public boolean isBoxInsideBlock() {
        return minX >= LittleTile.minPos && maxX <= LittleTile.maxPos
                && minY >= LittleTile.minPos
                && maxY <= LittleTile.maxPos
                && minZ >= LittleTile.minPos
                && maxZ <= LittleTile.maxPos;
    }

    public LittleTileBox expand(ForgeDirection direction) {
        LittleTileBox result = this.copy();
        switch (direction) {

            case EAST:
                result.maxX++;
                break;
            case WEST:
                result.minX--;
                break;
            case UP:
                result.maxY++;
                break;
            case DOWN:
                result.minY--;
                break;
            case SOUTH:
                result.maxZ++;
                break;
            case NORTH:
                result.minZ--;
                break;
            default:
                break;
        }
        return result;
    }

    public LittleTileBox shrink(ForgeDirection direction) {
        LittleTileBox result = this.copy();
        switch (direction) {

            case EAST:
                result.maxX--;
                break;
            case WEST:
                result.minX++;
                break;
            case UP:
                result.maxY--;
                break;
            case DOWN:
                result.minY++;
                break;
            case SOUTH:
                result.maxZ--;
                break;
            case NORTH:
                result.minZ++;
                break;
            default:
                break;
        }
        return result;
    }

    public void resort() {
        set(
                Math.min(minX, maxX),
                Math.min(minY, maxY),
                Math.min(minZ, maxZ),
                Math.max(minX, maxX),
                Math.max(minY, maxY),
                Math.max(minZ, maxZ));
    }

}
