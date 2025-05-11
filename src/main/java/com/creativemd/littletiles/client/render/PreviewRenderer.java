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

import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.rendering.RenderHelper3D;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.common.packet.LittleFlipPacket;
import com.creativemd.littletiles.common.packet.LittleRotatePacket;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
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

    public static void moveMarkedHit(ForgeDirection direction, ForgeDirection direction_look) {
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

        int move = 1;
        if (GuiScreen.isCtrlKeyDown()) move = 16;
        markedHit.moveInDirection(direction, move);
    }

    @SubscribeEvent
    public void tick(RenderHandEvent event) {
        if (mc.thePlayer != null && mc.inGameHasFocus) {

            if (!ItemStack.areItemStackTagsEqual(lastItem, mc.thePlayer.getHeldItem())) {
                markedHit = null;
            }
            lastItem = mc.thePlayer.getHeldItem();
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
                if (look != null && look.typeOfHit == MovingObjectType.BLOCK) {
                    pos = LittleTileBlockPos.fromMovingObjectPosition(look);
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
                                direction_look);
                        else processKey(ForgeDirection.UP);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.up)) LittleTilesClient.pressedUp = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.down) && !LittleTilesClient.pressedDown) {
                        LittleTilesClient.pressedDown = true;
                        if (markedHit != null) moveMarkedHit(
                                mc.thePlayer.isSneaking() ? ForgeDirection.DOWN : ForgeDirection.SOUTH,
                                direction_look);
                        else processKey(ForgeDirection.DOWN);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.down)) LittleTilesClient.pressedDown = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.right) && !LittleTilesClient.pressedRight) {
                        LittleTilesClient.pressedRight = true;
                        if (markedHit != null) moveMarkedHit(ForgeDirection.EAST, direction_look);
                        else processKey(ForgeDirection.SOUTH);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.right)) LittleTilesClient.pressedRight = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.left) && !LittleTilesClient.pressedLeft) {
                        LittleTilesClient.pressedLeft = true;
                        if (markedHit != null) moveMarkedHit(ForgeDirection.WEST, direction_look);
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
                        Vec3 color = previewTile.getPreviewColor();
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
