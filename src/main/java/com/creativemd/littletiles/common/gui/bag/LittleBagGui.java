package com.creativemd.littletiles.common.gui.bag;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.creativemd.littletiles.common.bag.LittleBagItemHandler;

/**
 * The gui of a little bag. It shows the inventory of the bag it was opened from, see {@link LittleBagItemHandler}.
 */
public class LittleBagGui {

    /** Amount of rows shown at once. Everything above that is scrolled. */
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = 196;
    private static final int SCROLL_BAR_WIDTH = 4;
    /** Space above the info lines. */
    private static final int TOP_MARGIN = 8;
    /** Height of the player inventory below the slots, including the space around it. */
    private static final int PLAYER_INVENTORY_HEIGHT = 88;
    /** Height of a single info line above the slots. */
    private static final int INFO_HEIGHT = 12;
    /** Amount of info lines above the slots. The hint needs two of them, as it is too long for a single one. */
    private static final int INFO_ROWS = 3;
    /** Space between the two info texts. */
    private static final int INFO_GAP = 4;

    private static final String SLOT_GROUP = "little_bag";

    private LittleBagGui() {}

    public static ModularPanel build(PlayerInventoryGuiData data, PanelSyncManager syncManager) {
        LittleBagItemHandler itemHandler = new LittleBagItemHandler(data::getUsedItemStack, data::setUsedItemStack);
        syncManager.registerSlotGroup(SLOT_GROUP, LittleBagItemHandler.COLUMNS);

        // The bag is always opened from the main hand, so it always lies in the inventory shown below. It has to be
        // locked there, otherwise the gui would keep editing an item the player does not hold anymore.
        int bagSlot = data.getSlotIndex();
        syncManager.bindPlayerInventory(
                data.getPlayer(),
                (inventory, index) -> new ModularSlot(inventory, index)
                        .accessibility(index != bagSlot, index != bagSlot));

        int rows = itemHandler.getRows();
        int visibleRows = Math.min(rows, MAX_VISIBLE_ROWS);
        int width = LittleBagItemHandler.COLUMNS * SLOT_SIZE + SCROLL_BAR_WIDTH;
        int x = (PANEL_WIDTH - width) / 2;

        // Both texts cover the whole line and only differ in where they are aligned, so that neither of them has to
        // give up space the other one may need in a language where it is longer.
        TextWidget<?> fill = new TextWidget<>(
                IKey.lang("littletiles.little_bag.fill", () -> new Object[] { itemHandler.getFillPercentage() }))
                        .pos(x, TOP_MARGIN).size(width, INFO_HEIGHT);
        TextWidget<?> materials = new TextWidget<>(
                IKey.lang(
                        "littletiles.little_bag.materials",
                        () -> new Object[] { itemHandler.getMaterialCount(), itemHandler.getMaxMaterials() }))
                                .textAlign(Alignment.CenterRight).pos(x, TOP_MARGIN).size(width, INFO_HEIGHT);
        // The bag melts everything down, which the player should know before dropping a build into it.
        TextWidget<?> hint = new TextWidget<>(IKey.lang("littletiles.little_bag.hint"))
                .pos(x, TOP_MARGIN + INFO_HEIGHT + INFO_GAP).size(width, (INFO_ROWS - 1) * INFO_HEIGHT);

        // The bag can have any amount of rows, so the slots are always scrollable. If everything fits there is simply
        // nothing to scroll.
        Grid slots = new Grid()
                .gridOfWidthHeight(
                        LittleBagItemHandler.COLUMNS,
                        rows,
                        (column, row, index) -> new ItemSlot()
                                .slot(new LittleBagSlot(itemHandler, index).slotGroup(SLOT_GROUP)))
                .scrollable(new VerticalScrollData()).size(width, visibleRows * SLOT_SIZE)
                .pos(x, TOP_MARGIN + INFO_ROWS * INFO_HEIGHT + INFO_GAP);

        int height = TOP_MARGIN + INFO_ROWS * INFO_HEIGHT
                + INFO_GAP
                + visibleRows * SLOT_SIZE
                + PLAYER_INVENTORY_HEIGHT;
        return ModularPanel.defaultPanel(SLOT_GROUP).size(PANEL_WIDTH, height).child(fill).child(materials).child(hint)
                .child(slots).child(SlotGroupWidget.playerInventory(7, true));
    }
}
