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
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
import com.creativemd.littletiles.utils.InsideShiftHandler;
import com.creativemd.littletiles.utils.PreviewTile;
import com.creativemd.littletiles.utils.ShiftHandler;

/** This class does all caculate on where to place a block. Used for rendering preview and placing **/
public class PlacementHelper {

    private static PlacementHelper instance;

    public static PlacementHelper getInstance(EntityPlayer player) {
        if (instance == null) instance = new PlacementHelper(player);
        else {
            instance.player = player;
            instance.world = player.worldObj;
        }
        return instance;
    }

    public EntityPlayer player;
    public World world;

    public PlacementHelper(EntityPlayer player) {
        this.player = player;
        this.world = player.worldObj;
    }

    public static ILittleTile getLittleInterface(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItem() instanceof ILittleTile) return (ILittleTile) stack.getItem();
        if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile)
            return (ILittleTile) Block.getBlockFromItem(stack.getItem());
        return null;
    }

    public static boolean isLittleBlock(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() instanceof ILittleTile)
            return ((ILittleTile) stack.getItem()).getLittlePreview(stack) != null;
        if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile)
            return ((ILittleTile) Block.getBlockFromItem(stack.getItem())).getLittlePreview(stack) != null;
        return false;
    }

    public static LittleTileVec getInternalOffset(ArrayList<LittleTilePreview> tiles) {
        byte minX = LittleTile.maxPos;
        byte minY = LittleTile.maxPos;
        byte minZ = LittleTile.maxPos;
        for (LittleTilePreview tile : tiles) {
            if (tile == null) return new LittleTileVec(0, 0, 0);
            if (tile.box != null) {
                minX = (byte) Math.min(minX, tile.box.minX);
                minY = (byte) Math.min(minY, tile.box.minY);
                minZ = (byte) Math.min(minZ, tile.box.minZ);
            }
        }
        return new LittleTileVec(minX, minY, minZ);
    }

    public static LittleTileSize getSize(ArrayList<LittleTilePreview> tiles) {
        byte minX = LittleTile.maxPos;
        byte minY = LittleTile.maxPos;
        byte minZ = LittleTile.maxPos;
        byte maxX = LittleTile.minPos;
        byte maxY = LittleTile.minPos;
        byte maxZ = LittleTile.minPos;
        LittleTileSize size = new LittleTileSize(0, 0, 0);
        for (LittleTilePreview tile : tiles) {
            if (tile == null) return new LittleTileSize(0, 0, 0);
            if (tile.box != null) {
                minX = (byte) Math.min(minX, tile.box.minX);
                minY = (byte) Math.min(minY, tile.box.minY);
                minZ = (byte) Math.min(minZ, tile.box.minZ);
                maxX = (byte) Math.max(maxX, tile.box.maxX);
                maxY = (byte) Math.max(maxY, tile.box.maxY);
                maxZ = (byte) Math.max(maxZ, tile.box.maxZ);
            } else {
                size.max(tile.size);
            }
        }
        return new LittleTileSize(maxX - minX, maxY - minY, maxZ - minZ).max(size);
    }

    public ArrayList<PreviewTile> getPreviewTiles(ItemStack stack, LittleTileBlockPos pos, boolean customPlacement) {
        ArrayList<ShiftHandler> shifthandlers = new ArrayList<>();
        ArrayList<PreviewTile> preview = new ArrayList<>();
        ArrayList<LittleTilePreview> tiles = null;

        ILittleTile iTile = PlacementHelper.getLittleInterface(stack);

        if (iTile != null) tiles = iTile.getLittlePreview(stack);

        if (tiles != null) {
            LittleTileSize size = getSize(tiles);

            if (tiles.size() == 1) shifthandlers.addAll(tiles.get(0).shifthandlers);

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
            LittleTileVec internalOffset = getInternalOffset(tiles);
            internalOffset.invert();

            boolean canPlaceNormal = false;

            if (!customPlacement && player.isSneaking()) {
                int x = pos.getPosX();
                int y = pos.getPosY();
                int z = pos.getPosZ();

                if (tiles.size() > 0 && tiles.get(0).box != null) {
                    Block block = world.getBlock(x, y, z);
                    if (block.isReplaceable(world, x, y, z) || block instanceof BlockTile) {
                        TileEntity te = world.getTileEntity(x, y, z);
                        canPlaceNormal = true;
                        if (te instanceof TileEntityLittleTiles) {
                            TileEntityLittleTiles teTiles = (TileEntityLittleTiles) te;
                            for (LittleTilePreview tile : tiles) {
                                if (!teTiles.isSpaceForLittleTile(tile.box)) {
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
                    if (tile.box == null) {
                        preview.add(new PreviewTile(box.copy(), tile));
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

    public LittleTileBox getTilesBox(LittleTileSize size, LittleTileBlockPos pos, boolean doCenter) {
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

    public boolean canBePlacedInsideBlock(int x, int y, int z) {
        TileEntity tileEntity = player.worldObj.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityLittleTiles;
    }
}
