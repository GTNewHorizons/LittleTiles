package com.creativemd.littletiles.waila;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;

public class WailaLittleTileHandler implements IWailaDataProvider {

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        TileEntity te = accessor.getTileEntity();
        if (te instanceof TileEntityLittleTiles) {
            TileEntityLittleTiles tile = (TileEntityLittleTiles) te;
            tile.updateLoadedTile(accessor.getPlayer());

            try {
                ArrayList<ItemStack> drops = tile.loadedTile.getDrops();
                if (drops.isEmpty()) return currenttip;
                ItemStack stack = drops.get(0);
                currenttip.add(stack.getDisplayName());
            } catch (Exception ignored) {

            }
        }
        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> currenttip, IWailaDataAccessor accessor,
            IWailaConfigHandler config) {
        return currenttip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x,
            int y, int z) {
        return null;
    }
}
