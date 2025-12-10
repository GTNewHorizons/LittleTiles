package com.creativemd.littletiles.client.render;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.rendering.RenderHelper3D;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.common.gui.GuiToolConfig;
import com.creativemd.littletiles.common.packet.LittleFlipPacket;
import com.creativemd.littletiles.common.packet.LittleRotatePacket;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.utils.PreviewTile;
import com.creativemd.littletiles.utils.ShiftHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PreviewRenderer {

    public static Minecraft mc = Minecraft.getMinecraft();

    public void processKey(ForgeDirection direction) {
        LittleRotatePacket packet = new LittleRotatePacket(direction);
        packet.executeClient(mc.thePlayer);
        PacketHandler.sendPacketToServer(packet);
    }

    public static LittleTileBlockPos markedHit = null;
    public static LittleTileBlockPos firstHit = null;
    private static ItemStack lastItem = null;

    private static ForgeDirection rotateDirection(ForgeDirection direction) {
        switch (direction) {
            case NORTH:
                return ForgeDirection.EAST;
            case EAST:
                return ForgeDirection.SOUTH;
            case SOUTH:
                return ForgeDirection.WEST;
            case WEST:
                return ForgeDirection.NORTH;
        }
        return ForgeDirection.UNKNOWN;
    }

    public static void moveMarkedHit(ForgeDirection direction, ForgeDirection direction_look, int amount) {
        if (direction != ForgeDirection.UP && direction != ForgeDirection.DOWN) {
            if (direction_look == ForgeDirection.EAST) {
                direction = rotateDirection(direction);
            }
            if (direction_look == ForgeDirection.SOUTH) {
                direction = rotateDirection(direction);
                direction = rotateDirection(direction);
            }
            if (direction_look == ForgeDirection.WEST) {
                direction = rotateDirection(direction);
                direction = rotateDirection(direction);
                direction = rotateDirection(direction);
            }
        }

        if (GuiScreen.isCtrlKeyDown()) amount = 16;
        markedHit.moveInDirection(direction, amount);
    }

    @SubscribeEvent
    public void tick(RenderHandEvent event) {
        if (mc.thePlayer != null && mc.inGameHasFocus) {

            if (!ItemStack.areItemStackTagsEqual(lastItem, mc.thePlayer.getHeldItem())) {
                markedHit = null;
                firstHit = null;
            }
            lastItem = mc.thePlayer.getHeldItem();

            if (mc.thePlayer.getHeldItem() != null) {
                if (GameSettings.isKeyDown(LittleTilesClient.toolConfig) && !LittleTilesClient.pressedToolConfig) {
                    LittleTilesClient.pressedToolConfig = true;
                    GuiToolConfig.show(mc.thePlayer.getHeldItem());
                } else if (!GameSettings.isKeyDown(LittleTilesClient.toolConfig)) {
                    LittleTilesClient.pressedToolConfig = false;
                }
            }
            if (PlacementHelper.isLittleBlock(mc.thePlayer.getHeldItem())) {
                int i4 = MathHelper.floor_double((double) (mc.thePlayer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                ForgeDirection direction_look = null;
                switch (i4) {
                    case 0:
                        direction_look = ForgeDirection.SOUTH;
                        break;
                    case 1:
                        direction_look = ForgeDirection.WEST;
                        break;
                    case 2:
                        direction_look = ForgeDirection.NORTH;
                        break;
                    case 3:
                        direction_look = ForgeDirection.EAST;
                        break;
                }
                if (GameSettings.isKeyDown(LittleTilesClient.flip) && !LittleTilesClient.pressedFlip) {
                    LittleTilesClient.pressedFlip = true;

                    ForgeDirection direction = direction_look;
                    if (mc.thePlayer.rotationPitch > 45) direction = ForgeDirection.DOWN;
                    if (mc.thePlayer.rotationPitch < -45) direction = ForgeDirection.UP;
                    LittleFlipPacket packet = new LittleFlipPacket(direction);
                    packet.executeClient(mc.thePlayer);
                    PacketHandler.sendPacketToServer(packet);
                } else if (!GameSettings.isKeyDown(LittleTilesClient.flip)) {
                    LittleTilesClient.pressedFlip = false;
                }

                MovingObjectPosition look = mc.objectMouseOver;
                PlacementHelper helper = PlacementHelper.getInstance(mc.thePlayer);
                LittleTileBlockPos pos = null;
                int align = 1;
                if (mc.thePlayer.getHeldItem().getItem() == LittleTiles.chisel) {
                    align = new LittleToolHandler(mc.thePlayer.getHeldItem()).getGrid();
                }
                if (look != null && look.typeOfHit == MovingObjectType.BLOCK) {
                    pos = LittleTileBlockPos.fromMovingObjectPosition(look, align);
                }

                if (markedHit != null) pos = markedHit;

                if (pos != null && mc.thePlayer.getHeldItem() != null) {
                    if (GameSettings.isKeyDown(LittleTilesClient.mark) && !LittleTilesClient.pressedMark) {
                        LittleTilesClient.pressedMark = true;
                        if (markedHit == null) {
                            markedHit = pos;
                            return;
                        } else markedHit = null;
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.mark)) {
                        LittleTilesClient.pressedMark = false;
                    }

                    // Rotate Block
                    if (GameSettings.isKeyDown(LittleTilesClient.up) && !LittleTilesClient.pressedUp) {
                        LittleTilesClient.pressedUp = true;
                        if (markedHit != null) moveMarkedHit(
                                mc.thePlayer.isSneaking() ? ForgeDirection.UP : ForgeDirection.NORTH,
                                direction_look,
                                align);
                        else processKey(ForgeDirection.UP);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.up)) LittleTilesClient.pressedUp = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.down) && !LittleTilesClient.pressedDown) {
                        LittleTilesClient.pressedDown = true;
                        if (markedHit != null) moveMarkedHit(
                                mc.thePlayer.isSneaking() ? ForgeDirection.DOWN : ForgeDirection.SOUTH,
                                direction_look,
                                align);
                        else processKey(ForgeDirection.DOWN);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.down)) LittleTilesClient.pressedDown = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.right) && !LittleTilesClient.pressedRight) {
                        LittleTilesClient.pressedRight = true;
                        if (markedHit != null) moveMarkedHit(ForgeDirection.EAST, direction_look, align);
                        else processKey(ForgeDirection.SOUTH);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.right)) LittleTilesClient.pressedRight = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.left) && !LittleTilesClient.pressedLeft) {
                        LittleTilesClient.pressedLeft = true;
                        if (markedHit != null) moveMarkedHit(ForgeDirection.WEST, direction_look, align);
                        else processKey(ForgeDirection.NORTH);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.left)) LittleTilesClient.pressedLeft = false;

                    GL11.glEnable(GL11.GL_BLEND);
                    OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
                    GL11.glLineWidth(2.0F);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDepthMask(false);

                    ArrayList<PreviewTile> previews;

                    previews = helper.getPreviewTiles(mc.thePlayer.getHeldItem(), pos, markedHit != null);

                    double x = (double) pos.getPosX() - TileEntityRendererDispatcher.staticPlayerX;
                    double y = (double) pos.getPosY() - TileEntityRendererDispatcher.staticPlayerY;
                    double z = (double) pos.getPosZ() - TileEntityRendererDispatcher.staticPlayerZ;
                    for (PreviewTile previewTile : previews) {
                        GL11.glPushMatrix();
                        LittleTileBox previewBox = previewTile.getPreviewBox();
                        CubeObject cube = previewBox.getCube();
                        Vec3 size = previewBox.getSizeD();
                        double cubeX = x + cube.minX + size.xCoord / 2D;
                        double cubeY = y + cube.minY + size.yCoord / 2D;
                        double cubeZ = z + cube.minZ + size.zCoord / 2D;
                        if (firstHit != null) {
                            LittleTileBlockPos.Comparison comparison = pos.compareTo(firstHit);
                            Vec3 hitVec = firstHit.toHitVec();
                            if (comparison.biggerOrEqualX) {
                                cubeX = -TileEntityRendererDispatcher.staticPlayerX + hitVec.xCoord + size.xCoord / 2D;
                            } else {
                                cubeX = -TileEntityRendererDispatcher.staticPlayerX + hitVec.xCoord
                                        - size.xCoord / 2D
                                        + align / 16f;
                            }
                            if (comparison.biggerOrEqualY) {
                                cubeY = -TileEntityRendererDispatcher.staticPlayerY + hitVec.yCoord + size.yCoord / 2D;
                            } else {
                                cubeY = -TileEntityRendererDispatcher.staticPlayerY + hitVec.yCoord
                                        - size.yCoord / 2D
                                        + align / 16f;
                            }

                            if (comparison.biggerOrEqualZ) {
                                cubeZ = -TileEntityRendererDispatcher.staticPlayerZ + hitVec.zCoord + size.zCoord / 2D;
                            } else {
                                cubeZ = -TileEntityRendererDispatcher.staticPlayerZ + hitVec.zCoord
                                        - size.zCoord / 2D
                                        + align / 16f;
                            }
                        }
                        Vec3 color = previewTile.getPreviewColor();

                        LittleToolHandler toolHandler = new LittleToolHandler(mc.thePlayer.getHeldItem());
                        LittleTileShapeMode shape = toolHandler.getShape();

                        // Needed for block picked cutouts
                        Vector3d cutoutSize = toolHandler.getTileSize();

                        if (shape == LittleTileShapeMode.SLOPE) {
                            cubeX -= size.xCoord / 2;
                            cubeY -= size.yCoord / 2;
                            cubeZ -= size.zCoord / 2;
                        }

                        if (cutoutSize == null) {
                            cutoutSize = new Vector3d(size.xCoord, size.yCoord, size.zCoord);
                        }
                        if (shape == LittleTileShapeMode.BOX) {
                            RenderHelper3D.renderBlock(
                                    cubeX,
                                    cubeY,
                                    cubeZ,
                                    size.xCoord,
                                    size.yCoord,
                                    size.zCoord,
                                    0,
                                    0,
                                    0,
                                    color.xCoord,
                                    color.yCoord,
                                    color.zCoord,
                                    Math.sin(System.nanoTime() / 200000000D) * 0.2 + 0.5);
                        } else {
                            LittleTilesBlockRenderHelper.renderMesh(
                                    cubeX,
                                    cubeY,
                                    cubeZ,
                                    cutoutSize,
                                    toolHandler.getOrientation(),
                                    color.xCoord,
                                    color.yCoord,
                                    color.zCoord,
                                    Math.sin(System.nanoTime() / 200000000D) * 0.2 + 0.5,
                                    toolHandler.getTileOriginal(), // Needed for block picked cutouts
                                    new Vector3i(),
                                    new Vector3i(
                                            (int) Math.round(size.xCoord * 16),
                                            (int) Math.round(size.yCoord * 16),
                                            (int) Math.round(size.zCoord * 16)));
                        }

                        GL11.glPopMatrix();
                    }

                    if (markedHit == null && mc.thePlayer.isSneaking()) {
                        ArrayList<ShiftHandler> shifthandlers = new ArrayList<>();

                        for (PreviewTile preview : previews)
                            if (preview.preview != null) shifthandlers.addAll(preview.preview.shifthandlers);

                        for (ShiftHandler shifthandler : shifthandlers) {
                            shifthandler.handleRendering(mc, x, y, z);
                        }
                    }

                    GL11.glDepthMask(true);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_BLEND);
                }
            }
        }
    }
}
