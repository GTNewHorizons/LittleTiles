package com.creativemd.littletiles.common.packet;

import net.minecraft.entity.player.EntityPlayer;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.utils.PlacementSetting;

import io.netty.buffer.ByteBuf;

/** The player's {@link PlacementSetting} as the server keeps it, sent to their client when they join. */
public class LittlePlacementSettingPacket extends CreativeCorePacket {

    private int grid;
    private int placeMode;

    public LittlePlacementSettingPacket() {

    }

    public LittlePlacementSettingPacket(int grid, int placeMode) {
        this.grid = grid;
        this.placeMode = placeMode;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(grid);
        buf.writeInt(placeMode);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        grid = buf.readInt();
        placeMode = buf.readInt();
    }

    @Override
    public void executeClient(EntityPlayer player) {
        PlacementSetting.receive(grid, placeMode);
    }

    @Override
    public void executeServer(EntityPlayer player) {

    }
}
