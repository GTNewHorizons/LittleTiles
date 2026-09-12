package com.creativemd.littletiles.common.packet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.utils.LittleToolHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittleCursorBlockUpdatePacket extends CreativeCorePacket {

    public int blockId;
    public int meta;

    public LittleCursorBlockUpdatePacket() {

    }

    public LittleCursorBlockUpdatePacket(final int blockId, final int meta) {
        this.blockId = blockId;
        this.meta = meta;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(blockId);
        buf.writeInt(meta);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        blockId = buf.readInt();
        meta = buf.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {

        ItemStack cursorStack = player.inventory.getItemStack();
        if (cursorStack == null || cursorStack.getItem() != LittleTiles.chisel) {
            return;
        }

        Block block = Block.getBlockById(blockId);
        if (!BlockValidator.isBlockValid(block)) {
            return;
        }

        new LittleToolHandler(cursorStack).setBlock(block, meta);
    }

}
