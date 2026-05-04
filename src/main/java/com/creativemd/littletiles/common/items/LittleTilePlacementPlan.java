package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.creativecore.common.utils.HashMapList;
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

public class LittleTilePlacementPlan {

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
    private LittleTileBox originalBox;

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
        this.originalBox = previews.get(0).box;
        boolean specialPlaceMode = placeMode != LittleTilePlaceMode.NORMAL && structure == null;
        canApplyPlan = tryFillPlan(world, x, y, z, previews, specialPlaceMode);
    }

    public boolean canApplyPlan() {
        return canApplyPlan;
    }

    public boolean applyPlan(World world, EntityPlayer player, ItemStack stack, LittleStructure structure,
            ArrayList<LittleTile> unplaceableTiles, LittleTileCutoutInfo cutoutInfo) {
        structureMainPosition = null;
        soundsToBePlayed.clear();
        boolean didPlace = false;
        for (PlacementEntry entry : entries) {
            TileEntityLittleTiles tile = getOrCreateTileEntity(world, entry);
            for (PreviewTile placeTile : entry.placeTiles) {
                didPlace |= applyTile(entry, placeTile, tile, player, stack, structure, unplaceableTiles, cutoutInfo);
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

        HashMapList<ChunkCoordinates, PreviewTile> splittedTiles = getSplittedTiles(previews, x, y, z);
        if (splittedTiles == null) return false;

        // specialPlaceMode means: place what fits, handle overlaps in a custom fashion
        // Otherwise placement is atomic — any unplaceable coord rejects the whole plan
        for (int i = 0; i < splittedTiles.size(); i++) {
            ChunkCoordinates coord = splittedTiles.getKey(i);
            ArrayList<PreviewTile> placeTiles = splittedTiles.getValues(i);
            TileEntityLittleTiles tile = getTileEntity(world, coord);

            // The coord is usable if it already hosts a LittleTiles TE (we'll merge into it) or holds a
            // replaceable non-BlockTile we can overwrite. Anything else (solid block, BlockTile without a
            // TE, out of world) is fundamentally not placeable — special place mode can't fix that, only skip it.
            boolean canPlaceHere = tile != null || canCreateBlockTile(world, coord);
            if (!canPlaceHere) {
                if (specialPlaceMode) continue;
                return false;
            }

            // Previews that don't need a collision test are markers/visual-only and never get placed.
            if (placeTiles == null || !needsCollisionTest(placeTiles)) continue;

            // Collision check against an existing LittleTiles TE. Special place mode bypasses this on
            // purpose — overriding collisions is its whole reason to exist.
            if (!specialPlaceMode && tile != null && !isSpaceForTiles(tile, placeTiles)) {
                return false;
            }

            entries.add(new PlacementEntry(coord, placeTiles));
        }
        return true;
    }

    private static HashMapList<ChunkCoordinates, PreviewTile> getSplittedTiles(ArrayList<PreviewTile> tiles, int x,
            int y, int z) {
        HashMapList<ChunkCoordinates, PreviewTile> splitted = new HashMapList<>();
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
            EntityPlayer player, ItemStack stack, LittleStructure structure, ArrayList<LittleTile> unplaceableTiles,
            LittleTileCutoutInfo cutoutInfo) {
        LittleTileCutoutInfo cutoutInfoCurrent = getCutoutInfoCurrent(entry, placeTile, cutoutInfo);
        if (cutoutInfo != null && cutoutInfoCurrent == null) {
            return false;
        }

        List<LittleTile> tiles = placeTile.placeTile(player, stack, tile, structure, unplaceableTiles, placeMode);
        if (tiles == null) {
            return false;
        }

        boolean didPlace = false;
        for (LittleTile littleTile : tiles) {
            didPlace = true;
            littleTile.setCutoutInfo(cutoutInfoCurrent);
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

    private LittleTileCutoutInfo getCutoutInfoCurrent(PlacementEntry entry, PreviewTile placeTile,
            LittleTileCutoutInfo cutoutInfo) {
        if (cutoutInfo == null) {
            return null;
        }

        LittleTileBox currentBox = placeTile.box;

        LittleTileCutoutInfo cutoutInfoCurrent = new LittleTileCutoutInfo(cutoutInfo);
        cutoutInfoCurrent.pos.x += (originX - entry.coord.posX) * 16 + originalBox.minX - currentBox.minX;
        cutoutInfoCurrent.pos.y += (originY - entry.coord.posY) * 16 + originalBox.minY - currentBox.minY;
        cutoutInfoCurrent.pos.z += (originZ - entry.coord.posZ) * 16 + originalBox.minZ - currentBox.minZ;

        Mesh3d mesh = Mesh3dUtil.createMesh(
                0,
                0,
                0,
                cutoutInfoCurrent,
                currentBox.minX / 16.0,
                currentBox.minY / 16.0,
                currentBox.minZ / 16.0,
                currentBox.maxX / 16.0,
                currentBox.maxY / 16.0,
                currentBox.maxZ / 16.0,
                null,
                0);
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
        if (canCreateBlockTile(world, coord)) {
            world.setBlock(coord.posX, coord.posY, coord.posZ, LittleTiles.blockTile, 0, 3);
        }

        TileEntityLittleTiles tile = getTileEntity(world, coord);
        if (tile == null) {
            throw new IllegalStateException("Failed to resolve LittleTiles tile entity for placement plan");
        }
        return tile;
    }

    private static boolean canCreateBlockTile(World world, ChunkCoordinates coord) {
        return !(world.getBlock(coord.posX, coord.posY, coord.posZ) instanceof BlockTile)
                && world.getBlock(coord.posX, coord.posY, coord.posZ).getMaterial().isReplaceable();
    }

    private static boolean isSpaceForTiles(TileEntityLittleTiles mainTile, ArrayList<PreviewTile> placeTiles) {
        for (PreviewTile tile : placeTiles)
            if (tile.needsCollisionTest() && !(mainTile).isSpaceForLittleTile(tile.box)) return false;
        return true;
    }
}
