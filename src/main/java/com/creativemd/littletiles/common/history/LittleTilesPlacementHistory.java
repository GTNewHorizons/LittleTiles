package com.creativemd.littletiles.common.history;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public final class LittleTilesPlacementHistory {

    private static final int MAX_HISTORY = 30;
    private static final HashMap<UUID, Deque<PlacementAction>> undoHistory = new HashMap<>();
    private static final HashMap<UUID, Deque<PlacementAction>> redoHistory = new HashMap<>();

    private LittleTilesPlacementHistory() {}

    public static void clear() {
        undoHistory.clear();
        redoHistory.clear();
    }

    public static void recordAction(EntityPlayer player, PlacementAction action) {
        if (player == null || player.worldObj == null
                || player.worldObj.isRemote
                || !player.capabilities.isCreativeMode
                || action == null
                || action.plan == null
                || action.plan.isEmpty()) {
            return;
        }

        UUID id = player.getUniqueID();
        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        undoStack.push(action);
        trimStack(undoStack);
        getStack(redoHistory, id).clear();
    }

    public static void undo(EntityPlayer player) {
        if (player == null || !player.capabilities.isCreativeMode) {
            return;
        }

        UUID id = player.getUniqueID();
        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        if (undoStack.isEmpty()) {
            return;
        }

        PlacementAction action = undoStack.peek();
        if (!action.applyUndo(player)) {
            player.addChatMessage(new ChatComponentTranslation("littletiles.history.undo_failed"));
            return;
        }
        undoStack.pop();

        Deque<PlacementAction> redoStack = getStack(redoHistory, id);
        redoStack.push(action);
        trimStack(redoStack);
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

        PlacementAction action = redoStack.peek();
        if (!action.applyRedo(player)) {
            player.addChatMessage(new ChatComponentTranslation("littletiles.history.redo_failed"));
            return;
        }
        redoStack.pop();

        Deque<PlacementAction> undoStack = getStack(undoHistory, id);
        undoStack.push(action);
        trimStack(undoStack);
    }

    public static final class PlacementAction {

        public final int dimensionId;
        public final LittleTileChangePlan plan;

        public PlacementAction(int dimensionId, LittleTileChangePlan plan) {
            this.dimensionId = dimensionId;
            this.plan = plan;
        }

        private boolean applyUndo(EntityPlayer player) {
            World world = player.worldObj;
            if (world == null || world.provider == null || world.provider.dimensionId != dimensionId) {
                return false;
            }

            return plan.invert().apply(world);
        }

        private boolean applyRedo(EntityPlayer player) {
            World world = player.worldObj;
            if (world == null || world.provider == null || world.provider.dimensionId != dimensionId) {
                return false;
            }

            return plan.apply(world);
        }
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

}
