package com.creativemd.littletiles.common.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.items.ItemBlockTiles;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.PlacementHelper;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittlePlacePacket extends CreativeCorePacket {

    public LittlePlacePacket() {
        // Used by reflection
    }

    public LittlePlacePacket(ItemStack stack, LittleTileBlockPos pos, boolean customPlacement,
            LittleTileCutoutInfo cutoutInfo) {
        this.stack = stack;
        this.pos = pos;
        this.customPlacement = customPlacement;
        this.cutoutInfo = cutoutInfo;
    }

    public ItemStack stack;
    public LittleTileBlockPos pos;
    public boolean customPlacement;
    public LittleTileCutoutInfo cutoutInfo;

    @Override
    public void writeBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
        buf.writeInt(pos.getPosX());
        buf.writeInt(pos.getPosY());
        buf.writeInt(pos.getPosZ());
        buf.writeInt(pos.getSubX());
        buf.writeInt(pos.getSubY());
        buf.writeInt(pos.getSubZ());
        buf.writeInt(pos.getSide().ordinal());
        buf.writeBoolean(customPlacement);
        if (cutoutInfo == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeInt(cutoutInfo.type.ordinal());
            buf.writeInt(cutoutInfo.size.x);
            buf.writeInt(cutoutInfo.size.y);
            buf.writeInt(cutoutInfo.size.z);
            buf.writeInt(cutoutInfo.pos.x);
            buf.writeInt(cutoutInfo.pos.y);
            buf.writeInt(cutoutInfo.pos.z);
            buf.writeByte(cutoutInfo.orientation);
        }
    }

    @Override
    public void readBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);
        int posX = buf.readInt();
        int posY = buf.readInt();
        int posZ = buf.readInt();
        int subX = buf.readInt();
        int subY = buf.readInt();
        int subZ = buf.readInt();
        int side = buf.readInt();
        this.pos = new LittleTileBlockPos(posX, posY, posZ, subX, subY, subZ, ForgeDirection.getOrientation(side));
        this.customPlacement = buf.readBoolean();
        if (buf.readBoolean()) {
            this.cutoutInfo = new LittleTileCutoutInfo();
            int cutoutInfo = buf.readInt();
            this.cutoutInfo.type = LittleTileShapeMode.values()[cutoutInfo];
            this.cutoutInfo.size.x = buf.readInt();
            this.cutoutInfo.size.y = buf.readInt();
            this.cutoutInfo.size.z = buf.readInt();
            int cutoutPosX = buf.readInt();
            int cutoutPosY = buf.readInt();
            int cutoutPosZ = buf.readInt();
            this.cutoutInfo.pos = new Vector3i(cutoutPosX, cutoutPosY, cutoutPosZ);
            this.cutoutInfo.orientation = buf.readByte();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        if (PlacementHelper.isLittleBlock(stack)) {
            PlacementHelper helper = PlacementHelper.getInstance(player);

            ((ItemBlockTiles) Item.getItemFromBlock(LittleTiles.blockTile))
                    .placeBlockAt(player, stack, player.worldObj, pos, helper, customPlacement, cutoutInfo);

            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            Slot slot = playerMP.openContainer.getSlotFromInventory(playerMP.inventory, playerMP.inventory.currentItem);
            playerMP.playerNetServerHandler.sendPacket(
                    new S2FPacketSetSlot(
                            playerMP.openContainer.windowId,
                            slot.slotNumber,
                            playerMP.inventory.getCurrentItem()));

        }
    }

}
