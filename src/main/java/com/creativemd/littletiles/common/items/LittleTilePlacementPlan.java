package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTile.LittleTilePosition;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileCoord;
import com.creativemd.littletiles.utils.PreviewTile;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

public class LittleTilePlacementPlan {

    private static class PlannedCollisionTile {

        public final LittleTileBox box;
        public final LittleTileCutoutInfo cutout;

        public PlannedCollisionTile(LittleTileBox box, LittleTileCutoutInfo cutout) {
            this.box = box;
            this.cutout = cutout;
        }
    }

    public static final class SplitPreviewMap
            extends Object2ObjectLinkedOpenHashMap<ChunkCoordinates, ArrayList<PreviewTile>> {
    }

    private static class PlacementEntry {

        public final ChunkCoordinates coord;
        public final ArrayList<PreviewTile> placeTiles;

        public PlacementEntry(ChunkCoordinates coord, ArrayList<PreviewTile> placeTiles) {
            this.coord = coord;
            this.placeTiles = placeTiles;
        }
    }

    private final ArrayList<PlacementEntry> entries = new ArrayList<>();
    private final ArrayList<SoundType> soundsToBePlayed = new ArrayList<>();
    private boolean canApplyPlan;
    private LittleTilePlaceMode placeMode;
    private LittleTilePosition structureMainPosition;
    private int originX;
    private int originY;
    private int originZ;
    private LittleTileCutoutInfo cutoutInfo;

    public void fillPlan(World world, int x, int y, int z, ArrayList<PreviewTile> previews, LittleStructure structure,
            LittleTilePlaceMode placeMode, LittleTileCutoutInfo cutoutInfo) {
        this.placeMode = placeMode;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.cutoutInfo = cutoutInfo;
        if (previews.isEmpty()) {
            canApplyPlan = false;
            return;
        }
        boolean specialPlaceMode = placeMode != LittleTilePlaceMode.NORMAL && structure == null;
        canApplyPlan = tryFillPlan(world, x, y, z, previews, specialPlaceMode);
    }

    public boolean canApplyPlan() {
        return canApplyPlan;
    }

    public boolean applyPlan(World world, EntityPlayer player, ItemStack stack, LittleStructure structure,
            ArrayList<LittleTile> unplaceableTiles) {
        structureMainPosition = null;
        soundsToBePlayed.clear();
        boolean didPlace = false;
        for (PlacementEntry entry : entries) {
            TileEntityLittleTiles tile = getOrCreateTileEntity(world, entry);
            if (tile == null) {
                continue;
            }
            for (PreviewTile placeTile : entry.placeTiles) {
                didPlace |= applyTile(entry, placeTile, tile, player, stack, structure, unplaceableTiles);
            }
            if (structure != null) tile.combineTiles(structure);
        }
        for (SoundType soundType : soundsToBePlayed) {
            playTileSound(world, player, soundType);
        }
        return didPlace;
    }

    private boolean tryFillPlan(World world, int x, int y, int z, ArrayList<PreviewTile> previews,
            boolean specialPlaceMode) {
        entries.clear();

        SplitPreviewMap splittedTiles = getSplittedTiles(previews, x, y, z);
        if (splittedTiles == null) return false;

        // specialPlaceMode means: place what fits, handle overlaps in a custom fashion
        // Otherwise placement is atomic — any unplaceable coord rejects the whole plan
        for (var entry : splittedTiles.object2ObjectEntrySet()) {
            ChunkCoordinates coord = entry.getKey();
            ArrayList<PreviewTile> placeTiles = entry.getValue();
            TileEntityLittleTiles tile = getTileEntity(world, coord);
            Block block = world.getBlock(coord.posX, coord.posY, coord.posZ);

            // STENCIL only removes overlapping tiles, so it needs an existing TE — otherwise we'd stamp
            // an empty BlockTile onto air. Skip the coord rather than rejecting the whole plan.
            if (placeMode == LittleTilePlaceMode.STENCIL && tile == null) continue;

            // The coord is usable if it already hosts a LittleTiles TE (we'll merge into it) or holds a
            // replaceable non-BlockTile we can overwrite.
            boolean canPlaceHere = tile != null
                    || (!(block instanceof BlockTile) && block.getMaterial().isReplaceable());
            if (!canPlaceHere) {
                if (specialPlaceMode) continue;
                return false;
            }

            // Previews that don't need a collision test are markers/visual-only and never get placed.
            if (placeTiles == null || !needsCollisionTest(placeTiles)) continue;

            // Collision check against an existing LittleTiles TE. Special place mode bypasses this on
            // purpose — overriding collisions is its whole reason to exist.
            if (!specialPlaceMode && !isSpaceForTiles(tile, placeTiles, coord)) {
                return false;
            }

            entries.add(new PlacementEntry(coord, placeTiles));
        }
        return true;
    }

    private static SplitPreviewMap getSplittedTiles(ArrayList<PreviewTile> tiles, int x, int y, int z) {
        SplitPreviewMap splitted = new SplitPreviewMap();
        for (PreviewTile tile : tiles) {
            if (!tile.split(splitted, x, y, z)) return null;
        }
        return splitted;
    }

    private static boolean needsCollisionTest(ArrayList<PreviewTile> placeTiles) {
        for (PreviewTile tile : placeTiles) {
            if (tile.needsCollisionTest()) {
                return true;
            }
        }
        return false;
    }

    private static TileEntityLittleTiles getTileEntity(World world, ChunkCoordinates coord) {
        TileEntity te = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
        if (te instanceof TileEntityLittleTiles lte) {
            return lte;
        }
        return null;
    }

