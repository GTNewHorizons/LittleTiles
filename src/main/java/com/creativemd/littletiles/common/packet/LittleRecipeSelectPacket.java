package com.creativemd.littletiles.common.packet;

import net.minecraft.entity.player.EntityPlayer;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.items.ItemRecipe;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/** Both corners of the area selected with an empty recipe, sent once the selection is done. */
public class LittleRecipeSelectPacket extends CreativeCorePacket {

    private static final double MAX_SELECTION_DISTANCE = 32.0;

    public int firstX, firstY, firstZ;
    public int secondX, secondY, secondZ;

    public LittleRecipeSelectPacket() {
        // Used by reflection
    }

    public LittleRecipeSelectPacket(int firstX, int firstY, int firstZ, int secondX, int secondY, int secondZ) {
        this.firstX = firstX;
        this.firstY = firstY;
        this.firstZ = firstZ;
        this.secondX = secondX;
        this.secondY = secondY;
        this.secondZ = secondZ;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(firstX);
        buf.writeInt(firstY);
        buf.writeInt(firstZ);
        buf.writeInt(secondX);
        buf.writeInt(secondY);
        buf.writeInt(secondZ);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        firstX = buf.readInt();
        firstY = buf.readInt();
        firstZ = buf.readInt();
        secondX = buf.readInt();
        secondY = buf.readInt();
        secondZ = buf.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        // A marked corner can be moved away with the arrow keys, so both are checked, not just the clicked one.
        if (player.getDistance(firstX, firstY, firstZ) > MAX_SELECTION_DISTANCE
                || player.getDistance(secondX, secondY, secondZ) > MAX_SELECTION_DISTANCE)
            return;
        ItemRecipe.selectArea(player, player.getHeldItem(), firstX, firstY, firstZ, secondX, secondY, secondZ);
    }
}
