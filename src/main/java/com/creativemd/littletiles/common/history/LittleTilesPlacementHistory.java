package com.creativemd.littletiles.common.history;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public final class LittleTilesPlacementHistory {

    private static final int MAX_HISTORY = 30;
    private static final HashMap<UUID, Deque<PlacementAction>> undoHistory = new HashMap<>();
    private static final HashMap<UUID, Deque<PlacementAction>> redoHistory = new HashMap<>();

    private LittleTilesPlacementHistory() {}

    public static void recordPlacement(EntityPlayer player, PlacementAction action) {
        if (player == null || action == null || action.beforeStates.isEmpty() || action.afterStates.isEmpty()) {
            return;
        }

        UUID id = player.getUniqueID();
        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        if (player.capabilities.isCreativeMode) {
            undoStack.push(action);
            trimStack(undoStack);
        } else {
            // Non-creative players only keep their latest place action.
            undoStack.clear();
            undoStack.push(action);
        }
        getStack(redoHistory, id).clear();
    }

    public static void undo(EntityPlayer player) {
        if (player == null) {
            return;
        }

        UUID id = player.getUniqueID();
        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        if (undoStack.isEmpty()) {
            return;
        }

        PlacementAction action = undoStack.pop();
        if (!action.applyUndo(player)) {
            return;
        }

        if (player.capabilities.isCreativeMode) {
            Deque<PlacementAction> redoStack = getStack(redoHistory, id);
            redoStack.push(action);
            trimStack(redoStack);
        } else {
            getStack(redoHistory, id).clear();
        }
    }

    public static void redo(EntityPlayer player) {
        if (player == null || !player.capabilities.isCreativeMode) {
            return;
        }

        UUID id = player.getUniqueID();
        Deque<PlacementAction> redoStack = getStack(redoHistory, id);
        if (redoStack.isEmpty()) {
            return;
        }

        PlacementAction action = redoStack.pop();
        if (!action.applyRedo(player)) {
            return;
        }

        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        undoStack.push(action);
        trimStack(undoStack);
    }

    public static final class PlacementAction {

        public final int dimensionId;
        public final ArrayList<BlockSnapshot> beforeStates;
        public final ArrayList<BlockSnapshot> afterStates;

        public PlacementAction(int dimensionId, ArrayList<BlockSnapshot> beforeStates,
                ArrayList<BlockSnapshot> afterStates) {
            this.dimensionId = dimensionId;
            this.beforeStates = beforeStates;
            this.afterStates = afterStates;
        }

        private boolean applyUndo(EntityPlayer player) {
            World world = player.worldObj;
            if (world == null || world.provider == null || world.provider.dimensionId != dimensionId) {
                return false;
            }

            applySnapshots(world, beforeStates);
            return true;
        }

        private boolean applyRedo(EntityPlayer player) {
            World world = player.worldObj;
            if (world == null || world.provider == null || world.provider.dimensionId != dimensionId) {
                return false;
            }

            applySnapshots(world, afterStates);
            return true;
        }
    }

    public static final class BlockSnapshot {

        public final int x;
        public final int y;
        public final int z;
        public final String blockName;
        public final int meta;
        public final NBTTagCompound tileEntity;

        private BlockSnapshot(int x, int y, int z, String blockName, int meta, NBTTagCompound tileEntity) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockName = blockName;
            this.meta = meta;
            this.tileEntity = tileEntity;
        }
    }

    public static ArrayList<BlockSnapshot> captureSnapshots(World world, ArrayList<ChunkCoordinates> positions) {
        ArrayList<BlockSnapshot> snapshots = new ArrayList<>();
        if (world == null || positions == null) {
            return snapshots;
        }

        for (ChunkCoordinates pos : positions) {
            int x = pos.posX;
            int y = pos.posY;
            int z = pos.posZ;
            Block block = world.getBlock(x, y, z);
            String blockName = (String) Block.blockRegistry.getNameForObject(block);
            int meta = world.getBlockMetadata(x, y, z);

            NBTTagCompound tileNbt = null;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile != null) {
                tileNbt = new NBTTagCompound();
                tile.writeToNBT(tileNbt);
            }
            snapshots.add(new BlockSnapshot(x, y, z, blockName, meta, copy(tileNbt)));
        }
        return snapshots;
    }

    private static Deque<PlacementAction> getStack(HashMap<UUID, Deque<PlacementAction>> storage, UUID id) {
        Deque<PlacementAction> stack = storage.get(id);
        if (stack == null) {
            stack = new ArrayDeque<>();
            storage.put(id, stack);
        }
        return stack;
    }

    private static void trimStack(Deque<PlacementAction> stack) {
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    private static void applySnapshots(World world, ArrayList<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            Block block = snapshot.blockName == null ? null : Block.getBlockFromName(snapshot.blockName);
            if (block == null) {
                world.setBlockToAir(snapshot.x, snapshot.y, snapshot.z);
            } else {
                world.setBlock(snapshot.x, snapshot.y, snapshot.z, block, snapshot.meta, 3);
            }

            world.removeTileEntity(snapshot.x, snapshot.y, snapshot.z);
            if (snapshot.tileEntity != null) {
                TileEntity tile = TileEntity.createAndLoadEntity(copy(snapshot.tileEntity));
                if (tile != null) {
                    tile.setWorldObj(world);
                    tile.xCoord = snapshot.x;
                    tile.yCoord = snapshot.y;
                    tile.zCoord = snapshot.z;
                    world.setTileEntity(snapshot.x, snapshot.y, snapshot.z, tile);
                    tile.markDirty();
                }
            }

            world.markBlockForUpdate(snapshot.x, snapshot.y, snapshot.z);
        }
    }

    private static NBTTagCompound copy(NBTTagCompound tag) {
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }
}
