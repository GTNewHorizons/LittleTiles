package com.creativemd.littletiles.common.packet;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.utils.LittleToolHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittleCursorItemUpdatePacket extends CreativeCorePacket {

    public ItemStack itemStack;

    public LittleCursorItemUpdatePacket() {

    }

    public LittleCursorItemUpdatePacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        writeItemStack(buf, itemStack);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        itemStack = readItemStack(buf);
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

        Block block = Block.getBlockFromItem(itemStack.getItem());
        if (!BlockValidator.isBlockValid(block)) {
            return;
        }

        new LittleToolHandler(cursorStack)
                .setBlock(block, ((ItemBlock) itemStack.getItem()).getMetadata(itemStack.getItemDamage()));
    }

}
