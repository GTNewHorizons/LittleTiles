package com.creativemd.littletiles.common.gui.bag;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.creativemd.littletiles.common.bag.LittleBagItemHandler;

/**
 * A slot of the little bag gui. The bag limits what it takes in tiles rather than in items, so how much of a stack fits
 * depends on the stack, which vanilla only asks for on some of its paths.
 */
public class LittleBagSlot extends ModularSlot {

    private final LittleBagItemHandler handler;
    private final int slot;

    public LittleBagSlot(LittleBagItemHandler handler, int slot) {
        super(handler, slot);
        this.handler = handler;
        this.slot = slot;
    }

    /**
     * Vanilla quick craft (dragging a stack over several slots) is the one insert path that asks for the generic slot
     * limit instead of the stack specific one. It debits the cursor by whatever that limit allows and only then hands
     * the stack to the slot, so a limit that is too generous would overfill the bag.
     */
    @Override
    public int getSlotStackLimit() {
        // The limit is asked for before the slot is bound to its sync handler, which is where the player comes from.
        if (!isInitialized()) return super.getSlotStackLimit();
        ItemStack heldStack = getPlayer().inventory.getItemStack();
        if (heldStack == null) return super.getSlotStackLimit();
        return this.handler.getStackLimit(this.slot, heldStack);
    }

    /**
     * Vanilla hotbar swap (pressing a number key over an empty slot) asks whether the stack in the hotbar is valid and
     * then puts all of it into the slot. The slot accepts any stack of which at least one item fits, so a stack that
     * still lies in the hotbar has to fit completely. Every other path clamps to the limit afterwards: a click takes
     * the stack from the cursor and shift click works on a copy.
     */
    @Override
    public boolean isItemValid(ItemStack stack) {
        if (!super.isItemValid(stack)) return false;
        if (!isInitialized() || !isInHotbar(stack)) return true;
        return this.handler.getStackLimit(this.slot, stack) >= stack.stackSize;
    }

    private boolean isInHotbar(ItemStack stack) {
        InventoryPlayer inventory = getPlayer().inventory;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            if (inventory.getStackInSlot(i) == stack) return true;
        }
        return false;
    }
}
