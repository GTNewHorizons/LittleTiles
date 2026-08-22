package com.creativemd.littletiles.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
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
        renderItem.renderItemIntoGUI(
                Minecraft.getMinecraft().fontRenderer,
                Minecraft.getMinecraft().renderEngine,
                stack,
                0,
                0,
                false);

        LittleToolHandler handler = new LittleToolHandler(stack);
        LittleTileShapeMode shape = handler.getShape();

        if (shape == LittleTileShapeMode.BOX) {
            // A box is just a plain block; keep the previous full-block icon.
            renderBlockAsIcon(handler);
        } else if (!renderShapeAsIcon(handler, shape)) {
            // Fall back to the full-block icon if the shape mesh could not be produced.
            renderBlockAsIcon(handler);
        }
    }

    private void renderBlockAsIcon(LittleToolHandler handler) {
        GL11.glPushMatrix();
        float scale = 0.75F;
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glTranslatef(16F * (1F - scale), 16F * (1F - scale), 0F);
        renderItem.renderItemIntoGUI(
                Minecraft.getMinecraft().fontRenderer,
                Minecraft.getMinecraft().renderEngine,
                handler.getStack(),
                0,
                0,
                false);
        GL11.glPopMatrix();
    }

    /**
     * Draws the selected cutout shape as a textured, isometric 3D icon
     *
     * @return whether the shape was successfully rendered
     */
    private boolean renderShapeAsIcon(LittleToolHandler handler, LittleTileShapeMode shape) {
        Block block = handler.getBlock();
        int meta = handler.getMeta();

        LittleTileCutoutInfo cutoutInfo = new LittleTileCutoutInfo();
        cutoutInfo.type = shape;
        cutoutInfo.size = new Vector3i(16, 16, 16);

        Mesh3d mesh;
        try {
            mesh = Mesh3dUtil.createMesh(
                    cutoutInfo,
                    new Vector3d(1, 1, 1),
                    new Vector3d(),
                    new Vector3i(),
                    new Vector3i(),
                    new Vector3i(16, 16, 16),
                    null,
                    0,
                    0);
            if (mesh == null || mesh.getTriangles().isEmpty()) {
                return false;
            }
            mesh.setTextures(block, meta);
        } catch (Exception ignored) {
            return false;
        }

        boolean alphaTestWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        GL11.glPushMatrix();
        float scale = 0.75F;
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glTranslatef(16F * (1F - scale), 16F * (1F - scale), 0F);
        GL11.glTranslatef(-2.0F, 3.0F, -3.0F);
        GL11.glScalef(10.0F, 10.0F, 10.0F);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mesh.renderIcon();
        if (!alphaTestWasEnabled) {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        }
        GL11.glPopMatrix();

        return true;
    }
}
