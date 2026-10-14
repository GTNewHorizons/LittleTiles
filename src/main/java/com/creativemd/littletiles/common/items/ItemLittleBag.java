package com.creativemd.littletiles.common.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.gui.bag.LittleBagGui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A bag which melts everything that is put into it down into the materials it is made of, see
 * {@link com.creativemd.littletiles.common.bag.LittleBagItemHandler}.
 */
public class ItemLittleBag extends Item implements IGuiHolder<PlayerInventoryGuiData> {

    private static final String CAPACITY_TAG = "capacity";
    /** Capacity of a bag that was made before bags had one, the size of a chest. */
    public static final int DEFAULT_CAPACITY = 27;

    public ItemLittleBag() {
        setMaxStackSize(1);
    }

    /** Creates an empty bag which holds as many tiles as the given amount of stacks of blocks. */
    public static ItemStack create(int capacity) {
        ItemStack stack = new ItemStack(LittleTiles.littleBag);
        stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger(CAPACITY_TAG, capacity);
        return stack;
    }

    /** How many stacks of blocks the given bag holds. */
    public static int getCapacity(ItemStack stack) {
        if (stack.stackTagCompound == null || !stack.stackTagCompound.hasKey(CAPACITY_TAG)) return DEFAULT_CAPACITY;
        return stack.stackTagCompound.getInteger(CAPACITY_TAG);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(LittleTiles.modid, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return LittleBagGui.build(data, syncManager);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            GuiFactories.playerInventory().openFromMainHand(player);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTLittleBag";
    }
}
