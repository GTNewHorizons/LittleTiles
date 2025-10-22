package com.creativemd.littletiles.common.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.common.utils.small.LittleTileVec;

/**
 * Represents the current selected little tile block. Not the block that the player looks at, but the place where the
 * new tile would be placed at.
 */
public class LittleTileBlockPos {

    public static class Comparison {

        public boolean biggerOrEqualX;
        public boolean biggerOrEqualY;
        public boolean biggerOrEqualZ;
    }

    public static class Subtraction {

        public int x;
        public int y;
        public int z;
    }

    private int posX;
    private int posY;
    private int posZ;
    private int subX;
    private int subY;
    private int subZ;
    private final ForgeDirection side;

    public LittleTileBlockPos(int posX, int posY, int posZ, int subX, int subY, int subZ, ForgeDirection side) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.side = side;
        // Sanitize
        moveSubX(subX);
        moveSubY(subY);
        moveSubZ(subZ);
    }

    public static LittleTileBlockPos fromMovingObjectPosition(MovingObjectPosition pos, int align) {
        ForgeDirection side = ForgeDirection.getOrientation(pos.sideHit);
        double x = pos.hitVec.xCoord;
        double y = pos.hitVec.yCoord;
        double z = pos.hitVec.zCoord;

        if (side == ForgeDirection.WEST) {
            x -= align / 16f;
        }
        if (side == ForgeDirection.DOWN) {
            y -= align / 16f;
        }
        if (side == ForgeDirection.NORTH) {
            z -= align / 16f;
        }
        int subX = (int) Math.floor((x - Math.floor(x)) * 16);
        int subY = (int) Math.floor((y - Math.floor(y)) * 16);
        int subZ = (int) Math.floor((z - Math.floor(z)) * 16);

        switch (side) {
            case DOWN:
            case UP:
                subX = subX / align * align;
                subZ = subZ / align * align;
                break;
            case NORTH:
            case SOUTH:
                subX = subX / align * align;
                subY = subY / align * align;
                break;
            case EAST:
            case WEST:
                subY = subY / align * align;
                subZ = subZ / align * align;
                break;
        }

        return new LittleTileBlockPos(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z),
                subX,
                subY,
                subZ,
                side);
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getPosZ() {
        return posZ;
    }

    public int getSubX() {
        return subX;
    }

    public int getSubY() {
        return subY;
    }

    public int getSubZ() {
        return subZ;
    }

    public ForgeDirection getSide() {
        return side;
    }

    public void moveSubX(int count) {
        subX += count;
        while (subX > 15) {
            subX -= 16;
            posX++;
        }
        while (subX < 0) {
            subX += 16;
            posX--;
        }
    }

    public void moveSubY(int count) {
        subY += count;
        while (subY > 15) {
            subY -= 16;
            posY++;
        }
        while (subY < 0) {
            subY += 16;
            posY--;
        }
    }

    public void moveSubZ(int count) {
        subZ += count;
        while (subZ > 15) {
            subZ -= 16;
            posZ++;
        }
        while (subZ < 0) {
            subZ += 16;
            posZ--;
        }
    }

    public void moveInDirection(ForgeDirection direction, int count) {
        moveSubX(direction.offsetX * count);
        moveSubY(direction.offsetY * count);
        moveSubZ(direction.offsetZ * count);
    }

    public LittleTileVec toHitVecRelative() {
        return new LittleTileVec(subX, subY, subZ);
    }

    public Vec3 toHitVec() {
        return Vec3.createVectorHelper(posX + subX / 16.0, posY + subY / 16.0, posZ + subZ / 16.0);
    }

    public Comparison compareTo(LittleTileBlockPos other) {
        Comparison ret = new Comparison();
        if (posX != other.posX) {
            ret.biggerOrEqualX = posX >= other.posX;
        } else {
            ret.biggerOrEqualX = subX >= other.subX;
        }

        if (posY != other.posY) {
            ret.biggerOrEqualY = posY >= other.posY;
        } else {
            ret.biggerOrEqualY = subY >= other.subY;
        }

        if (posZ != other.posZ) {
            ret.biggerOrEqualZ = posZ >= other.posZ;
        } else {
            ret.biggerOrEqualZ = subZ >= other.subZ;
        }

        return ret;
    }

    public Subtraction subtract(LittleTileBlockPos other) {
        Subtraction ret = new Subtraction();
        ret.x = (posX - other.posX) * 16 + (subX - other.subX);
        ret.y = (posY - other.posY) * 16 + (subY - other.subY);
        ret.z = (posZ - other.posZ) * 16 + (subZ - other.subZ);
        return ret;
    }

    public Subtraction subtract(ItemStack stack, LittleTileBlockPos other) {
        Subtraction ret = subtract(other);
        LittleToolHandler handler = new LittleToolHandler(stack);
        int align = handler.getGrid();
        ret.x = Math.max(Math.abs(ret.x) + align, align);
        ret.y = Math.max(Math.abs(ret.y) + align, align);
        ret.z = Math.max(Math.abs(ret.z) + align, align);
        return ret;
    }
}
