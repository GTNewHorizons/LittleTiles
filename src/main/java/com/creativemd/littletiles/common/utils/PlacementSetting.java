package com.creativemd.littletiles.common.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.common.packet.LittlePlacementSettingPacket;

/**
 * The grid and place mode for placing tiles, shared by every tile a player places except with the chisel, which keeps
 * its own. The server keeps it with the player, so it survives relogging and dying, and the client mirrors it.
 */
public final class PlacementSetting {

    private static final String KEY = "littletiles:place";

    private static int clientGrid = 1;
    private static LittleTilePlaceMode clientPlaceMode = LittleTilePlaceMode.NORMAL;

    private PlacementSetting() {}

    /** The grid as {@link LittleToolHandler#getGrid}: how many pixels one step is. */
    public static int getGrid(EntityPlayer player) {
        if (player.worldObj.isRemote) return clientGrid;
        NBTTagCompound nbt = load(player);
        return nbt.hasKey("grid") ? validGrid(nbt.getByte("grid")) : 1;
    }

    public static LittleTilePlaceMode getPlaceMode(EntityPlayer player) {
        if (player.worldObj.isRemote) return clientPlaceMode;
        NBTTagCompound nbt = load(player);
        return nbt.hasKey("placeMode") ? LittleTilePlaceMode.fromOrdinal(nbt.getByte("placeMode"))
                : LittleTilePlaceMode.NORMAL;
    }

    /** Sets it on the side it is called on, the gui sets both. */
    public static void setGrid(EntityPlayer player, int grid) {
        grid = validGrid(grid);
        if (player.worldObj.isRemote) clientGrid = grid;
        else save(player).setByte("grid", (byte) grid);
    }

    public static void setPlaceMode(EntityPlayer player, int placeMode) {
        LittleTilePlaceMode mode = LittleTilePlaceMode.fromOrdinal(placeMode);
        if (player.worldObj.isRemote) clientPlaceMode = mode;
        else save(player).setByte("placeMode", (byte) mode.ordinal());
    }

    /** Hands the setting the server keeps for the player to their client. */
    public static void sendTo(EntityPlayerMP player) {
        PacketHandler.sendPacketToPlayer(
                new LittlePlacementSettingPacket(getGrid(player), getPlaceMode(player).ordinal()),
                player);
    }

    /** What the server sent, see {@link #sendTo}. */
    public static void receive(int grid, int placeMode) {
        clientGrid = validGrid(grid);
        clientPlaceMode = LittleTilePlaceMode.fromOrdinal(placeMode);
    }

    private static int validGrid(int grid) {
        return grid == 1 || grid == 2 || grid == 4 || grid == 8 || grid == 16 ? grid : 1;
    }

    private static NBTTagCompound load(EntityPlayer player) {
        return player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(KEY);
    }

    private static NBTTagCompound save(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(KEY)) persisted.setTag(KEY, new NBTTagCompound());
        return persisted.getCompoundTag(KEY);
    }
}
