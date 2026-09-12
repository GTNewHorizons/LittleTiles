package com.creativemd.littletiles.client.render;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
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
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.gui.GuiToolConfig;
import com.creativemd.littletiles.common.packet.LittleFlipPacket;
import com.creativemd.littletiles.common.packet.LittleRotatePacket;
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
    private static ItemStack lastItem = null;

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

    /** The 12 edges of the box, as pairs of corner indices - the two corners of an edge differ in exactly one axis. */
    private static final int[][] BOX_EDGES = { { 0, 1 }, { 2, 3 }, { 4, 5 }, { 6, 7 }, // along x
            { 0, 2 }, { 1, 3 }, { 4, 6 }, { 5, 7 }, // along y
            { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 }, // along z
    };

    /**
     * Draws the deformed box being edited as a wireframe of its 12 edges. Faces are never filled, so the player can see
     * the tiles behind the box while shaping it.
     */
    private static void renderBoxEdges() {
        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        GL11.glColor4d(valid ? 0.2 : 1, valid ? 0.8 : 0.1, valid ? 1 : 0.1, 0.9);
        GL11.glBegin(GL11.GL_LINES);
        for (int[] edge : BOX_EDGES) {
            for (int index : edge) {
                vertexAtCorner(index);
            }
        }
        GL11.glEnd();

        renderFaceDiagonals();
    }

    private static void vertexAtCorner(int index) {
        Vec3 vec = LittleDeformedBoxHelper.cornerHitVec(index);
        GL11.glVertex3d(
                vec.xCoord - TileEntityRendererDispatcher.staticPlayerX,
                vec.yCoord - TileEntityRendererDispatcher.staticPlayerY,
                vec.zCoord - TileEntityRendererDispatcher.staticPlayerZ);
    }

    /**
     * Draws the split diagonal of every face whose 4 corners are no longer coplanar, showing where the surface actually
     * bends. The diagonal comes from {@link Mesh3dUtil#splitsAlongFirstDiagonal}, the same call the mesh itself is
     * built from, so the line can never disagree with the geometry it is describing.
     */
    private static void renderFaceDiagonals() {
        LittleTileCutoutInfo cutout = LittleDeformedBoxHelper.currentCutout();
        Vector3d[] local = new Vector3d[cutout.corners.length];
        for (int i = 0; i < local.length; i++) {
            local[i] = Mesh3dUtil.toLocal(cutout.corners[i], cutout.size);
        }

        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        GL11.glColor4d(valid ? 0.2 : 1, valid ? 0.8 : 0.1, valid ? 1 : 0.1, 0.45);
        GL11.glBegin(GL11.GL_LINES);
        for (int[] face : Mesh3dUtil.DEFORMED_BOX_FACES) {
            if (isFacePlanar(face, cutout.corners)) {
                continue;
            }
            boolean first = Mesh3dUtil
                    .splitsAlongFirstDiagonal(local[face[0]], local[face[1]], local[face[2]], local[face[3]]);
            vertexAtCorner(first ? face[0] : face[1]);
            vertexAtCorner(first ? face[2] : face[3]);
        }
        GL11.glEnd();
    }

    /**
     * Whether a face's 4 corners still lie in one plane - that is, whether the face is merely tilted or has actually
     * been folded. A flat face is drawn by two coplanar triangles, so its diagonal is invisible on the surface and
     * drawing it would suggest a bend that is not there; only a folded face has a fold worth showing.
     * <p>
     * Worked out as a scalar triple product in whole grid units, which makes the test exact: corner offsets are
     * integers, so the product either is zero or it is not, and there is no epsilon to tune. Longs because a corner
     * dragged a long way makes the intermediate cross product outgrow an int.
     * <p>
     * A face with three collinear corners counts as planar, correctly: some plane always contains that line and the
     * fourth corner, and the mesh gets nothing but a degenerate triangle out of it.
     */
    private static boolean isFacePlanar(int[] face, Vector3i[] corners) {
        Vector3i a = corners[face[0]];
        long abx = corners[face[1]].x - a.x, aby = corners[face[1]].y - a.y, abz = corners[face[1]].z - a.z;
        long acx = corners[face[2]].x - a.x, acy = corners[face[2]].y - a.y, acz = corners[face[2]].z - a.z;
        long adx = corners[face[3]].x - a.x, ady = corners[face[3]].y - a.y, adz = corners[face[3]].z - a.z;

        long nx = aby * acz - abz * acy;
        long ny = abz * acx - abx * acz;
        long nz = abx * acy - aby * acx;
        return nx * adx + ny * ady + nz * adz == 0;
    }

    /**
     * Draws a small cube on each of the 8 corners of the deformed box being edited, so the player can see what there is
     * to grab. The selected corner is drawn in a different colour. These are the very same cubes
     * {@link LittleDeformedBoxHelper#pickCorner} raytraces against, so what is clicked is what is shown.
     */
    private static void renderCornerMarkers(int grid) {
        boolean valid = LittleDeformedBoxHelper.hasValidGeometry();
        for (int i = 0; i < Mesh3dUtil.DEFORMED_BOX_CORNER_COUNT; i++) {
            AxisAlignedBB box = LittleDeformedBoxHelper.getCornerBoxAABB(i, grid);
            boolean selected = LittleDeformedBoxHelper.isMarkedCorner(i);
            RenderHelper3D.renderBlock(
                    (box.minX + box.maxX) / 2 - TileEntityRendererDispatcher.staticPlayerX,
                    (box.minY + box.maxY) / 2 - TileEntityRendererDispatcher.staticPlayerY,
                    (box.minZ + box.maxZ) / 2 - TileEntityRendererDispatcher.staticPlayerZ,
                    box.maxX - box.minX,
                    box.maxY - box.minY,
                    box.maxZ - box.minZ,
                    0,
                    0,
                    0,
                    valid ? (selected ? 1 : 0.2) : 1,
                    valid ? 0.6 : 0.1,
                    valid ? (selected ? 0 : 1) : 0.1,
                    selected ? 0.9 : 0.5);
        }
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
        else processKey(rotate);
    }

    @SubscribeEvent
    public void tick(RenderHandEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.inGameHasFocus) {

            if (!ItemStack.areItemStackTagsEqual(lastItem, mc.thePlayer.getHeldItem())) {
                markedHit = null;
                firstHit = null;
                LittleDeformedBoxHelper.reset();
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
                boolean editingDeformedBox = mc.thePlayer.getHeldItem().getItem() == LittleTiles.chisel
                        && LittleDeformedBoxHelper.isEditing()
                        && new LittleToolHandler(mc.thePlayer.getHeldItem()).isDeformedBoxShape();
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
                        renderBoxEdges();
                        renderCornerMarkers(align);
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
