package com.creativemd.littletiles.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.container.SubContainer;

public class SubContainerStructure extends SubContainer {

    public ItemStack stack;
    public int index;

    public SubContainerStructure(EntityPlayer player, ItemStack stack) {
        super(player);
        this.stack = stack;
        this.index = player.inventory.currentItem;
    }

    @Override
    public void createControls() {

    }

    @Override
    public void onGuiPacket(int controlID, NBTTagCompound nbt, EntityPlayer player) {
        if (controlID == 0) {
            if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
            if (nbt != null && nbt.hasKey("structure")) {
                NBTTagCompound structure = nbt.getCompoundTag("structure");
                structure.removeTag("stack");
                stack.stackTagCompound.setTag("structure", structure);
            } else stack.stackTagCompound.removeTag("structure");
            player.inventory.mainInventory[index] = stack;
            // player.inventory.
            /*
             * if(player.capabilities.isCreativeMode) { ItemStack multiTiles = new ItemStack(LittleTiles.multiTiles);
             * multiTiles.stackTagCompound = nbt; WorldUtils.dropItem(player, multiTiles); }
             */
            // closeLayer(null);
        }
    }

}
