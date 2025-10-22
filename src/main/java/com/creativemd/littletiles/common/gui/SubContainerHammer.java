package com.creativemd.littletiles.common.gui;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.container.SubContainer;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.items.ItemTileContainer;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockColored;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

public class SubContainerHammer extends SubContainer {

    public SubContainerHammer(EntityPlayer player) {
        super(player, true);
    }

    public InventoryBasic basic = new InventoryBasic("Hammer", false, 1);

    @Override
    public void onGuiPacket(int control, NBTTagCompound value, EntityPlayer player) {
        if (control == 6) {
            LittleTileSize size = new LittleTileSize(
                    value.getByte("sizeX"),
                    value.getByte("sizeY"),
                    value.getByte("sizeZ"));
            ItemStack stack = basic.getStackInSlot(0);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = Block.getBlockFromItem(stack.getItem());
                if (BlockValidator.isBlockValid(block)) {
                    int alltiles = (int) (1 / size.getPercentVolume() * stack.stackSize);
                    int tiles = Math.min(alltiles, 64);
                    int blocks = (int) Math.ceil(tiles * size.getPercentVolume());
                    stack.stackSize -= blocks;
                    if (stack.stackSize <= 0) basic.setInventorySlotContents(0, null);
                    if (block.hasTileEntity(stack.getItemDamage())) return;
                    // LittleTile tile = new LittleTile(block, stack.getItemDamage(), size);
                    ItemStack dropstack = new ItemStack(LittleTiles.blockTile);
                    dropstack.stackSize = tiles;
                    dropstack.stackTagCompound = new NBTTagCompound();
                    size.writeToNBT("size", dropstack.stackTagCompound);
                    LittleTile tile;
                    if (value.hasKey("color")) tile = new LittleTileBlockColored(
                            block,
                            stack.getItemDamage(),
                            ColorUtils.IntToRGB(value.getInteger("color")));
                    else tile = new LittleTileBlock(block, stack.getItemDamage());
                    tile.saveTile(dropstack.stackTagCompound);

                    float missingTiles = blocks - tiles * size.getPercentVolume();
                    if (missingTiles > 0)
                        ItemTileContainer.addBlock(player, block, stack.getItemDamage(), missingTiles);
                    // dropstack.stackTagCompound.setString("block", Block.blockRegistry.getNameForObject(block));
                    // dropstack.stackTagCompound.setInteger("meta", stack.getItemDamage());
                    // ItemBlockTiles.saveLittleTile(player.worldObj, dropstack, tile);
                    player.inventory.addItemStackToInventory(dropstack);
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        if (basic.getStackInSlot(0) != null) {
            player.dropPlayerItemWithRandomChoice(basic.getStackInSlot(0), false);
        }
    }

    @Override
    public void createControls() {
        addSlotToContainer(new Slot(basic, 0, 10, 10));
        addPlayerSlotsToContainer(player, 20, 120);
    }

    @Override
    public void sendUpdate() {

    }

}
