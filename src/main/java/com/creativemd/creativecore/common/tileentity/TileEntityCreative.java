package com.creativemd.creativecore.common.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class TileEntityCreative extends TileEntity {

    public void getDescriptionNBT(NBTTagCompound nbt) {

    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        getDescriptionNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, nbt);
    }

    public double getDistance(ChunkCoordinates coord) {
        return Math.sqrt(
                Math.pow(xCoord + coord.posX, 2) + Math.pow(yCoord + coord.posY, 2) + Math.pow(zCoord + coord.posZ, 2));
    }

    @SideOnly(Side.CLIENT)
    public void updateRender() {
        worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }

    public void updateBlock() {
        if (!worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            markDirty();
        }
    }

    public void receiveUpdatePacket(NBTTagCompound nbt) {

    }

}
