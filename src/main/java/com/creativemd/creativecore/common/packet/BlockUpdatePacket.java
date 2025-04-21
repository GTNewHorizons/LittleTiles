package com.creativemd.creativecore.common.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.creativemd.creativecore.common.tileentity.TileEntityCreative;

import io.netty.buffer.ByteBuf;

public class BlockUpdatePacket extends CreativeCorePacket {

    public BlockUpdatePacket() {

    }

    public int x;
    public int y;
    public int z;
    public NBTTagCompound nbt;

    public BlockUpdatePacket(TileEntity te, NBTTagCompound nbt) {
        this(te.xCoord, te.yCoord, te.zCoord, nbt);
    }

    public BlockUpdatePacket(int x, int y, int z, NBTTagCompound nbt) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.nbt = nbt;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        writeNBT(buf, nbt);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        nbt = readNBT(buf);
    }

    @Override
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        TileEntity te = player.worldObj.getTileEntity(x, y, z);
        if (te instanceof TileEntityCreative) {
            ((TileEntityCreative) te).receiveUpdatePacket(nbt);
        }
    }

}
