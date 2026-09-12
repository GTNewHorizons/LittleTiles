package com.creativemd.littletiles.common.packet;

import net.minecraft.entity.player.EntityPlayer;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.history.LittleTilesPlacementHistory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittleUndoRedoPacket extends CreativeCorePacket {

    public boolean undo;

    public LittleUndoRedoPacket() {
        // Used by reflection
    }

    public LittleUndoRedoPacket(boolean undo) {
        this.undo = undo;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeBoolean(undo);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        undo = buf.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        if (undo) {
            LittleTilesPlacementHistory.undo(player);
        } else {
            LittleTilesPlacementHistory.redo(player);
        }
    }
}
