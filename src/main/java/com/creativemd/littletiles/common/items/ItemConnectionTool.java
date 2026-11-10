package com.creativemd.littletiles.common.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.packet.LittleBlockPacket;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Toggles whether a tile takes part in connected textures. Creative only for now. */
public class ItemConnectionTool extends Item {

    public ItemConnectionTool() {
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add(StatCollector.translateToLocal("item.connection_tool.tooltip"));
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (!player.capabilities.isCreativeMode) return false;
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles) {
            if (world.isRemote) {
                PacketHandler.sendPacketToServer(
                        new LittleBlockPacket(
                                x,
                                y,
                                z,
                                player,
                                LittleBlockPacket.Action.TOGGLE_CONNECTION,
                                new NBTTagCompound()));
            }
            return true;
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":connection_tool";
    }
}
