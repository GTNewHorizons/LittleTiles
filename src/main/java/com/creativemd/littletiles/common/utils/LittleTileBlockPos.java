package com.creativemd.littletiles.common.utils;

import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.common.utils.small.LittleTileVec;

/**
 * Represents the current selected little tile block. Not the block that the player looks at, but the place where the
 * new tile would be placed at.
 */
public class LittleTileBlockPos {

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

    public static LittleTileBlockPos fromMovingObjectPosition(MovingObjectPosition pos) {
        ForgeDirection side = ForgeDirection.getOrientation(pos.sideHit);
        double x = pos.hitVec.xCoord;
        double y = pos.hitVec.yCoord;
        double z = pos.hitVec.zCoord;

        if (side == ForgeDirection.WEST) {
            x -= 1.0 / 16;
        }
        if (side == ForgeDirection.DOWN) {
            y -= 1.0 / 16;
        }
        if (side == ForgeDirection.NORTH) {
            z -= 1.0 / 16;
        }
        int subX = (int) Math.floor((x - Math.floor(x)) * 16);
        int subY = (int) Math.floor((y - Math.floor(y)) * 16);
        int subZ = (int) Math.floor((z - Math.floor(z)) * 16);
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
}
