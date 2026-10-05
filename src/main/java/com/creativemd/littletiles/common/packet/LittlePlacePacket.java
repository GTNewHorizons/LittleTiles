package com.creativemd.littletiles.common.packet;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.items.ItemBlockTiles;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittlePlacePacket extends CreativeCorePacket {

    private static final double MAX_PLACEMENT_DISTANCE = 32.0;

    public LittlePlacePacket() {
        // Used by reflection
    }

    public LittlePlacePacket(ItemStack stack, LittleTileBlockPos pos, boolean customPlacement) {
        this.stack = stack;
        this.pos = pos;
        this.customPlacement = customPlacement;
    }

    public ItemStack stack;
    public LittleTileBlockPos pos;
    public boolean customPlacement;

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
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        if (player.getDistance(pos.getPosX(), pos.getPosY(), pos.getPosZ()) > MAX_PLACEMENT_DISTANCE) {
            return;
        }

        ItemStack heldStack = player.inventory.getCurrentItem();
        ItemStack placementStack = getPlacementStack(heldStack, stack);
        if (placementStack != null) {
            LittleTilePlaceMode serverPlaceMode = heldStack.getItem() == LittleTiles.chisel
                    ? new LittleToolHandler(heldStack).getPlaceMode()
                    : LittleTilePlaceMode.NORMAL;
            ((ItemBlockTiles) Item.getItemFromBlock(LittleTiles.blockTile))
                    .placeBlockAt(player, placementStack, player.worldObj, pos, customPlacement, serverPlaceMode);

            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            Slot slot = playerMP.openContainer.getSlotFromInventory(playerMP.inventory, playerMP.inventory.currentItem);
            playerMP.playerNetServerHandler.sendPacket(
                    new S2FPacketSetSlot(
                            playerMP.openContainer.windowId,
                            slot.slotNumber,
                            playerMP.inventory.getCurrentItem()));

        }
    }

    /** Resolve placement content without trusting client-supplied tile or structure data. */
    private static ItemStack getPlacementStack(ItemStack heldStack, ItemStack requestedStack) {
        if (heldStack == null || heldStack.stackSize <= 0) return null;
        if (heldStack.getItem() != LittleTiles.chisel) {
            return PlacementHelper.isLittleBlock(heldStack) ? heldStack.copy() : null;
        }

        // A chisel sends a temporary tile, not the held tool. Only its geometry comes from the client.
        if (requestedStack == null || requestedStack.getItem() != Item.getItemFromBlock(LittleTiles.blockTile)
                || !requestedStack.hasTagCompound())
            return null;
        NBTTagCompound requested = requestedStack.getTagCompound();
        LittleTileSize size = new LittleTileSize("size", requested);
        if (size.sizeX <= 0 || size.sizeY <= 0 || size.sizeZ <= 0) return null;
        int align = requested.getInteger("fromChiselAlign");

        if (!Arrays.asList(1, 2, 4, 8, 16).contains(align)) {
            return null;
        }

        LittleToolHandler handler = new LittleToolHandler(heldStack);
        if (!BlockValidator.isBlockValid(handler.getBlock())) return null;
        NBTTagCompound tag = new NBTTagCompound();
        new LittleTileBlock(handler.getBlock(), handler.getMeta()).saveTileForItem(tag);
        size.writeToNBT("size", tag);
        tag.setBoolean("fromChiselPosX", requested.getBoolean("fromChiselPosX"));
        tag.setBoolean("fromChiselPosY", requested.getBoolean("fromChiselPosY"));
        tag.setBoolean("fromChiselPosZ", requested.getBoolean("fromChiselPosZ"));
        tag.setInteger("fromChiselAlign", align);

        if (requested.hasKey("cutoutType")) {
            LittleTileCutoutInfo cutout = LittleTileCutoutInfo.loadFromNBT(requested);
            if (cutout == null || cutout.orientation < 0 || cutout.orientation >= 48) return null;
            cutout.writeToNBT(tag);
        }

        ItemStack result = new ItemStack(LittleTiles.blockTile);
        result.setTagCompound(tag);
        return result;
    }

}
