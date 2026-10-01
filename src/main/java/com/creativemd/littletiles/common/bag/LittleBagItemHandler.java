package com.creativemd.littletiles.common.bag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.creativemd.littletiles.LittleTilesConfig;
import com.creativemd.littletiles.common.items.ItemPartialTiles;
import com.creativemd.littletiles.common.material.LittleMaterial;
import com.creativemd.littletiles.common.material.LittleMaterialStack;
import com.creativemd.littletiles.common.material.LittleMaterialValuator;

/**
 * The inventory of a little bag, which is the view the gui works on. The bag has no fixed size: it is created with as
 * many rows as its contents need, plus {@link #EMPTY_ROWS} empty rows to fill it up. Once those are full the bag grows
 * again the next time it is opened.
 * <p>
 * The contents are sorted once when the inventory is created, which is what the player sees when opening the bag. Items
 * stay where the player puts them until the bag is opened again.
 * <p>
 * Tiles that don't add up to a whole block are shown as an extra partial tiles item behind the blocks of that material.
 * <p>
 * Little tiles can be stored as well. They keep their shape while they lie in a slot, but the bag only remembers the
 * material they are made of, so they come back as blocks and tiles once it is opened again.
 */
public class LittleBagItemHandler extends ItemStackHandler {

    public static final int COLUMNS = 9;
    /** How much a bag can hold in total. Only the amount of tiles matters, not how they are spread over the slots. */
    public static final int MAX_STACKS = 10;
    public static final int MAX_TILES = MAX_STACKS * 64 * LittleMaterialStack.TILES_PER_BLOCK;
    /** Amount of empty rows that is always available to fill the bag. */
    public static final int EMPTY_ROWS = 4;

    private final LittleBagStorage storage;
    /** What every slot is worth. Decomposing little tiles is too expensive to repeat on every frame. */
    private final StackValue[] slotValues;
    /** What the last stack offered to the bag is worth, usually the one on the cursor. */
    private StackValue incomingValue;

    public LittleBagItemHandler(Supplier<ItemStack> bagGetter, Consumer<ItemStack> bagSetter) {
        super(0);
        this.storage = new LittleBagStorage(bagGetter, bagSetter);

        List<ItemStack> sorted = new ArrayList<>();
        // Everything the bag holds is loaded, even above its limits, it only refuses new content then.
        List<LittleMaterialStack> materials = storage.read().getSortedMaterials();
        for (LittleMaterialStack materialStack : materials) {
            int blocks = materialStack.getBlocks();
            // A material can need more than one slot.
            while (blocks > 0) {
                ItemStack stack = materialStack.material.createItemStack(1);
                // Always take at least one block, so an item with a broken stack size cannot loop forever.
                stack.stackSize = Math.max(1, Math.min(stack.getMaxStackSize(), blocks));
                sorted.add(stack);
                blocks -= stack.stackSize;
            }
            // Whatever does not add up to a whole block goes into an extra item.
            ItemStack partial = ItemPartialTiles.create(materialStack.material, materialStack.getRemainingTiles());
            if (partial != null) {
                sorted.add(partial);
            }
        }

        int usedRows = (int) Math.ceil((float) sorted.size() / COLUMNS);
        setSize((usedRows + EMPTY_ROWS) * COLUMNS);
        for (int slot = 0; slot < sorted.size(); slot++) {
            this.stacks.set(slot, sorted.get(slot));
        }
        this.slotValues = new StackValue[getSlots()];
    }

    /** What the given slot is worth, recomputed only once it holds a different stack or a different amount. */
    private StackValue getSlotValue(int slot) {
        ItemStack stack = getStackInSlot(slot);
        StackValue value = slotValues[slot];
        if (value == null || !value.isFor(stack)) {
            value = new StackValue(stack);
            slotValues[slot] = value;
        }
        return value;
    }

    /**
     * What the given stack that is offered to the bag is worth. While dragging, the limit is asked for on every frame
     * with the same cursor stack, so only a different stack or amount is decomposed again.
     */
    private StackValue getIncomingValue(ItemStack stack) {
        if (incomingValue == null || !incomingValue.isFor(stack)) {
            incomingValue = new StackValue(stack);
        }
        return incomingValue;
    }

    public int getMaxMaterials() {
        return LittleTilesConfig.maxBagMaterials;
    }

    public int getRows() {
        return getSlots() / COLUMNS;
    }

