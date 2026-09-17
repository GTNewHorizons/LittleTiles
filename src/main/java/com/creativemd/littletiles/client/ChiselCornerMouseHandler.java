package com.creativemd.littletiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.LittleDeformedBoxHelper;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.utils.LittleToolHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Selects chisel corners with left click.
 * <p>
 * 1.7.10 has no hook for "left clicked while holding this item" - left click is attack/break - so the raw mouse event
 * is intercepted and cancelled instead.
 */
@SideOnly(Side.CLIENT)
public class ChiselCornerMouseHandler {

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button != 0 || !event.buttonstate) {
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
        boolean editingDeformedBox = handler.isDeformedBoxShape() && LittleDeformedBoxHelper.isEditing();
        if (!editingDeformedBox && !PreviewRenderer.hasTwoHitCorners()) {
            return;
        }

        // Same ray the tile raytrace in TileEntityLittleTiles uses - getPosition already accounts for eye height.
        double reach = mc.playerController.getBlockReachDistance();
        Vec3 start = player.getPosition(1);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        if (editingDeformedBox) {
            LittleDeformedBoxHelper
                    .toggleMarkedCorner(LittleDeformedBoxHelper.pickCorner(start, end, handler.getGrid()));
        } else {
            PreviewRenderer.selectTwoHitCorner(start, end, handler.getGrid());
        }
        // Swinging at the world is unwanted for the whole time corners are being edited, so a miss is consumed too.
        event.setCanceled(true);
    }
}
