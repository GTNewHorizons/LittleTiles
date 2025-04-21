package com.creativemd.creativecore.common.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.creativemd.creativecore.common.container.ContainerSub;
import com.creativemd.creativecore.common.gui.SubContainerTileEntity;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class TEContainerPacket extends CreativeCorePacket {

    public NBTTagCompound nbt;
    public int layer;

    public TEContainerPacket() {

    }

    public TEContainerPacket(TileEntity tileEntity, int layer) {
        this.nbt = new NBTTagCompound();
        tileEntity.writeToNBT(nbt);
        this.layer = layer;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(layer);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        nbt = ByteBufUtils.readTag(buf);
        layer = buf.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {
        if (player.openContainer instanceof ContainerSub
                && ((ContainerSub) player.openContainer).layers.get(layer) instanceof SubContainerTileEntity) {
            ((SubContainerTileEntity) ((ContainerSub) player.openContainer).layers.get(layer)).tileEntity
                    .readFromNBT(nbt);
        }
    }

    @Override
    public void executeServer(EntityPlayer player) {

    }

}
