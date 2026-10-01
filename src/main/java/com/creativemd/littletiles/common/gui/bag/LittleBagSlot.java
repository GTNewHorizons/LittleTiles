package com.creativemd.littletiles.common.gui.bag;

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
     * the stack to the slot, so a limit that is too generous makes the handler reject a stack the player has already
     * paid for.
     */
    @Override
    public int getSlotStackLimit() {
        // The limit is asked for before the slot is bound to its sync handler, which is where the player comes from.
        if (!isInitialized()) return super.getSlotStackLimit();
        ItemStack heldStack = getPlayer().inventory.getItemStack();
        if (heldStack == null) return super.getSlotStackLimit();
        return this.handler.getStackLimit(this.slot, heldStack);
    }
}
