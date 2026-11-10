package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
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

import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.common.gui.GuiToolConfig;
import com.creativemd.littletiles.common.packet.LittleFlipPacket;
import com.creativemd.littletiles.common.packet.LittleRotatePacket;
import com.creativemd.littletiles.common.packet.LittleUndoRedoPacket;
import com.creativemd.littletiles.common.items.ItemRecipe;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileBounds;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
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

    /** Up to how many blocks the chisel preview may reach into and still be textured. */
    private static final int CHISEL_PREVIEW_LIMIT = 27;

    public static LittleTileBlockPos markedHit = null;
    public static LittleTileBlockPos firstHit = null;
    private static Item lastItem = null;
    private static NBTTagCompound lastNbt = null;
    /** Bounds of the held multi tile item, so its tiles are not read every frame. Reset when the item changes. */
    private static LittleTileBounds heldBounds = null;

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

    private static double previewAlpha() {
        return Math.sin(System.nanoTime() / 200000000D) * 0.2 + 0.5;
    }

    /** Center of the preview tile, relative to the camera. Spans from the first hit while one is set. */
    private static Vec3 getPreviewCenter(PreviewTile previewTile, LittleTileBlockPos pos, double x, double y, double z,
            int align) {
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
                cubeX = -TileEntityRendererDispatcher.staticPlayerX + hitVec.xCoord - size.xCoord / 2D + align / 16f;
            }
            if (comparison.biggerOrEqualY) {
                cubeY = -TileEntityRendererDispatcher.staticPlayerY + hitVec.yCoord + size.yCoord / 2D;
            } else {
                cubeY = -TileEntityRendererDispatcher.staticPlayerY + hitVec.yCoord - size.yCoord / 2D + align / 16f;
            }

            if (comparison.biggerOrEqualZ) {
                cubeZ = -TileEntityRendererDispatcher.staticPlayerZ + hitVec.zCoord + size.zCoord / 2D;
            } else {
                cubeZ = -TileEntityRendererDispatcher.staticPlayerZ + hitVec.zCoord - size.zCoord / 2D + align / 16f;
            }
        }
        return Vec3.createVectorHelper(cubeX, cubeY, cubeZ);
    }

    private static boolean holdsChisel() {
        ItemStack held = Minecraft.getMinecraft().thePlayer.getHeldItem();
        return held != null && held.getItem() == LittleTiles.chisel;
    }

    private static Vector3i sizeInPixels(Vec3 size) {
        return new Vector3i(
                (int) Math.round(size.xCoord * 16),
                (int) Math.round(size.yCoord * 16),
                (int) Math.round(size.zCoord * 16));
    }

    /** The min corner of the preview in the world, in pixels. */
    private static Vector3i worldMinPixels(Vec3 center, Vec3 size) {
        return new Vector3i(
                (int) Math.round((center.xCoord - size.xCoord / 2D + TileEntityRendererDispatcher.staticPlayerX) * 16),
                (int) Math.round((center.yCoord - size.yCoord / 2D + TileEntityRendererDispatcher.staticPlayerY) * 16),
                (int) Math.round((center.zCoord - size.zCoord / 2D + TileEntityRendererDispatcher.staticPlayerZ) * 16));
    }

    /** How many blocks along one axis {@code size} pixels from {@code min} reach into. */
    private static int blocksSpanned(int min, int size) {
        return Math.floorDiv(min + size - 1, 16) - Math.floorDiv(min, 16) + 1;
    }

    /** The chisel is drawn block by block, so it is only textured while it reaches into a few. */
    private static boolean isTexturedPreview(PreviewTile previewTile, Vec3 center, Vec3 size) {
        if (!previewTile.isTextured()) return false;
        if (!holdsChisel()) return true;

        Vector3i min = worldMinPixels(center, size);
        Vector3i pixels = sizeInPixels(size);
        return (long) blocksSpanned(min.x, pixels.x) * blocksSpanned(min.y, pixels.y) * blocksSpanned(min.z, pixels.z)
                <= CHISEL_PREVIEW_LIMIT;
    }

    /**
     * The chisel previews what it is about to place, so its texture lines up with the world. A placed tile is textured
     * by each block it reaches into in that block's own coordinates, so the preview is drawn one block at a time.
     */
    private static void renderChiselTextured(PreviewTile previewTile, LittleToolHandler toolHandler,
            LittleTileCutoutInfo cutoutInfo, Vec3 center, Vec3 size, double alpha) {
        CubeObject cube = previewTile.preview.getCubeBlock();
        int meta = cube.meta == -1 ? 0 : cube.meta;
        int color = LittleTilesBlockRenderHelper.resolveRenderColor(cube.color, cube.block, meta);

        Vector3i min = worldMinPixels(center, size);
        Vector3i pixels = sizeInPixels(size);
        Vector3i max = new Vector3i(min).add(pixels);
        // Every piece is cut from the whole cutout
        Vector3i cutoutSize = toolHandler.getTileSize() != null ? toolHandler.getTileSize() : pixels;
        Vector3i posCutout = toolHandler.getTileOriginal();
        double minX = center.xCoord - size.xCoord / 2D;
        double minY = center.yCoord - size.yCoord / 2D;
        double minZ = center.zCoord - size.zCoord / 2D;

        for (int blockX = Math.floorDiv(min.x, 16); blockX * 16 < max.x; blockX++) {
            for (int blockY = Math.floorDiv(min.y, 16); blockY * 16 < max.y; blockY++) {
                for (int blockZ = Math.floorDiv(min.z, 16); blockZ * 16 < max.z; blockZ++) {
                    // The piece of the preview inside this block, in pixels from the preview's corner
                    Vector3i blockMin = new Vector3i(blockX * 16, blockY * 16, blockZ * 16).sub(min);
                    Vector3i subMin = new Vector3i(blockMin).max(new Vector3i());
                    Vector3i subMax = new Vector3i(blockMin).add(16, 16, 16).min(pixels);
                    Vec3 pieceSize = Vec3.createVectorHelper(
                            (subMax.x - subMin.x) / 16D,
                            (subMax.y - subMin.y) / 16D,
                            (subMax.z - subMin.z) / 16D);
                    LittleTilesBlockRenderHelper.renderTexturedShape(
                            toolHandler.getShape(),
                            minX + subMin.x / 16D + pieceSize.xCoord / 2D,
                            minY + subMin.y / 16D + pieceSize.yCoord / 2D,
                            minZ + subMin.z / 16D + pieceSize.zCoord / 2D,
                            pieceSize,
                            cutoutSize,
                            toolHandler.getOrientation(),
                            new Vector3i(posCutout).sub(subMin),
                            cutoutInfo,
                            cube.block,
                            meta,
                            color,
                            new Vector3d(
                                    (subMin.x - blockMin.x) / 16D,
                                    (subMin.y - blockMin.y) / 16D,
                                    (subMin.z - blockMin.z) / 16D),
                            alpha);
                }
            }
        }
    }

    /**
     * The cached geometry of the held item, see {@link TileGeometryBuffer}. It is the geometry of the item's tiles as
     * long as they keep their arrangement. Null for partial tiles, which show a fill level, and for the chisel, whose
     * tile changes with the hits.
     */
    private static TileGeometryBuffer itemBuffer(ItemStack held) {
        if (held == null || !(held.getItem() instanceof ITilesRenderer) || held.getItem() == LittleTiles.partialTiles) {
            return null;
        }
        return TileGeometryBuffer.get(held);
    }

    /**
     * Draws the item's geometry in place of its textured preview tiles. They are all moved by the same offset, so the
     * lowest corners of the item and of the preview match.
     */
    private static void renderItemBuffer(TileGeometryBuffer buffer, ArrayList<PreviewTile> previews,
            LittleTileBlockPos pos, double x, double y, double z, int align, double alpha) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        for (PreviewTile previewTile : previews) {
            if (!previewTile.isTextured()) continue;
            Vec3 center = getPreviewCenter(previewTile, pos, x, y, z, align);
            Vec3 size = previewTile.getPreviewBox().getSizeD();
            minX = Math.min(minX, center.xCoord - size.xCoord / 2D);
            minY = Math.min(minY, center.yCoord - size.yCoord / 2D);
            minZ = Math.min(minZ, center.zCoord - size.zCoord / 2D);
        }
        if (minX == Double.MAX_VALUE) return;

        boolean lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GL11.glDisable(GL11.GL_LIGHTING);
        // The vertices carry their own colors, so the fading comes from the blending instead
        GL14.glBlendColor(0, 0, 0, (float) alpha);
        GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
        // The buffer cannot be pulled towards the camera like the immediate preview, so it is offset in depth instead
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1, -1);

        Vec3 bufferMin = buffer.getMin();
        GL11.glPushMatrix();
        GL11.glTranslated(minX - bufferMin.xCoord, minY - bufferMin.yCoord, minZ - bufferMin.zCoord);
        buffer.render();
        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        if (lightingWasEnabled) GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /** The shape of the preview tile: textured like its item, or untextured in its preview color. */
    private static void renderPreviewShape(PreviewTile previewTile, LittleTileBlockPos pos, double x, double y,
            double z, int align, double alpha) {
        GL11.glPushMatrix();
        Vec3 center = getPreviewCenter(previewTile, pos, x, y, z, align);
        Vec3 size = previewTile.getPreviewBox().getSizeD();
        Vec3 color = previewTile.getPreviewColor();

        LittleToolHandler toolHandler;

        LittleTileCutoutInfo cutoutInfo = null;
        if (previewTile.preview != null) {
            toolHandler = new LittleToolHandler(previewTile.preview.nbt);
            cutoutInfo = LittleTileCutoutInfo.loadFromNBT(previewTile.preview.nbt);
        } else {
            toolHandler = new LittleToolHandler(Minecraft.getMinecraft().thePlayer.getHeldItem());
        }

        if (isTexturedPreview(previewTile, center, size) && holdsChisel()) {
            renderChiselTextured(previewTile, toolHandler, cutoutInfo, center, size, alpha);
            GL11.glPopMatrix();
            return;
        }

        if (isTexturedPreview(previewTile, center, size)) {
            // Textured as the item shows the tile, from where it sits in its own block - not where it is placed.
            LittleTilePreview tile = previewTile.preview;
            CubeObject cube = tile.getCubeBlock();
            CubeObject itemCube = (tile.itemBox != null ? tile.itemBox : tile.getRenderBox()).getCube();
            int meta = cube.meta == -1 ? 0 : cube.meta;
            LittleTilesBlockRenderHelper.renderTexturedShape(
                    toolHandler.getShape(),
                    center.xCoord,
                    center.yCoord,
                    center.zCoord,
                    size,
                    toolHandler.getTileSize(),
                    toolHandler.getOrientation(),
                    toolHandler.getTileOriginal(),
                    cutoutInfo,
                    cube.block,
                    meta,
                    LittleTilesBlockRenderHelper.resolveRenderColor(cube.color, cube.block, meta),
                    new Vector3d(
                            itemCube.minX - Math.floor(itemCube.minX),
                            itemCube.minY - Math.floor(itemCube.minY),
                            itemCube.minZ - Math.floor(itemCube.minZ)),
                    alpha);
            GL11.glPopMatrix();
            return;
        }

        LittleTilesBlockRenderHelper.renderShape(
                toolHandler.getShape(),
                center.xCoord,
                center.yCoord,
                center.zCoord,
                size,
                toolHandler.getTileSize(), // Needed for block picked cutouts
                toolHandler.getOrientation(),
                toolHandler.getTileOriginal(), // Needed for block picked cutouts
                color,
                alpha,
                cutoutInfo);

        GL11.glPopMatrix();
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
                heldBounds = null;
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

                    // A multi tile item is drawn from its buffer, so one tile spanning its bounds is enough to place
                    // it. The bounds are read once instead of every frame.
                    TileGeometryBuffer itemBuffer = itemBuffer(held);
                    if (itemBuffer != null && held.getItem() == LittleTiles.multiTiles) {
                        if (heldBounds == null) heldBounds = new LittleTileBounds(ItemRecipe.getPreview(held));
                        LittleTileVec min = heldBounds.min;
                        LittleTileSize size = heldBounds.size;
                        ArrayList<LittleTilePreview> boundsTile = new ArrayList<>();
                        boundsTile.add(
                                new LittleTilePreview(
                                        new LittleTileBox(
                                                min.x,
                                                min.y,
                                                min.z,
                                                min.x + size.sizeX,
                                                min.y + size.sizeY,
                                                min.z + size.sizeZ),
                                        new NBTTagCompound()));
                        previews = PlacementHelper
                                .getPreviewTiles(mc.thePlayer, held, pos, markedHit != null, boundsTile, heldBounds);
                    } else {
                        previews = PlacementHelper
                                .getPreviewTiles(mc.thePlayer, mc.thePlayer.getHeldItem(), pos, markedHit != null);
                    }

                    double x = (double) pos.getPosX() - TileEntityRendererDispatcher.staticPlayerX;
                    double y = (double) pos.getPosY() - TileEntityRendererDispatcher.staticPlayerY;
                    double z = (double) pos.getPosZ() - TileEntityRendererDispatcher.staticPlayerZ;
                    // A deformed box being edited was already drawn above, as a wireframe of its own corners -
                    // filling its faces here would hide whatever the player is trying to line the box up against.
                    if (!editingDeformedBox) {
                        double alpha = previewAlpha();
                        // Textured previews only show their front-most faces. Faces inside them, like those of two
                        // touching tiles, would otherwise show through the alpha and add up with the ones in front.
                        // So they go into depth first, then in color only where they are in front. The depth buffer
                        // is cleared for the hand right after this event, so writing to it leaves nothing behind.
                        // The color mask is restored, not reset, as anaglyph 3D only lets each eye write its channels.
                        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
                        GL11.glColorMask(false, false, false, false);
                        GL11.glDepthMask(true);
                        if (itemBuffer != null) renderItemBuffer(itemBuffer, previews, pos, x, y, z, align, alpha);
                        else for (PreviewTile previewTile : previews) {
                            Vec3 center = getPreviewCenter(previewTile, pos, x, y, z, align);
                            if (isTexturedPreview(previewTile, center, previewTile.getPreviewBox().getSizeD())) {
                                renderPreviewShape(previewTile, pos, x, y, z, align, alpha);
                            }
                        }
                        GL11.glPopAttrib();
                        GL11.glDepthMask(false);
                        GL11.glDepthFunc(GL11.GL_LEQUAL);

                        if (itemBuffer != null) renderItemBuffer(itemBuffer, previews, pos, x, y, z, align, alpha);
                        for (PreviewTile previewTile : previews) {
                            if (itemBuffer != null && previewTile.isTextured()) continue;
                            renderPreviewShape(previewTile, pos, x, y, z, align, alpha);
                        }
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
