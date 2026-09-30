package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.common.gui.GuiToolConfig;
import com.creativemd.littletiles.common.packet.LittleFlipPacket;
import com.creativemd.littletiles.common.packet.LittleRotatePacket;
import com.creativemd.littletiles.common.packet.LittleUndoRedoPacket;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
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

    public void processKey(ForgeDirection direction) {
        LittleRotatePacket packet = new LittleRotatePacket(direction);
        packet.executeClient(Minecraft.getMinecraft().thePlayer);
        PacketHandler.sendPacketToServer(packet);
    }

    public static LittleTileBlockPos markedHit = null;
    public static LittleTileBlockPos firstHit = null;
    private static Item lastItem = null;
    private static NBTTagCompound lastNbt = null;

    private static ForgeDirection rotateDirection(ForgeDirection direction) {
        return switch (direction) {
            case NORTH -> ForgeDirection.EAST;
            case EAST -> ForgeDirection.SOUTH;
            case SOUTH -> ForgeDirection.WEST;
            case WEST -> ForgeDirection.NORTH;
            default -> ForgeDirection.UNKNOWN;
        };
    }

    /** Turns a screen-relative arrow direction into a world direction, based on which way the player is facing. */
    private static ForgeDirection relativeToLook(ForgeDirection direction, ForgeDirection direction_look) {
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
        return direction;
    }

    /** How far one arrow key press moves something: a single grid step, or a whole block while ctrl is held. */
    private static int stepAmount(int amount) {
        return GuiScreen.isCtrlKeyDown() ? 16 : amount;
    }

    private static void moveMarkedHit(ForgeDirection direction, ForgeDirection direction_look, int amount) {
        markedHit.moveInDirection(relativeToLook(direction, direction_look), stepAmount(amount));
    }

    /** Whether both corners of an ordinary two-hit chisel preview are fixed and can be selected. */
    public static boolean hasTwoHitCorners() {
        return firstHit != null && markedHit != null;
    }

    /** The pickable cubes of the two fixed chisel corners, {@link #firstHit} first: the grid cell each one names. */
    static AxisAlignedBB[] twoHitCornerBoxes(int grid) {
        return new AxisAlignedBB[] { firstHit.getHitBox(grid), markedHit.getHitBox(grid) };
    }

    /** Index into {@link #twoHitCornerBoxes} of the corner the player looks at, or -1 if none. */
    static int pickTwoHitCorner(EntityPlayer player, int grid) {
        if (!hasTwoHitCorners()) return -1;
        return LittleDeformedBoxHelper.pickLookedAtBox(player, twoHitCornerBoxes(grid));
    }

    /**
     * Selects the looked at one of the two fixed chisel corners. {@link #markedHit} remains the selected and movable
     * corner, so selecting {@link #firstHit} only has to swap the two - the preview spans the same box either way.
     */
    public static void selectTwoHitCorner(EntityPlayer player, int grid) {
        if (pickTwoHitCorner(player, grid) != 0) return;

        LittleTileBlockPos previousFirst = firstHit;
        firstHit = markedHit;
        markedHit = previousFirst;
    }

    /** Whether the held chisel is currently shaping a deformed box, rather than showing a regular preview. */
    private static boolean isEditingDeformedBox() {
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        return held != null && held.getItem() == LittleTiles.chisel
                && LittleDeformedBoxHelper.isEditing()
                && new LittleToolHandler(held).isDeformedBoxShape();
    }

    private static void moveMarkedCorner(ForgeDirection direction, ForgeDirection direction_look, int amount) {
        LittleDeformedBoxHelper.nudgeMarked(relativeToLook(direction, direction_look), stepAmount(amount));
    }

    /**
     * The arrow keys mean one of three things depending on what is currently selected: nudge the selected corner of a
     * deformed box, move the marked preview, or - with nothing marked at all - rotate the preview.
     */
    private void handleArrow(ForgeDirection move, ForgeDirection rotate, ForgeDirection direction_look, int align) {
        if (LittleDeformedBoxHelper.hasMarkedCorner()) moveMarkedCorner(move, direction_look, align);
        else if (markedHit != null) moveMarkedHit(move, direction_look, align);
        else if (!isEditingDeformedBox()) processKey(rotate);
    }

    @SubscribeEvent
    public void tick(RenderHandEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.inGameHasFocus) {

            ItemStack held = mc.thePlayer.getHeldItem();
            Item item = held != null ? held.getItem() : null;
            NBTTagCompound nbt = held != null ? held.getTagCompound() : null;

            if (item != lastItem || !Objects.equals(lastNbt, nbt)) {
                markedHit = null;
                firstHit = null;
                LittleDeformedBoxHelper.reset();
                lastItem = item;
                // Copy the tags so in-place tool setting changes are detected on the next tick.
                lastNbt = nbt != null ? (NBTTagCompound) nbt.copy() : null;
            }

            if (mc.thePlayer.getHeldItem() != null) {
                if (GameSettings.isKeyDown(LittleTilesClient.toolConfig) && !LittleTilesClient.pressedToolConfig) {
                    LittleTilesClient.pressedToolConfig = true;
                    GuiToolConfig.show(mc.thePlayer.getHeldItem());
                } else if (!GameSettings.isKeyDown(LittleTilesClient.toolConfig)) {
                    LittleTilesClient.pressedToolConfig = false;
                }
            }

            if (GameSettings.isKeyDown(LittleTilesClient.undoPlacement) && !LittleTilesClient.pressedUndoPlacement) {
                LittleTilesClient.pressedUndoPlacement = true;
                PacketHandler.sendPacketToServer(new LittleUndoRedoPacket(true));
            } else if (!GameSettings.isKeyDown(LittleTilesClient.undoPlacement)) {
                LittleTilesClient.pressedUndoPlacement = false;
            }

            if (GameSettings.isKeyDown(LittleTilesClient.redoPlacement) && !LittleTilesClient.pressedRedoPlacement) {
                LittleTilesClient.pressedRedoPlacement = true;
                PacketHandler.sendPacketToServer(new LittleUndoRedoPacket(false));
            } else if (!GameSettings.isKeyDown(LittleTilesClient.redoPlacement)) {
                LittleTilesClient.pressedRedoPlacement = false;
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

                    // Like rotating, flipping would change the tool's nbt and drop the deformed box being edited.
                    if (!isEditingDeformedBox()) {
                        ForgeDirection direction = direction_look;
                        if (mc.thePlayer.rotationPitch > 45) direction = ForgeDirection.DOWN;
                        if (mc.thePlayer.rotationPitch < -45) direction = ForgeDirection.UP;
                        LittleFlipPacket packet = new LittleFlipPacket(direction);
                        packet.executeClient(mc.thePlayer);
                        PacketHandler.sendPacketToServer(packet);
                    }
                } else if (!GameSettings.isKeyDown(LittleTilesClient.flip)) {
                    LittleTilesClient.pressedFlip = false;
                }

                MovingObjectPosition look = mc.objectMouseOver;
                LittleTileBlockPos pos = null;
                int align = 1;
                if (mc.thePlayer.getHeldItem().getItem() == LittleTiles.chisel) {
                    align = new LittleToolHandler(mc.thePlayer.getHeldItem()).getGrid();
                }
                if (look != null && look.typeOfHit == MovingObjectType.BLOCK) {
                    pos = LittleTileBlockPos.fromMovingObjectPosition(look, align);
                }

                if (markedHit != null) pos = markedHit;

                // A box being deformed is anchored by its own corners, not by what the player is looking at - so it
                // stays on screen even while looking at nothing.
                boolean editingDeformedBox = isEditingDeformedBox();
                if (editingDeformedBox) pos = LittleDeformedBoxHelper.placementAnchor();

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
                        handleArrow(
                                mc.thePlayer.isSneaking() ? ForgeDirection.UP : ForgeDirection.NORTH,
                                ForgeDirection.UP,
                                direction_look,
                                align);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.up)) LittleTilesClient.pressedUp = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.down) && !LittleTilesClient.pressedDown) {
                        LittleTilesClient.pressedDown = true;
                        handleArrow(
                                mc.thePlayer.isSneaking() ? ForgeDirection.DOWN : ForgeDirection.SOUTH,
                                ForgeDirection.DOWN,
                                direction_look,
                                align);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.down)) LittleTilesClient.pressedDown = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.right) && !LittleTilesClient.pressedRight) {
                        LittleTilesClient.pressedRight = true;
                        handleArrow(ForgeDirection.EAST, ForgeDirection.SOUTH, direction_look, align);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.right)) LittleTilesClient.pressedRight = false;

                    if (GameSettings.isKeyDown(LittleTilesClient.left) && !LittleTilesClient.pressedLeft) {
                        LittleTilesClient.pressedLeft = true;
                        handleArrow(ForgeDirection.WEST, ForgeDirection.NORTH, direction_look, align);
                    } else if (!GameSettings.isKeyDown(LittleTilesClient.left)) LittleTilesClient.pressedLeft = false;

                    GL11.glEnable(GL11.GL_BLEND);
                    OpenGlHelper.glBlendFunc(770, 771, 1, 0);
                    GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
                    GL11.glLineWidth(2.0F);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDepthMask(false);

                    if (editingDeformedBox) {
                        LittleDeformedBoxPreviewRenderer.renderBoxEdges(align);
                        LittleDeformedBoxPreviewRenderer.renderCornerMarkers(align);
                    }

                    ArrayList<PreviewTile> previews;

                    previews = PlacementHelper
                            .getPreviewTiles(mc.thePlayer, mc.thePlayer.getHeldItem(), pos, markedHit != null);

                    double x = (double) pos.getPosX() - TileEntityRendererDispatcher.staticPlayerX;
                    double y = (double) pos.getPosY() - TileEntityRendererDispatcher.staticPlayerY;
                    double z = (double) pos.getPosZ() - TileEntityRendererDispatcher.staticPlayerZ;
                    for (PreviewTile previewTile : previews) {
                        // A deformed box being edited was already drawn above, as a wireframe of its own corners -
                        // filling its faces here would hide whatever the player is trying to line the box up against.
                        if (editingDeformedBox) break;

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

                        LittleToolHandler toolHandler;

                        LittleTileCutoutInfo cutoutInfo = null;
                        if (previewTile.preview != null) {
                            toolHandler = new LittleToolHandler(previewTile.preview.nbt);
                            cutoutInfo = LittleTileCutoutInfo.loadFromNBT(previewTile.preview.nbt);
                        } else {
                            toolHandler = new LittleToolHandler(mc.thePlayer.getHeldItem());
                        }
                        LittleTilesBlockRenderHelper.renderShape(
                                toolHandler.getShape(),
                                cubeX,
                                cubeY,
                                cubeZ,
                                size,
                                toolHandler.getTileSize(), // Needed for block picked cutouts
                                toolHandler.getOrientation(),
                                toolHandler.getTileOriginal(), // Needed for block picked cutouts
                                color,
                                Math.sin(System.nanoTime() / 200000000D) * 0.2 + 0.5,
                                cutoutInfo);

                        GL11.glPopMatrix();
                    }

                    // Drawn after the filled preview so the markers remain visible where their cubes overlap it.
                    if (!editingDeformedBox && hasTwoHitCorners()) {
                        LittleDeformedBoxPreviewRenderer.renderTwoHitCornerMarkers(align);
                    }

                    // Sneaking is how corners are nudged up/down, so the shift handler overlay would otherwise be on
                    // screen for most of the time a box is being shaped.
                    if (!editingDeformedBox && markedHit == null && mc.thePlayer.isSneaking()) {
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
