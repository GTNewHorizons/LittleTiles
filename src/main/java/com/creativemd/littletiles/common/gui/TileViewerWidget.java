package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.creativemd.creativecore.client.rendering.RenderHelper3D;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.client.render.LittleTilesBlockRenderHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

/**
 * Shows the tiles of a recipe, optionally with the rotation axis of a door. Drag to move, scroll or +/- to zoom.
 */
public class TileViewerWidget extends Widget<TileViewerWidget> implements Interactable {

    public ItemStack stack;

    public float scale = 1;
    public float offsetX = 0;
    public float offsetY = 0;

    public ForgeDirection viewDirection = ForgeDirection.EAST;

    public boolean visibleAxis = false;

    public Axis normalAxis = null;
    public Axis axisDirection = Axis.AxisY;
    public int axisX = 0;
    public int axisY = 0;
    public int axisZ = 0;

    private boolean grabbed = false;
    private int lastMouseX;
    private int lastMouseY;

    public TileViewerWidget(ItemStack stack) {
        this.stack = stack;
        updateNormalAxis();
    }

    public void updateNormalAxis() {
        ArrayList<CubeObject> cubes = ((ITilesRenderer) stack.getItem()).getRenderingCubes(stack);
        double minX = Integer.MAX_VALUE;
        double minY = Integer.MAX_VALUE;
        double minZ = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double maxY = Integer.MIN_VALUE;
        double maxZ = Integer.MIN_VALUE;

        for (CubeObject cube : cubes) {
            minX = Math.min(minX, cube.minX);
            minY = Math.min(minY, cube.minY);
            minZ = Math.min(minZ, cube.minZ);
            maxX = Math.max(maxX, cube.maxX);
            maxY = Math.max(maxY, cube.maxY);
            maxZ = Math.max(maxZ, cube.maxZ);
        }

        double sizeX = maxX - minX;
        double sizeY = maxY - minY;
        double sizeZ = maxZ - minZ;

        switch (axisDirection) {
            case AxisX:
                if (sizeY >= sizeZ) normalAxis = Axis.AxisZ;
                else normalAxis = Axis.AxisY;
                break;
            case AxisY:
                if (sizeX >= sizeZ) normalAxis = Axis.AxisZ;
                else normalAxis = Axis.AxisX;
                break;
            case AxisZ:
                if (sizeX >= sizeY) normalAxis = Axis.AxisY;
                else normalAxis = Axis.AxisX;
                break;
            default:
                break;
        }
    }

    public void changeNormalAxis() {
        switch (axisDirection) {
            case AxisX:
                if (normalAxis == Axis.AxisZ) normalAxis = Axis.AxisY;
                else normalAxis = Axis.AxisZ;
                break;
            case AxisY:
                if (normalAxis == Axis.AxisZ) normalAxis = Axis.AxisX;
                else normalAxis = Axis.AxisZ;
                break;
            case AxisZ:
                if (normalAxis == Axis.AxisY) normalAxis = Axis.AxisX;
                else normalAxis = Axis.AxisY;
                break;
            default:
                break;
        }
    }

    public void updateViewDirection() {
        switch (axisDirection) {
            case AxisX:
                viewDirection = Axis.AxisZ.getDirection();
                break;
            case AxisY:
                viewDirection = Axis.AxisY.getDirection();
                break;
            case AxisZ:
                viewDirection = Axis.AxisX.getDirection();
                break;
            default:
                break;
        }
    }

    public void resetView() {
        offsetX = 0;
        offsetY = 0;
        scale = 1;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int width = getArea().width;
        int height = getArea().height;
        GuiDraw.drawRect(0, 0, width, height, 0xFF000000);
        GuiDraw.drawRect(1, 1, width - 2, height - 2, 0xFFFFFFFF);

        Stencil.apply(1, 1, width - 2, height - 2, context);
        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT
                        | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_CURRENT_BIT
                        | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        // In front of the panel, the tiles need the depth test to hide their back faces.
        GL11.glTranslatef(width / 2F - offsetX, height / 2F - offsetY, 150);
        GL11.glScaled(4, 4, 4);
        GL11.glScaled(scale, scale, scale);
        GL11.glTranslated(offsetX * 2, offsetY * 2, 0);

        // Stencil.apply draws its mask with texturing disabled and does not enable it again.
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.5F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        if (viewDirection == ForgeDirection.UP) GL11.glRotated(-90, 1, 0, 0);
        else if (viewDirection == ForgeDirection.DOWN) GL11.glRotated(90, 1, 0, 0);
        else RenderHelper3D.applyDirection(viewDirection);

        GL11.glTranslatef(-2, 3, -3.0F);
        GL11.glScalef(10.0F, 10.0F, 10.0F);
        GL11.glTranslatef(1.0F, 0.5F, 1.0F);
        GL11.glScalef(1.0F, 1.0F, -1.0F);
        GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
        GL11.glEnable(GL11.GL_LIGHTING);

        ArrayList<CubeObject> cubes = ((ITilesRenderer) stack.getItem()).getRenderingCubes(stack);
        if (visibleAxis) {
            cubes.add(createAxisCube(normalAxis, 0));
            cubes.add(createAxisCube(axisDirection, 5));
        }
        LittleTilesBlockRenderHelper.renderInventoryCubes(
                RenderHelper3D.renderBlocks,
                cubes,
                Block.getBlockFromItem(stack.getItem()),
                stack.getItemDamage());

        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        Stencil.remove();
    }

    /** A line through the axis tile along {@code axis}, in the wool color {@code meta}. */
    private CubeObject createAxisCube(Axis axis, int meta) {
        double min = -10d / scale;
        double max = -min;
        CubeObject cube = new LittleTileBox(axisX, axisY, axisZ, axisX + 1, axisY + 1, axisZ + 1).getCube();
        cube.block = Blocks.wool;
        cube.meta = meta;
        switch (axis) {
            case AxisX:
                cube.minX = min;
                cube.maxX = max;
                break;
            case AxisY:
                cube.minY = min;
                cube.maxY = max;
                break;
            case AxisZ:
                cube.minZ = min;
                cube.maxZ = max;
                break;
            default:
                break;
        }
        return cube;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (scrollDirection.isUp()) scale *= 1.5F;
        else scale /= 1.5F;
        return true;
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        grabbed = true;
        lastMouseX = getContext().getAbsMouseX();
        lastMouseY = getContext().getAbsMouseY();
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (!grabbed) return;
        int mouseX = getContext().getAbsMouseX();
        int mouseY = getContext().getAbsMouseY();
        double percent = 0.3;
        offsetX += (float) (1 / scale * (lastMouseX - mouseX) * percent);
        offsetY += (float) (1 / scale * (lastMouseY - mouseY) * percent);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        grabbed = false;
        return true;
    }

    @Override
    public Result onKeyPressed(char character, int keyCode) {
        int amount = 5;
        switch (keyCode) {
            case Keyboard.KEY_ADD:
                scale *= 2;
                return Result.SUCCESS;
            case Keyboard.KEY_SUBTRACT:
                scale /= 2;
                return Result.SUCCESS;
            case Keyboard.KEY_UP:
                offsetY += amount;
                return Result.SUCCESS;
            case Keyboard.KEY_DOWN:
                offsetY -= amount;
                return Result.SUCCESS;
            case Keyboard.KEY_RIGHT:
                offsetX -= amount;
                return Result.SUCCESS;
            case Keyboard.KEY_LEFT:
                offsetX += amount;
                return Result.SUCCESS;
            default:
                return Result.IGNORE;
        }
    }
}
