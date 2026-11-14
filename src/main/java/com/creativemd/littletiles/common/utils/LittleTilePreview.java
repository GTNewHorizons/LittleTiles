package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.material.LittleMaterial;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
import com.creativemd.littletiles.utils.ShiftHandler;

public final class LittleTilePreview {

    public boolean canSplit = true;
    public LittleTileSize size;

    public NBTTagCompound nbt;

    public LittleTileBox box;
    /**
     * Where the item shows the tile, kept when {@link PlacementHelper#getPreviewTiles} moves {@link #box} to where it
     * is placed. Null until then.
     */
    public LittleTileBox itemBox;

    public ArrayList<ShiftHandler> shifthandlers = new ArrayList<>();

    public LittleTilePreview(LittleTileBox box, NBTTagCompound nbt) {
        this(box.getSize(), nbt);
        this.box = box;
    }

    public LittleTilePreview(LittleTileSize size, NBTTagCompound nbt) {
        this.size = size;
        this.nbt = nbt;
    }

    public void updateSize() {
        size = box.getSize();
    }

    public LittleTile getLittleTile(TileEntityLittleTiles te) {
        return LittleTile.CreateandLoadTile(te, te.getWorldObj(), nbt);
    }

    /** The box the item shows the tile in: its own, or centered in a block if it has none. */
    public LittleTileBox getRenderBox() {
        return box != null ? box : new LittleTileBox(new LittleTileVec(8, 8, 8), size, true);
    }

    public CubeObject getCubeBlock() {
        LittleTileBox renderBox = getRenderBox();
        LittleTilesCubeObject cube = renderBox.getCube();
        if (nbt.hasKey("block")) {
            cube.block = Block.getBlockFromName(nbt.getString("block"));
            if (cube.block == null || cube.block instanceof BlockAir) cube.block = LittleTiles.missingBlock;
            cube.meta = nbt.getInteger("meta");
        } else {
            cube.block = Blocks.stone;
        }
        cube.cutoutInfo = LittleTileCutoutInfo.loadFromNBT(nbt);
        cube.geometryCache = new LittleTileGeometryCache(() -> renderBox, () -> cube.cutoutInfo);
        cube.cutsGeneration = cube.geometryCache.captureCutsGeneration();
        if (nbt.hasKey("color")) cube.color = nbt.getInteger("color");
        return cube;
    }

    public LittleTilePreview copy() {
        LittleTilePreview preview = new LittleTilePreview(
                size != null ? size.copy() : null,
                (NBTTagCompound) nbt.copy());
        preview.canSplit = this.canSplit;
        preview.shifthandlers = new ArrayList<>(this.shifthandlers);
        if (box != null) preview.box = box.copy();
        if (itemBox != null) preview.itemBox = itemBox.copy();
        return preview;
    }

    public static void flipPreview(NBTTagCompound nbt, ForgeDirection direction) {
        if (nbt.hasKey("bSize")) {
            int count = nbt.getInteger("bSize");
            for (int i = 0; i < count; i++) {
                LittleTileBox box = new LittleTileBox("bBox" + i, nbt);
                box.flipBoxWithCenter(direction, null);
                box.writeToNBT("bBox" + i, nbt);
            }
        }
    }

    /**
     * Size of the tile the preview describes, or null if the nbt carries neither a box nor a size. The bounding box
     * wins, since it is the actual extent of the tile. Multiblock previews (recipes) only ever carry boxes.
     */
    public static LittleTileSize getSizeFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("bBox0minX")) {
            return new LittleTileBox("bBox0", nbt).getSize();
        }
        if (nbt.hasKey("sizex")) {
            return new LittleTileSize("size", nbt);
        }
        return null;
    }

    public static void rotatePreview(NBTTagCompound nbt, ForgeDirection direction) {
        if (nbt.hasKey("sizex")) {
            LittleTileSize size = new LittleTileSize("size", nbt);
            size.rotateSize(direction);
            size.writeToNBT("size", nbt);
        }
        if (nbt.hasKey("bSize")) {
            int count = nbt.getInteger("bSize");
            for (int i = 0; i < count; i++) {
                LittleTileBox box = new LittleTileBox("bBox" + i, nbt);
                box.rotateBox(direction);
                box.writeToNBT("bBox" + i, nbt);
            }
        }
    }

    public static LittleTilePreview getPreviewFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        // everything reading the block of a preview reads it from its nbt
        LittleMaterial.migrateNBT(nbt);
        LittleTileSize size = null;
        LittleTileBox box = null;
        if (nbt.hasKey("sizex")) size = new LittleTileSize("size", nbt);
        // The box the tile saved, where it was in its block. Recipes' tiles have it, and so do picked or dropped ones.
        if (nbt.hasKey("bBox0minX")) {
            box = new LittleTileBox("bBox0", nbt);
            if (size == null) size = box.getSize();
        }

        if (size != null) {
            LittleTilePreview preview = new LittleTilePreview(size, nbt);
            preview.box = box;
            return preview;
        } else {
            return null;
        }
    }

}
