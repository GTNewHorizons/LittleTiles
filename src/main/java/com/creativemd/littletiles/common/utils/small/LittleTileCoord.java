package com.creativemd.littletiles.common.utils.small;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;

import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;

public class LittleTileCoord {

    private final ChunkCoordinates coord;
    public LittleTileVec position;
    /**
     * The cutout of the tile, as tiles can share their {@link #position}, null for a plain box. Unknown for coords saved
     * before, which then only match by the position.
     */
    private LittleTileCutoutInfo cutout;
    private boolean knowsCutout;

    public LittleTileCoord(TileEntity te, ChunkCoordinates coord, LittleTileVec position) {
        this(te.xCoord, te.yCoord, te.zCoord, coord, position);
    }

    public LittleTileCoord(ChunkCoordinates origin, ChunkCoordinates coord, LittleTileVec position) {
        this(origin.posX, origin.posY, origin.posZ, coord, position);
    }

    public LittleTileCoord(int baseX, int baseY, int baseZ, ChunkCoordinates coord, LittleTileVec position) {
        this(coord.posX - baseX, coord.posY - baseY, coord.posZ - baseZ, position);
    }

    protected LittleTileCoord(int relativeX, int relativeY, int relativeZ, LittleTileVec position) {
        this.coord = new ChunkCoordinates(relativeX, relativeY, relativeZ);
        this.position = position;
    }

    public LittleTileCoord(String id, NBTTagCompound nbt) {
        coord = new ChunkCoordinates(
                nbt.getInteger(id + "coordX"),
                nbt.getInteger(id + "coordY"),
                nbt.getInteger(id + "coordZ"));
        position = new LittleTileVec(id + "pos", nbt);
        if (nbt.hasKey(id + "coordCutout")) {
            knowsCutout = true;
            cutout = LittleTileCutoutInfo.loadFromNBT(nbt.getCompoundTag(id + "coordCutout"));
        }
    }

    public LittleTileCoord(NBTTagCompound nbt) {
        this("", nbt);
    }

    /** Sets the cutout of the tile at {@link #position}, null for a plain box. */
    public LittleTileCoord withCutout(LittleTileCutoutInfo cutout) {
        this.cutout = cutout == null ? null : new LittleTileCutoutInfo(cutout);
        knowsCutout = true;
        return this;
    }

    /** Whether it is the tile this points to, given it is in the block this points to. */
    public boolean matches(LittleTile tile) {
        LittleTileVec corner = tile.cornerVec;
        if (corner.x != position.x || corner.y != position.y || corner.z != position.z) return false;
        return !knowsCutout || LittleTileCutoutInfo.sameCutout(cutout, tile.getCutoutInfo());
    }

    public ChunkCoordinates getAbsolutePosition(TileEntity te) {
        return getAbsolutePosition(te.xCoord, te.yCoord, te.zCoord);
    }

    public ChunkCoordinates getAbsolutePosition(ChunkCoordinates origin) {
        return getAbsolutePosition(origin.posX, origin.posY, origin.posZ);
    }

    public ChunkCoordinates getAbsolutePosition(int x, int y, int z) {
        return new ChunkCoordinates(coord.posX + x, coord.posY + y, coord.posZ + z);
    }

    public void writeToNBT(String id, NBTTagCompound nbt) {
        nbt.setInteger(id + "coordX", coord.posX);
        nbt.setInteger(id + "coordY", coord.posY);
        nbt.setInteger(id + "coordZ", coord.posZ);
        position.writeToNBT(id + "pos", nbt);
        if (knowsCutout) {
            // Empty for a plain box
            NBTTagCompound shape = new NBTTagCompound();
            if (cutout != null) cutout.writeToNBT(shape);
            nbt.setTag(id + "coordCutout", shape);
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        writeToNBT("", nbt);
    }

    @Override
    public String toString() {
        return "coord:[" + coord.posX + "," + coord.posY + "," + coord.posZ + "]|position:" + position;
    }

    public LittleTileCoord copy() {
        LittleTileCoord copy = new LittleTileCoord(coord.posX, coord.posY, coord.posZ, position.copy());
        if (knowsCutout) copy.withCutout(cutout);
        return copy;
    }

}
