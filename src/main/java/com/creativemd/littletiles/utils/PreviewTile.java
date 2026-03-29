package com.creativemd.littletiles.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;

import com.creativemd.creativecore.common.utils.HashMapList;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

public class PreviewTile {

    public static final Vec3 white = Vec3.createVectorHelper(1, 1, 1);

    public LittleTileBox box;
    public LittleTilePreview preview;

    public PreviewTile(LittleTileBox box, LittleTilePreview preview) {
        this.box = box;
        this.preview = preview;
    }

    public PreviewTile copy() {
        return new PreviewTile(box.copy(), preview.copy());
    }

    public Vec3 getPreviewColor() {
        return white;
    }

    public boolean needsCollisionTest() {
        return true;
    }

    public LittleTileBox getPreviewBox() {
        return box;
    }

    private List<LittleTile> placeTileModeFill(LittleTile tileNew,
            EntityPlayer player, ItemStack stack) {
        List<LittleTile> tiles = new ArrayList<>();
        tiles.add(tileNew);

        for (LittleTile existing : tileNew.te.getTiles()) {
            List<LittleTile> newTiles = new ArrayList<>();
            for (LittleTile t : tiles) {
                if (t.overlapsTile(existing)) {
                    newTiles.addAll(t.splitByTile(existing));
                } else {
                    newTiles.add(t);
                }
            }
            tiles = newTiles;
        }

        for (LittleTile tile : tiles) {
            tile.place();
            tile.onPlaced(player, stack);
        }
        return tiles;
    }

    private List<LittleTile> placeTileModeOverwrite(LittleTile tileNew,
            EntityPlayer player, ItemStack stack, boolean doAdd) {
        List<LittleTile> tiles = new ArrayList<>(tileNew.te.getTiles());
        List<LittleTile> newTiles = new ArrayList<>();

        for (LittleTile t : tiles) {
            if (t.overlapsTile(tileNew)) {
                newTiles.addAll(t.splitByTile(tileNew));
                boolean cleanUpTileEntity = newTiles.isEmpty();
                t.destroy(cleanUpTileEntity);
            }
        }
        if (doAdd) {
            newTiles.add(tileNew);
        }
        for (LittleTile tile : newTiles) {
            tile.place();
            tile.onPlaced(player, stack);
        }
        return newTiles;
    }

    public List<LittleTile> placeTile(EntityPlayer player, ItemStack stack, TileEntityLittleTiles teLT,
            LittleStructure structure, ArrayList<LittleTile> unplaceableTiles, LittleTilePlaceMode placeMode) {
        LittleTile LT = preview.getLittleTile(teLT);
        if (LT == null) return null;

        LT.boundingBox = box.copy();
        LT.updateCorner();

        if (structure != null) {
            LT.isStructureBlock = true;
            LT.structure = structure;
            structure.getTiles().add(LT);
        }

        if (teLT.isSpaceForLittleTile(box.copy())) {
            if (placeMode == LittleTilePlaceMode.STENCIL) {
                return null;
            }
            LT.place();
            LT.onPlaced(player, stack);
            List<LittleTile> ret = new ArrayList<>();
            ret.add(LT);
            return ret;
        } else if (placeMode == LittleTilePlaceMode.FILL) {
            return placeTileModeFill(LT, player, stack);
        } else if (placeMode == LittleTilePlaceMode.OVERWRITE) {
            return placeTileModeOverwrite(LT, player, stack, true);
        } else if (placeMode == LittleTilePlaceMode.STENCIL) {
            return placeTileModeOverwrite(LT, player, stack, false);
        } else if (unplaceableTiles != null) {
            unplaceableTiles.add(LT);
        }
        return null;
    }

    public boolean split(HashMapList<ChunkCoordinates, PreviewTile> tiles, int x, int y, int z) {
        if (preview != null && !preview.canSplit && box.needsMultipleBlocks()) return false;
        LittleTileSize size = box.getSize();

        int offX = box.minX / 16;
        if (box.minX < 0) offX = (int) Math.floor(box.minX / 16D);
        int offY = box.minY / 16;
        if (box.minY < 0) offY = (int) Math.floor(box.minY / 16D);
        int offZ = box.minZ / 16;
        if (box.minZ < 0) offZ = (int) Math.floor(box.minZ / 16D);

        int posX = x + offX;
        int posY;
        int posZ;

        int spaceX = box.minX - offX * 16;
        int spaceY = box.minY - offY * 16;
        int spaceZ = box.minZ - offZ * 16;

        for (int i = 0; spaceX + size.sizeX > i * 16; i++) {
            posY = y + offY;
            for (int j = 0; spaceY + size.sizeY > j * 16; j++) {
                posZ = z + offZ;
                for (int h = 0; spaceZ + size.sizeZ > h * 16; h++) {

                    PreviewTile tile = this.copy();
                    if (i > 0) tile.box.minX = 0;
                    else tile.box.minX = spaceX;
                    if (i * 16 + 16 > spaceX + size.sizeX) {
                        tile.box.maxX = (box.maxX - box.maxX / 16 * 16);
                        if (box.maxX < 0) tile.box.maxX = 16 + tile.box.maxX;
                    } else tile.box.maxX = 16;

                    if (j > 0) tile.box.minY = 0;
                    else tile.box.minY = spaceY;
                    if (j * 16 + 16 > spaceY + size.sizeY) {
                        tile.box.maxY = (box.maxY - box.maxY / 16 * 16);
                        if (box.maxY < 0) tile.box.maxY = 16 + tile.box.maxY;
                    } else tile.box.maxY = 16;

                    if (h > 0) tile.box.minZ = 0;
                    else tile.box.minZ = spaceZ;
                    if (h * 16 + 16 > spaceZ + size.sizeZ) {
                        tile.box.maxZ = (box.maxZ - box.maxZ / 16 * 16);
                        if (box.maxZ < 0) tile.box.maxZ = 16 + tile.box.maxZ;
                    } else tile.box.maxZ = 16;

                    if (tile.box.isValidBox()) {
                        tiles.add(new ChunkCoordinates(posX, posY, posZ), tile);
                    }
                    posZ++;
                }
                posY++;
            }
            posX++;
        }
        return true;
    }

}
