package com.creativemd.littletiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.LittleDeformedBoxHelper;
import com.creativemd.littletiles.common.utils.LittleToolHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Selects the corners of a deformed box with left click, the way the glove does in modern LittleTiles.
 * <p>
 * 1.7.10 has no hook for "left clicked while holding this item" - left click is attack/break - so the raw mouse event
 * is intercepted and cancelled instead. This only happens while a deformed box is actually being edited, which is
 * exactly when swinging at the world would be unwanted anyway.
 */
@SideOnly(Side.CLIENT)
public class ChiselCornerMouseHandler {

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button != 0 || !event.buttonstate || !LittleDeformedBoxHelper.isEditing()) {
            return;
        }

        final Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || !mc.inGameHasFocus) {
            return;
        }

        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != LittleTiles.chisel) {
            return;
        }
        LittleToolHandler handler = new LittleToolHandler(held);
        if (!handler.isDeformedBoxShape()) {
            return;
        }

        int corner = LittleDeformedBoxHelper.pickLookedAtCorner(player, handler.getGrid());
        LittleDeformedBoxHelper.toggleMarkedCorner(corner);
        event.setCanceled(true);
    }
}
