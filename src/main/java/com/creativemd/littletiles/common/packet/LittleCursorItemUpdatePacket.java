package com.creativemd.littletiles.common.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.LittleTiles;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittleCursorItemUpdatePacket extends CreativeCorePacket {

    public NBTTagCompound nbt;

    public LittleCursorItemUpdatePacket() {

    }

    public LittleCursorItemUpdatePacket(NBTTagCompound nbt) {
        this.nbt = nbt;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        writeNBT(buf, nbt);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        nbt = readNBT(buf);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        ItemStack cursor = player.inventory.getItemStack();
        if (cursor == null || cursor.getItem() != LittleTiles.chisel) {
            return;
        }
        cursor.setTagCompound(nbt);
    }

}