    /** Amount of tiles stored in the bag. */
    public int getTileCount() {
        int tiles = 0;
        for (int slot = 0; slot < getSlots(); slot++) {
            tiles += getSlotValue(slot).tiles;
        }
        return tiles;
    }

    /** Amount of different materials stored in the bag. */
    public int getMaterialCount() {
        return getAllMaterials().size();
    }

    /** How full the bag is, from 0 to 100. */
    public int getFillPercentage() {
        return (int) ((long) getTileCount() * 100 / MAX_TILES);
    }

    /** The materials stored in the bag. */
    private Set<LittleMaterial> getAllMaterials() {
        return getMaterials(-1);
    }

    /**
     * The materials stored in the bag. What lies in the given slot is left out, the same way {@link #getFreeTiles(int)}
     * does not count it as used.
     */
    private Set<LittleMaterial> getMaterials(int excludedSlot) {
        Set<LittleMaterial> materials = new HashSet<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            if (slot == excludedSlot) continue;
            materials.addAll(getSlotValue(slot).materials);
        }
        return materials;
    }

    /**
     * Whether the materials of the given stack still fit next to those in the other slots. A stack whose materials are
     * all stored already always fits, no matter how full the bag is, so materials cannot be locked out of their own
     * slot. A bag that holds more materials than allowed, because the limit was lowered, keeps them and only refuses
     * new ones until enough were taken out.
     */
    private boolean fitsMaterialLimit(int slot, Set<LittleMaterial> stackMaterials) {
        Set<LittleMaterial> materials = getMaterials(slot);
        materials.addAll(stackMaterials);
        return materials.size() <= getMaxMaterials() || getAllMaterials().containsAll(stackMaterials);
    }

    /**
     * Amount of items that may be stored in the given slot, which is limited by how full the bag is. The cap counts
     * tiles, not stacks, so how the tiles are spread over the slots does not matter.
     * <p>
     * This is the only place the limits are enforced, the slots themselves accept anything. Every gui path has to clamp
     * to it before it takes anything off the cursor. Vanilla quick craft and hotbar swap do not on their own, see
     * {@link com.creativemd.littletiles.common.gui.bag.LittleBagSlot}.
     */
    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        StackValue value = getIncomingValue(stack);
        if (value.tiles <= 0 || !fitsMaterialLimit(slot, value.materials)) return 0;

        // Every item of a stack is worth the same: a whole block, the tiles a little tile is made of, or the content of
        // partial tiles. Only an edited stack of partial tiles can be worth less than one tile per item.
        int tilesPerItem = value.tiles / stack.stackSize;
        if (tilesPerItem <= 0) return 0;
        return Math.min(super.getStackLimit(slot, stack), getFreeTiles(slot) / tilesPerItem);
    }

    /** Tiles that may still go into the given slot. What already lies in that slot does not count as used. */
    private int getFreeTiles(int slot) {
        int existing = getSlotValue(slot).tiles;
        return Math.max(MAX_TILES - getTileCount() + existing, 0);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return getIncomingValue(stack).tiles > 0;
    }

    @Override
    protected void onContentsChanged(int slot) {
        save();
    }

    private void save() {
        LittleBagStock stock = new LittleBagStock();
        for (int slot = 0; slot < getSlots(); slot++) {
            // Little tiles stay intact as long as they sit in a slot, they are only decomposed when the bag is saved.
            stock.addItemStack(getStackInSlot(slot));
        }
        storage.write(stock);
    }

    /** What a stack is worth, see {@link LittleMaterialValuator}. */
    private static class StackValue {

        private final ItemStack stack;
        private final int stackSize;
        private final Set<LittleMaterial> materials = new HashSet<>();
        private final int tiles;

        private StackValue(ItemStack stack) {
            this.stack = stack;
            this.stackSize = stack == null ? 0 : stack.stackSize;
            long tiles = 0;
            for (LittleMaterialStack materialStack : LittleMaterialValuator.stacksOf(stack)) {
                materials.add(materialStack.material);
                tiles += materialStack.count;
            }
            this.tiles = (int) Math.min(tiles, Integer.MAX_VALUE);
        }

        /** Vanilla changes the size of a stack in place when it merges onto it, so the size is checked as well. */
        private boolean isFor(ItemStack stack) {
            return this.stack == stack && (stack == null || stack.stackSize == stackSize);
        }
    }
}
