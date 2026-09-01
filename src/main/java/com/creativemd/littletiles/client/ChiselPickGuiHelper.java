package com.creativemd.littletiles.client;

import java.lang.reflect.Method;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.Loader;

public final class ChiselPickGuiHelper {

    private static Method NEI_GET_STACK_MOUSE_OVER;

    static {
        if (Loader.isModLoaded("NotEnoughItems")) {
            try {
                Class<?> manager = Class.forName("codechicken.nei.guihook.GuiContainerManager");
                NEI_GET_STACK_MOUSE_OVER = manager.getMethod("getStackMouseOver", GuiContainer.class);
            } catch (Throwable ignored) {
                NEI_GET_STACK_MOUSE_OVER = null;
            }
        }
    }

    private ChiselPickGuiHelper() {}

    // Returns the ItemStack under the mouse as resolved by NEI or
    // null when NEI is absent or has nothing under the cursor.
    public static ItemStack getNEIStackMouseOver(GuiContainer gui) {
        if (NEI_GET_STACK_MOUSE_OVER != null) {
            try {
                return (ItemStack) NEI_GET_STACK_MOUSE_OVER.invoke(null, gui);
            } catch (Throwable ignored) {
                // fall through
            }
        }
        return null;
    }
}
