package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.container.SubContainer;
import com.creativemd.creativecore.common.utils.WorldUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.items.ItemRecipe;
import com.creativemd.littletiles.common.items.ItemTileContainer;
import com.creativemd.littletiles.common.items.ItemTileContainer.BlockEntry;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.PlacementHelper;

public class SubContainerWrench extends SubContainer {

    public InventoryBasic basic = new InventoryBasic("default", false, 2);

    public SubContainerWrench(EntityPlayer player) {
        super(player, true);
    }

    @Override
    public void createControls() {
        addSlotToContainer(new Slot(basic, 0, 10, 5));
        addSlotToContainer(new Slot(basic, 1, 40, 5));

        addPlayerSlotsToContainer(player);
    }

    public static ArrayList<BlockEntry> getMissing(ArrayList<LittleTilePreview> tiles, ArrayList<BlockEntry> entries) {
        ArrayList<BlockEntry> missing = new ArrayList<>();
        for (LittleTilePreview tile : tiles) {
            Block blockofTile = Block.getBlockFromName(tile.nbt.getString("block"));
            int meta = tile.nbt.getInteger("meta");
            float size = tile.size.getPercentVolume();
            int j = 0;
            boolean found = false;
            while (j < entries.size()) {
                if (blockofTile == entries.get(j).block && meta == entries.get(j).meta) {
                    float amount = Math.min(entries.get(j).value, size);
                    entries.get(j).value -= amount;
                    size -= amount;

                    if (entries.get(j).value <= 0) {
                        entries.remove(j);
                        if (size > 0) continue;
                    }

                    if (size <= 0) {
                        found = true;
                        break;
                    }
                }
                j++;
            }
            if (!found) {
                BlockEntry entry = new BlockEntry(blockofTile, meta, size);
                int index = missing.indexOf(entry);
                if (index == -1) missing.add(entry);
                else missing.get(index).value += size;
            }
        }
        return missing;
    }

    public static ArrayList<BlockEntry> getContentofStack(ItemStack stack) {
        ArrayList<BlockEntry> entries = new ArrayList<>();
        if (stack != null) {
            ILittleTile tile = PlacementHelper.getLittleInterface(stack);

            if (tile != null) {
                ArrayList<LittleTilePreview> tiles = tile.getLittlePreview(stack);
                if (tiles != null) {
                    for (LittleTilePreview littleTilePreview : tiles) {
                        if (littleTilePreview.nbt.hasKey("block")) {
                            Block block2 = Block.getBlockFromName(littleTilePreview.nbt.getString("block"));
                            if (block2 != null && !(block2 instanceof BlockAir)) {
                                entries.add(
                                        new BlockEntry(
                                                block2,
                                                littleTilePreview.nbt.getInteger("meta"),
                                                littleTilePreview.size.getPercentVolume()));
                                // ItemTileContainer.addBlock(stack, block2, );
                            }
                        }
                    }

                }
            }

            if (stack.getItem() instanceof ItemTileContainer) {
                entries.addAll(ItemTileContainer.loadMap(stack));
            }

            Block block = Block.getBlockFromItem(stack.getItem());

            if (block != null && !(block instanceof BlockAir)) {
                if (BlockValidator.isBlockValid(block)) {
                    entries.add(new BlockEntry(block, stack.getItemDamage(), 1));
                }
            }
        }
        return entries;
    }

    @Override
    public void onGuiPacket(int controlID, NBTTagCompound nbt, EntityPlayer player) {
        if (controlID == 0) {
            ItemStack stack1 = basic.getStackInSlot(0);
            ItemStack stack2 = basic.getStackInSlot(1);
            if (stack1 != null) {
                if (stack1.getItem() instanceof ItemRecipe) {
                    if (stack1.stackTagCompound != null && !stack1.stackTagCompound.hasKey("x")) {
                        boolean enough = true;

                        ArrayList<BlockEntry> entries = getContentofStack(stack2);
                        ArrayList<LittleTilePreview> tiles = ItemRecipe.getPreview(stack1);

                        if (!player.capabilities.isCreativeMode) {
                            enough = getMissing(tiles, entries).size() == 0;
                        }

                        if (enough) {
                            if (stack2 != null && !player.capabilities.isCreativeMode) {
                                if (stack2.getItem() instanceof ItemTileContainer) {
                                    ItemTileContainer.saveMap(stack2, entries);
                                } else {
                                    stack2.stackSize--;
                                    if (stack2.stackSize == 0) basic.setInventorySlotContents(1, null);
                                    if (entries.size() > 0) {
                                        for (BlockEntry entry : entries) {
                                            ItemTileContainer.addBlock(player, entry.block, entry.meta, entry.value);
                                        }
                                    }
                                }
                            }

                            ItemStack stack = new ItemStack(LittleTiles.multiTiles);
                            stack.stackTagCompound = (NBTTagCompound) stack1.stackTagCompound.copy();
                            if (!player.inventory.addItemStackToInventory(stack)) WorldUtils.dropItem(player, stack);
                        }
                    }
                } else {
                    ILittleTile tile = PlacementHelper.getLittleInterface(stack1);
                    if (tile != null && stack2 != null && stack2.getItem() instanceof ItemRecipe) {
                        stack2.stackTagCompound = (NBTTagCompound) stack1.stackTagCompound.copy();
                    }
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        for (int i = 0; i < basic.getSizeInventory(); i++) {
            if (basic.getStackInSlot(i) != null) player.dropPlayerItemWithRandomChoice(basic.getStackInSlot(i), false);
        }
    }

}
