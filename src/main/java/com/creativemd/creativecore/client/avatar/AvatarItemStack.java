package com.creativemd.creativecore.client.avatar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.rendering.RenderHelper2D;

public class AvatarItemStack extends Avatar {

    public ItemStack stack;

    public AvatarItemStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void handleRendering(Minecraft mc, FontRenderer fontRenderer, int width, int height) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        // GL11.glTranslated(8, 8, 0);
        GL11.glScaled(width / 16D, height / 16D, 0);
        GL11.glTranslated(-width / 2D, -height / 2D, 0);
        RenderHelper2D.renderItem(stack, width / 2, height / 2, 1, 1, 16, 16);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
}
