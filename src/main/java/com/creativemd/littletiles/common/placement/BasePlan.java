package com.creativemd.littletiles.common.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

/** Shared logic of plans which change the tiles of several blocks at once. */
abstract class BasePlan {

    /**
     * Applies every entry or none of them. All blocks are created before the first entry is applied, so applying an
     * entry must not fail anymore.
     *
     * @return false if a block could not be created, the world is unchanged then
     */
    static <E extends BaseEntry> boolean applyEntries(World world, List<E> entries, Consumer<E> apply) {
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).prepare(world)) {
                // The failed entry may have replaced a block as well
                for (int j = 0; j <= i; j++) {
                    entries.get(j).unprepare(world);
                }
                return false;
            }
        }

        for (E entry : entries) {
            apply.accept(entry);
        }
        return true;
    }

    /** Tiles can go into a block with a tile entity already, or into a replaceable block which is overwritten. */
    static boolean canHoldTiles(Block block, TileEntityLittleTiles tile) {
        return tile != null || (!(block instanceof BlockTile) && block.getMaterial().isReplaceable());
    }

    /**
     * A structure is destroyed as a whole, so every structure in the block has to be loaded completely. Otherwise the
     * unloaded part survives the change and is not recorded either.
     * <p>
     * Has to run before the tile entity of the block is looked up: Linking a structure removes tiles whose structure is
     * gone, which can remove the tile entity as well.
     */
    static boolean areStructuresLoaded(World world, ChunkCoordinates coord) {
        TileEntityLittleTiles tileEntity = getTileEntity(world, coord);
        if (tileEntity == null) return true;
        for (LittleTile tile : new ArrayList<>(tileEntity.getTiles())) {
            if (!tile.isStructureBlock) continue;
            if (!tile.isLoaded() || !tile.structure.hasLoaded()) return false;
        }
        return true;
    }

    static TileEntityLittleTiles getTileEntity(World world, ChunkCoordinates coord) {
        TileEntity tile = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
        if (tile instanceof TileEntityLittleTiles littleTile) return littleTile;
        return null;
    }
}
