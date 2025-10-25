package com.creativemd.littletiles.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.creativemd.littletiles.common.utils.LittleToolHandler;

public class BlockOverlayRenderer implements IItemRenderer {

    private final RenderItem renderItem = new RenderItem();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        LittleToolHandler handler = new LittleToolHandler(stack);

        renderItem.renderItemIntoGUI(
                Minecraft.getMinecraft().fontRenderer,
                Minecraft.getMinecraft().renderEngine,
                stack,
                0,
                0,
                false);

        ItemStack stackBlock = handler.getStack();
        GL11.glPushMatrix();
        float scale = 0.75F;
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glTranslatef(16F * (1F - scale), 16F * (1F - scale), 0F);
        renderItem.renderItemIntoGUI(
                Minecraft.getMinecraft().fontRenderer,
                Minecraft.getMinecraft().renderEngine,
                stackBlock,
                0,
                0,
                false);
        GL11.glPopMatrix();
    }
}
