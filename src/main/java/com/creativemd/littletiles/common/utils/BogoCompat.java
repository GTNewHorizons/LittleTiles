package com.creativemd.littletiles.common.utils;

import net.minecraft.item.ItemStack;

import com.cleanroommc.bogosorter.api.BeforeSortEvent;
import com.cleanroommc.bogosorter.client.keybinds.control.BSKeybinds;
import com.creativemd.littletiles.common.items.ItemLittleChisel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BogoCompat {

    private static final int MOUSE_MIDDLE = -98;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onBeforeSortInGui(BeforeSortEvent.BeforeSortInGuiEvent event) {
        // Cancel Bogo inGUI inventory sort if Little Chisel is picked up.
        // Bogo sort is usually bound to middle-click.
        // This is just to ease default player experience.
        if (!event.isFromKeybind()) return;
        if (BSKeybinds.sortKeyInGUI.getKeyCode() != MOUSE_MIDDLE) return;

        ItemStack item = event.getPlayer().inventory.getItemStack();
        if (item == null || !(item.getItem() instanceof ItemLittleChisel)) return;

        // Cancel sort
        event.setCanceled(true);
    }
}
