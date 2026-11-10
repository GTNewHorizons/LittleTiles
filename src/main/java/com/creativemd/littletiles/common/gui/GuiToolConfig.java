package com.creativemd.littletiles.common.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.items.ItemRecipe;

public class GuiToolConfig {

    public static void show(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item == LittleTiles.chisel || (item == LittleTiles.recipe && ItemRecipe.hasTiles(itemStack))) {
            EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
            GuiManager.openFromClient(
                    GuiFactories.playerInventory(),
                    new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, player.inventory.currentItem));
        }

    }
}
