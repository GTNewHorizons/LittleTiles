package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.material.LittleMaterial;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
import com.creativemd.littletiles.utils.InsideShiftHandler;
import com.creativemd.littletiles.utils.PreviewTile;
import com.creativemd.littletiles.utils.ShiftHandler;

/** This class does all caculate on where to place a block. Used for rendering preview and placing **/
public final class PlacementHelper {

    private PlacementHelper() {}

    public static ILittleTile getLittleInterface(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() instanceof ILittleTile) return (ILittleTile) stack.getItem();
        if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile)
            return (ILittleTile) Block.getBlockFromItem(stack.getItem());
        return null;
    }

    public static boolean isLittleBlock(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() instanceof ILittleTile) return ((ILittleTile) stack.getItem()).hasLittlePreview(stack);
        if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile)
            return ((ILittleTile) Block.getBlockFromItem(stack.getItem())).hasLittlePreview(stack);
        return false;
    }

    /**
     * Whether the stack places at least one tile and all its materials pass the
     * {@link com.creativemd.littletiles.common.BlockValidator}.
     */
    public static boolean hasOnlyValidMaterials(ItemStack stack) {
        ILittleTile littleTile = getLittleInterface(stack);
        if (littleTile == null) return false;
        ArrayList<LittleTilePreview> previews = littleTile.getLittlePreview(stack);
        if (previews == null || previews.isEmpty()) return false;
        for (LittleTilePreview preview : previews) {
            if (preview == null || preview.nbt == null || !LittleMaterial.readFromNBT(preview.nbt).isValid())
                return false;
        }
        return true;
    }

    /** The min corner of the boxes of the tiles, wherever they start. */
    public static LittleTileVec getInternalOffset(ArrayList<LittleTilePreview> tiles) {
        LittleTileBox around = null;
        for (LittleTilePreview tile : tiles) {
            if (tile == null) return new LittleTileVec(0, 0, 0);
            if (tile.box != null) around = grown(around, tile.box);
        }
        return around == null ? new LittleTileVec(0, 0, 0) : around.getMinVec();
    }

    /** The size of the box around the boxes of the tiles, at least that of each tile without one. */
    public static LittleTileSize getSize(ArrayList<LittleTilePreview> tiles) {
        LittleTileBox around = null;
        LittleTileSize size = new LittleTileSize(0, 0, 0);
        for (LittleTilePreview tile : tiles) {
            if (tile == null) return new LittleTileSize(0, 0, 0);
            if (tile.box != null) around = grown(around, tile.box);
            else size.max(tile.size);
        }
        return around == null ? size : around.getSize().max(size);
    }

    /** The box around both, a copy of {@code box} if there is nothing around yet. */
    private static LittleTileBox grown(LittleTileBox around, LittleTileBox box) {
        if (around == null) return box.copy();
        around.set(
                Math.min(around.minX, box.minX),
                Math.min(around.minY, box.minY),
                Math.min(around.minZ, box.minZ),
                Math.max(around.maxX, box.maxX),
                Math.max(around.maxY, box.maxY),
                Math.max(around.maxZ, box.maxZ));
        return around;
    }

    public static ArrayList<PreviewTile> getPreviewTiles(EntityPlayer player, ItemStack stack, LittleTileBlockPos pos,
            boolean customPlacement) {
        ArrayList<LittleTilePreview> tiles = null;

        ILittleTile iTile = PlacementHelper.getLittleInterface(stack);

        if (iTile != null) tiles = iTile.getLittlePreview(stack);

        if (tiles == null) return new ArrayList<>();
        return getPreviewTiles(player, stack, pos, customPlacement, tiles, new LittleTileBounds(tiles));
    }

    /** Like {@link #getPreviewTiles}, but placed by the given bounds, which can be kept between calls. */
    public static ArrayList<PreviewTile> getPreviewTiles(EntityPlayer player, ItemStack stack, LittleTileBlockPos pos,
            boolean customPlacement, ArrayList<LittleTilePreview> tiles, LittleTileBounds bounds) {
        final World world = player.worldObj;
        ArrayList<ShiftHandler> shifthandlers = new ArrayList<>();
        ArrayList<PreviewTile> preview = new ArrayList<>();

        ILittleTile iTile = PlacementHelper.getLittleInterface(stack);

        if (tiles != null) {
            LittleTileSize size = bounds.size;

            shifthandlers.addAll(bounds.shiftHandlers);

            shifthandlers.add(new InsideShiftHandler());

            boolean fromChisel = stack.stackTagCompound != null
                    && (stack.getItem() == LittleTiles.chisel || stack.stackTagCompound.hasKey("fromChiselPosX"));

            LittleTileBox box = getTilesBox(size, pos, !fromChisel);
            if (fromChisel) {
                boolean posX = stack.stackTagCompound.getBoolean("fromChiselPosX");
                boolean posY = stack.stackTagCompound.getBoolean("fromChiselPosY");
                boolean posZ = stack.stackTagCompound.getBoolean("fromChiselPosZ");
                int align = stack.stackTagCompound.getInteger("fromChiselAlign");
                if (posX) {
                    box.minX -= size.sizeX - align;
                    box.maxX -= size.sizeX - align;
                }

                if (posY) {
                    box.minY -= size.sizeY - align;
                    box.maxY -= size.sizeY - align;
                }

                if (posZ) {
                    box.minZ -= size.sizeZ - align;
                    box.maxZ -= size.sizeZ - align;
                }
            }
            LittleTileVec internalOffset = bounds.min.copy();
            internalOffset.invert();

            boolean canPlaceNormal = false;

            if (!customPlacement && player.isSneaking()) {
                int x = pos.getPosX();
                int y = pos.getPosY();
                int z = pos.getPosZ();

                if (!bounds.boxes.isEmpty() && bounds.boxes.get(0) != null) {
                    Block block = world.getBlock(x, y, z);
                    if (block.isReplaceable(world, x, y, z) || block instanceof BlockTile) {
                        TileEntity te = world.getTileEntity(x, y, z);
                        canPlaceNormal = true;
                        if (te instanceof TileEntityLittleTiles teTiles) {
                            for (LittleTileBox tileBox : bounds.boxes) {
                                if (!teTiles.isSpaceForLittleTile(tileBox)) {
                                    canPlaceNormal = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!canPlaceNormal) {

                    for (ShiftHandler shiftHandler : shifthandlers) {
                        shiftHandler.init(world, x, y, z);
                    }

                    LittleTileVec hit = pos.toHitVecRelative();
                    ShiftHandler handler = null;
                    double distance = 2;
                    for (ShiftHandler shifthandler : shifthandlers) {
                        double tempDistance = shifthandler.getDistance(hit);
                        if (tempDistance < distance) {
                            distance = tempDistance;
                            handler = shifthandler;
                        }
                    }

                    if (handler != null) {
                        box = handler.getNewPosition(world, x, y, z, box);
                    }
                }
            }

            LittleTileVec offset = box.getMinVec();

            offset.addVec(internalOffset);

            for (LittleTilePreview tile : tiles) {
                if (tile != null) {
                    tile.itemBox = tile.getRenderBox().copy();
                    if (tile.box == null) {
                        tile.box = box.copy();
                        preview.add(new PreviewTile(tile.box.copy(), tile));
                    } else {
                        if (!canPlaceNormal) tile.box.addOffset(offset);
                        preview.add(new PreviewTile(tile.box, tile));
                    }
                }
            }

            LittleStructure structure = iTile.getLittleStructure(stack);
            if (structure != null) {
                ArrayList<PreviewTile> newBoxes = structure.getSpecialTiles();

                for (PreviewTile newBox : newBoxes) {
                    if (!canPlaceNormal) newBox.box.addOffset(offset);
                }

                preview.addAll(newBoxes);
            }
        }

        return preview;
    }

    public static LittleTileBox getTilesBox(LittleTileSize size, LittleTileBlockPos pos, boolean doCenter) {
        LittleTileVec hit = pos.toHitVecRelative();
        if (doCenter) {
            LittleTileVec center = size.calculateCenter();
            LittleTileVec centerInv = size.calculateInvertedCenter();
            switch (pos.getSide()) {
                case EAST:
                    hit.x += center.x;
                    break;
                case WEST:
                    hit.x -= centerInv.x - 1;
                    break;
                case UP:
                    hit.y += center.y;
                    break;
                case DOWN:
                    hit.y -= centerInv.y - 1;
                    break;
                case SOUTH:
                    hit.z += center.z;
                    break;
                case NORTH:
                    hit.z -= centerInv.z - 1;
                    break;
                default:
                    break;
            }
        }
        return new LittleTileBox(hit, size, doCenter);
    }

    public static boolean canBePlacedInsideBlock(EntityPlayer player, int x, int y, int z) {
        TileEntity tileEntity = player.worldObj.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityLittleTiles;
    }
}
