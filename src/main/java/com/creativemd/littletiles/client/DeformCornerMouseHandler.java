package com.creativemd.littletiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
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
public class DeformCornerMouseHandler {

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

        // Same ray the tile raytrace in TileEntityLittleTiles uses - getPosition already accounts for eye height.
        double reach = mc.playerController.getBlockReachDistance();
        Vec3 start = player.getPosition(1);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        LittleDeformedBoxHelper.toggleMarkedCorner(LittleDeformedBoxHelper.pickCorner(start, end, handler.getGrid()));
        event.setCanceled(true);
    }
}
