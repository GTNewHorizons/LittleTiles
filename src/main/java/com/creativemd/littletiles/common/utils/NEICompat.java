package com.creativemd.littletiles.common.utils;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.guihook.GuiContainerManager;

public class NEICompat {

    public ItemStack getStackMouseOver(GuiContainer gui) {
        return GuiContainerManager.getStackMouseOver(gui);
    }

    public boolean isNEIHidden() {
        return NEIClientConfig.isHidden();
    }
}
