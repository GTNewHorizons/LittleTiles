package com.creativemd.littletiles.common.utils;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.CubeObject;
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

    public CubeObject getCubeBlock() {
        LittleTileBox renderBox = box != null ? box : new LittleTileBox(new LittleTileVec(8, 8, 8), size, true);
        LittleTilesCubeObject cube = renderBox.getCube();
        if (nbt.hasKey("block")) {
            cube.block = Block.getBlockFromName(nbt.getString("block"));
            cube.meta = nbt.getInteger("meta");
        } else {
            cube.block = Blocks.stone;
        }
        cube.cutoutInfo = LittleTileCutoutInfo.loadFromNBT(nbt);
        cube.geometryCache = new LittleTileGeometryCache(() -> renderBox, () -> cube.cutoutInfo);
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
        return preview;
    }

    public static void flipPreview(NBTTagCompound nbt, ForgeDirection direction) {
        if (nbt.hasKey("bBoxminX")) {
            LittleTileBox box = new LittleTileBox("bBox", nbt);
            box.flipBoxWithCenter(direction, null);
            box.writeToNBT("bBox", nbt);
        }
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
        if (nbt.hasKey("bBoxminX")) {
            return new LittleTileBox("bBox", nbt).getSize();
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
        if (nbt.hasKey("bBoxminX")) {
            LittleTileBox box = new LittleTileBox("bBox", nbt);
            box.rotateBox(direction);
            box.writeToNBT("bBox", nbt);
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
        LittleTileSize size = null;
        LittleTileBox box = null;
        if (nbt.hasKey("sizex")) size = new LittleTileSize("size", nbt);
        if (nbt.hasKey("bBoxminX")) {
            box = new LittleTileBox("bBox", nbt);
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
