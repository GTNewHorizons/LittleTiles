package com.creativemd.littletiles.common.placement;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil.TileCoverage;
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

public class LittleTilePlacementPlan extends BasePlan {

    public static final class SplitPreviewMap
            extends Object2ObjectLinkedOpenHashMap<ChunkCoordinates, ArrayList<PreviewTile>> {
    }

    private final ArrayList<PlacementEntry> entries = new ArrayList<>();
    private final ArrayList<SoundType> soundsToBePlayed = new ArrayList<>();
    private boolean canApplyPlan;
    private LittleTilePlaceMode placeMode;
    private LittleTilePosition structureMainPosition;
    private int originX;
    private int originY;
    private int originZ;

    public void fillPlan(World world, int x, int y, int z, ArrayList<PreviewTile> previews, LittleStructure structure,
            LittleTilePlaceMode placeMode) {
        this.placeMode = placeMode;
        this.originX = x;
        this.originY = y;
        this.originZ = z;

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

    public LittleTilePlacementPlanResult applyPlan(World world, EntityPlayer player, ItemStack stack,
            LittleStructure structure) {
        structureMainPosition = null;
        soundsToBePlayed.clear();
        // Freeze every coordinate before any placement can change shared structure data.
        LittleTileChangeRecorder recorder = new LittleTileChangeRecorder(world);
        for (PlacementEntry entry : entries) {
            recorder.watch(entry.coord);
        }
        // On failure nothing was placed, so the result is empty.
        applyEntries(world, entries, entry -> {
            TileEntityLittleTiles tile = entry.getTileEntity();
            for (Fragment fragment : entry.fragments) {
                applyTile(entry, fragment, tile, player, stack, structure);
            }
            if (structure != null) tile.combineTiles(structure);
        });
        for (SoundType soundType : soundsToBePlayed) {
            playTileSound(world, player, soundType);
        }
        return recorder.finish();
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
            if (!areStructuresLoaded(world, coord)) {
                if (specialPlaceMode) continue;
                return false;
            }
            TileEntityLittleTiles tile = getTileEntity(world, coord);
            Block block = world.getBlock(coord.posX, coord.posY, coord.posZ);

            // STENCIL only removes overlapping tiles, so it needs an existing TE — otherwise we'd stamp
            // an empty BlockTile onto air. Skip the coord rather than rejecting the whole plan.
            if (placeMode == LittleTilePlaceMode.STENCIL && tile == null) continue;

            if (!canHoldTiles(block, tile)) {
                if (specialPlaceMode) continue;
                return false;
            }

            // Previews that don't need a collision test are markers/visual-only and never get placed.
            if (placeTiles == null || !needsCollisionTest(placeTiles)) continue;

            ArrayList<Fragment> fragments = new ArrayList<>();
            for (PreviewTile placeTile : placeTiles) {
                Fragment fragment = createFragment(coord, placeTile);
                if (fragment != null) {
                    fragments.add(fragment);
                }
            }
            // Only mesh-backed fragments the shape misses entirely, nothing to place here.
            if (fragments.isEmpty()) continue;

            // Collision check against an existing LittleTiles TE. Special place mode bypasses this on
            // purpose — overriding collisions is its whole reason to exist.
            if (!specialPlaceMode && !isSpaceForTiles(tile, fragments)) {
                return false;
            }

            entries.add(new PlacementEntry(coord, fragments));
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

    private void applyTile(PlacementEntry entry, Fragment fragment, TileEntityLittleTiles tile, EntityPlayer player,
            ItemStack stack, LittleStructure structure) {
        // Each placed tile gets its own copy, the fragment stays as planned
        LittleTileCutoutInfo cutout = fragment.cutout == null ? null : new LittleTileCutoutInfo(fragment.cutout);
        List<LittleTile> tiles = fragment.tile.placeTile(player, stack, tile, structure, placeMode, cutout);
        if (tiles == null) {
            return;
        }

        for (LittleTile littleTile : tiles) {
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
    }

    private static LittleTileCutoutInfo getBaseCutoutInfo(PreviewTile placeTile) {
        if (placeTile.preview != null && placeTile.preview.nbt != null) {
            return LittleTileCutoutInfo.loadFromNBT(placeTile.preview.nbt);
        }
        return null;
    }

    /**
     * Works out what a preview turns into within the block at {@code coord}. Null when a mesh-backed preview clips to
     * empty space there, which is skipped instead of being placed as a full box tile.
     */
    private Fragment createFragment(ChunkCoordinates coord, PreviewTile placeTile) {
        LittleTileCutoutInfo cutout = getCutoutInfoCurrent(coord, placeTile);
        if (cutout == null) {
            return new Fragment(placeTile, null);
        }

        // The box is this plan's own copy from splitting the preview, so it can be shrunk in place
        TileCoverage coverage = Mesh3dUtil.fitTile(placeTile.box, cutout);
        if (coverage == TileCoverage.EMPTY) {
            return null;
        }
        // Fragments the shape fills completely look the same as a plain box tile, which is far cheaper.
        if (coverage == TileCoverage.FULL) {
            return new Fragment(placeTile, null);
        }
        return new Fragment(placeTile, cutout);
    }

    private LittleTileCutoutInfo getCutoutInfoCurrent(ChunkCoordinates coord, PreviewTile placeTile) {
        LittleTileCutoutInfo cutoutInfoCurrent = getBaseCutoutInfo(placeTile);
        if (cutoutInfoCurrent == null) {
            return null;
        }

        LittleTileBox currentBox = placeTile.box;
        LittleTileBox originalBox = placeTile.preview.box;

        cutoutInfoCurrent.pos.x += (originX - coord.posX) * 16 + originalBox.minX - currentBox.minX;
        cutoutInfoCurrent.pos.y += (originY - coord.posY) * 16 + originalBox.minY - currentBox.minY;
        cutoutInfoCurrent.pos.z += (originZ - coord.posZ) * 16 + originalBox.minZ - currentBox.minZ;
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

    private static boolean isSpaceForTiles(TileEntityLittleTiles mainTile, ArrayList<Fragment> fragments) {
        for (Fragment fragment : fragments) {
            if (!fragment.tile.needsCollisionTest()) continue;

            // Check against already existing tiles in target block.
            if (mainTile != null && !mainTile.isSpaceForLittleTile(fragment.tile.box.copy(), fragment.cutout)) {
                return false;
            }
        }
        return true;
    }
}