    private boolean applyTile(PlacementEntry entry, PreviewTile placeTile, TileEntityLittleTiles tile,
            EntityPlayer player, ItemStack stack, LittleStructure structure, ArrayList<LittleTile> unplaceableTiles) {
        LittleTileCutoutInfo baseCutoutInfo = getBaseCutoutInfo(placeTile, this.cutoutInfo);
        LittleTileCutoutInfo cutoutInfoCurrent = getCutoutInfoCurrent(entry.coord, placeTile, this.cutoutInfo);
        // Mesh-backed fragments can clip to empty space when split across blocks.
        // In that case we skip placement for this fragment instead of placing a full box tile.
        if (baseCutoutInfo != null && cutoutInfoCurrent == null) {
            return false;
        }

        List<LittleTile> tiles = placeTile
                .placeTile(player, stack, tile, structure, unplaceableTiles, placeMode, cutoutInfoCurrent);
        if (tiles == null) {
            return false;
        }

        boolean didPlace = false;
        for (LittleTile littleTile : tiles) {
            didPlace = true;
            if (structure != null) {
                if (structureMainPosition == null) {
                    structure.mainTile = littleTile;
                    littleTile.isMainBlock = true;
                    littleTile.updateCorner();
                    structureMainPosition = new LittleTilePosition(entry.coord, littleTile.cornerVec.copy());
                } else {
                    littleTile.coord = new LittleTileCoord(
                            tile,
                            structureMainPosition.coord,
                            structureMainPosition.position);
                }
            }
            if (!soundsToBePlayed.contains(littleTile.getSound())) soundsToBePlayed.add(littleTile.getSound());
        }
        return didPlace;
    }

    private static LittleTileCutoutInfo getBaseCutoutInfo(PreviewTile placeTile, LittleTileCutoutInfo fallbackCutout) {
        if (fallbackCutout != null) {
            return new LittleTileCutoutInfo(fallbackCutout);
        }
        if (placeTile.preview != null && placeTile.preview.nbt != null) {
            return LittleTileCutoutInfo.loadFromNBT(placeTile.preview.nbt);
        }
        return null;
    }

    private LittleTileCutoutInfo getCutoutInfoCurrent(ChunkCoordinates coord, PreviewTile placeTile,
            LittleTileCutoutInfo cutoutInfo) {
        LittleTileCutoutInfo cutoutInfoCurrent = getBaseCutoutInfo(placeTile, cutoutInfo);
        if (cutoutInfoCurrent == null) {
            return null;
        }

        LittleTileBox currentBox = placeTile.box;
        LittleTileBox originalBox = placeTile.preview.box;

        cutoutInfoCurrent.pos.x += (originX - coord.posX) * 16 + originalBox.minX - currentBox.minX;
        cutoutInfoCurrent.pos.y += (originY - coord.posY) * 16 + originalBox.minY - currentBox.minY;
        cutoutInfoCurrent.pos.z += (originZ - coord.posZ) * 16 + originalBox.minZ - currentBox.minZ;

        Mesh3d mesh = Mesh3dUtil.meshFromTile(currentBox, cutoutInfoCurrent);
        if (mesh.getTriangles().isEmpty()) {
            return null;
        }
        return cutoutInfoCurrent;
    }

    private static void playTileSound(World world, EntityPlayer player, SoundType soundType) {
        world.playSoundEffect(
                (float) player.posX,
                (float) player.posY,
                (float) player.posZ,
                soundType.func_150496_b(),
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F);
    }

    private static TileEntityLittleTiles getOrCreateTileEntity(World world, PlacementEntry entry) {
        ChunkCoordinates coord = entry.coord;
        TileEntityLittleTiles tile = getTileEntity(world, coord);
        if (tile != null) {
            return tile;
        }

        world.setBlock(coord.posX, coord.posY, coord.posZ, LittleTiles.blockTile, 0, 3);
        tile = getTileEntity(world, coord);
        return tile;
    }

    private PlannedCollisionTile getPlannedCollisionTile(PreviewTile previewTile,
            LittleTileCutoutInfo cutoutInfoCurrent) {
        LittleTileBox box = previewTile.box.copy();
        LittleTileCutoutInfo effectiveCutout = cutoutInfoCurrent;

        if (previewTile.preview != null && previewTile.preview.nbt != null) {
            LittleTile tile = LittleTile.CreateandLoadTile(null, null, previewTile.preview.nbt);
            if (tile != null) {
                tile.boundingBox = box.copy();
                tile.updateCorner();
                if (cutoutInfoCurrent != null) {
                    tile.setCutoutInfo(cutoutInfoCurrent);
                }
                effectiveCutout = tile.getCutoutInfo();
            }
        }

        return new PlannedCollisionTile(box, effectiveCutout);
    }

    private boolean isSpaceForTiles(TileEntityLittleTiles mainTile, ArrayList<PreviewTile> placeTiles,
            ChunkCoordinates coord) {
        for (PreviewTile tile : placeTiles) {
            if (!tile.needsCollisionTest()) continue;

            LittleTileCutoutInfo baseCutoutInfo = getBaseCutoutInfo(tile, cutoutInfo);
            LittleTileCutoutInfo perTileCutout = null;
            if (baseCutoutInfo != null) {
                perTileCutout = getCutoutInfoCurrent(coord, tile, cutoutInfo);
                if (perTileCutout == null) {
                    continue;
                }
            }

            PlannedCollisionTile collisionTile = getPlannedCollisionTile(tile, perTileCutout);

            // Check against already existing tiles in target block.
            if (mainTile != null && !mainTile.isSpaceForLittleTile(collisionTile.box, collisionTile.cutout)) {
                return false;
            }
        }
        return true;
    }
}
