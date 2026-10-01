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
import com.creativemd.littletiles.common.material.LittleMaterialStack;
import com.creativemd.littletiles.common.material.LittleMaterialValuator;
import com.creativemd.littletiles.common.material.LittleTileItemType;

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

    public LittleBagItemHandler(Supplier<ItemStack> bagGetter, Consumer<ItemStack> bagSetter) {
        super(0);
        this.storage = new LittleBagStorage(bagGetter, bagSetter);

        List<ItemStack> sorted = new ArrayList<>();
        List<LittleMaterialStack> materials = storage.read(MAX_TILES, getMaxMaterials()).getSortedMaterials();
        for (LittleMaterialStack material : materials) {
            int blocks = material.getBlocks();
            // A material can need more than one slot.
            while (blocks > 0) {
                int stackSize = Math.min(64, blocks);
                ItemStack stack = material.createItemStack(stackSize);
                if (stack == null) break;
                sorted.add(stack);
                blocks -= stackSize;
            }
            // Whatever does not add up to a whole block goes into an extra item.
            ItemStack partial = ItemPartialTiles
                    .create(material.material.blockName, material.material.meta, material.getRemainingTiles());
            if (partial != null) {
                sorted.add(partial);
            }
        }

        int usedRows = (int) Math.ceil((float) sorted.size() / COLUMNS);
        setSize((usedRows + EMPTY_ROWS) * COLUMNS);
        for (int slot = 0; slot < sorted.size(); slot++) {
            this.stacks.set(slot, sorted.get(slot));
        }
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
            tiles += LittleMaterialValuator.tilesOf(getStackInSlot(slot));
        }
        return tiles;
    }

    /** Amount of different materials stored in the bag. */
    public int getMaterialCount() {
        return getMaterials(-1).size();
    }

    /** How full the bag is, from 0 to 100. */
    public int getFillPercentage() {
        return (int) ((long) getTileCount() * 100 / MAX_TILES);
    }

    /**
     * The materials stored in the bag, keyed by {@link LittleMaterialStack#getKey()}. What lies in the given slot is
     * left out, the same way {@link #getFreeTiles(int)} does not count it as used.
     */
    private Set<String> getMaterials(int excludedSlot) {
        Set<String> materials = new HashSet<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            if (slot == excludedSlot) continue;
            for (LittleMaterialStack material : LittleMaterialValuator.stacksOf(getStackInSlot(slot))) {
                materials.add(material.getKey());
            }
        }
        return materials;
    }

    /**
     * Whether the materials of the given stack still fit next to those in the other slots. A stack whose materials are
     * all stored already always fits, no matter how full the bag is, so materials cannot be locked out of their own
     * slot. This also means a stack that was taken out of a slot always fits back in, which
     * {@link com.cleanroommc.modularui.utils.item.SlotItemHandler} relies on while it checks whether an item may be
     * inserted.
     */
    private boolean fitsMaterialLimit(int slot, ItemStack stack) {
        Set<String> materials = getMaterials(slot);
        for (LittleMaterialStack material : LittleMaterialValuator.stacksOf(stack)) {
            materials.add(material.getKey());
        }
        return materials.size() <= getMaxMaterials();
    }

    /**
     * Amount of items that may be stored in the given slot, which is limited by how full the bag is. The cap counts
     * tiles, not stacks, so how the tiles are spread over the slots does not matter.
     * <p>
     * This is what {@link #setStackInSlot(int, ItemStack)} accepts, and every gui path has to clamp to it before it
     * takes anything off the cursor. Vanilla quick craft does not on its own, see
     * {@link com.creativemd.littletiles.common.gui.bag.LittleBagSlot#getSlotStackLimit()}.
     */
    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        LittleTileItemType type = LittleTileItemType.detectItemType(stack);
        if (type == null) return 0;

        int stackTiles = LittleMaterialValuator.tilesOf(stack);
        if (stackTiles <= 0) return 0;
        if (!fitsMaterialLimit(slot, stack)) return 0;

        int freeTiles = getFreeTiles(slot);

        // Partial tiles are a single item that is either stored completely or not at all.
        if (type == LittleTileItemType.PARTIAL_TILE) {
            return ItemPartialTiles.getTiles(stack) <= freeTiles ? 1 : 0;
        }
        // Little tiles are worth the tiles they are made of, which is a different amount for every one of them.
        if (type.isLittleTile()) {
            int tilesPerItem = stackTiles / stack.stackSize;
            if (tilesPerItem <= 0) return 0;
            return Math.min(super.getStackLimit(slot, stack), freeTiles / tilesPerItem);
        }
        return Math.min(super.getStackLimit(slot, stack), freeTiles / LittleMaterialStack.TILES_PER_BLOCK);
    }

    /** Tiles that may still go into the given slot. What already lies in that slot does not count as used. */
    private int getFreeTiles(int slot) {
        int existing = LittleMaterialValuator.tilesOf(getStackInSlot(slot));
        return Math.max(MAX_TILES - getTileCount() + existing, 0);
    }

    /**
     * Replaces the content of the given slot. This is all or nothing: storing only a part of the stack would destroy
     * blocks, since the caller replaces whatever was in the slot before. The rejection is a guard against corrupt
     * content, not a safety net for the gui: whoever calls this has already taken the stack from the player, so a
     * rejected stack is lost. Callers have to clamp to {@link #getStackLimit(int, ItemStack)} beforehand.
     * <p>
     * The limit is measured in tiles rather than in items, so that a stack which was in the slot before always fits
     * back in. {@link com.cleanroommc.modularui.utils.item.SlotItemHandler} empties and refills slots while it checks
     * whether an item may be inserted, and a full bag must not swallow the stack it takes out for that check.
     */
    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (stack != null && LittleMaterialValuator.tilesOf(stack) > getFreeTiles(slot)) return;
        if (stack != null && !fitsMaterialLimit(slot, stack)) return;

        super.setStackInSlot(slot, stack);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return LittleTileItemType.detectItemType(stack) != null && LittleMaterialValuator.tilesOf(stack) > 0;
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
}
