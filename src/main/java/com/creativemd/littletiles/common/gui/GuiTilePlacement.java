package com.creativemd.littletiles.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.text.DynamicKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.DropDownMenu;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.PlacementSetting;

/**
 * The gui section for the {@link PlacementSetting}, which all tiles share. Changing it sets it on both sides: the
 * client right away, the server through the sync.
 */
public class GuiTilePlacement {

    /** The size of the panels, the same as the chisel's. */
    public static final int PANEL_WIDTH = 250;
    public static final int PANEL_HEIGHT = 300;
    /** How much room the section takes from the top of the panel. */
    public static final int HEIGHT = 114;
    private static final int MARGIN = 5;
    private static final String[] GRID_SIZES = { "1", "2", "4", "8", "16" };

    private final EntityPlayer player;
    private final IntSyncValue grid;
    private final IntSyncValue placeMode;

    /** A panel with only the section, for tiles with nothing else to configure. */
    public static ModularPanel panel(PanelSyncManager syncManager, EntityPlayer player) {
        ModularPanel panel = ModularPanel.defaultPanel("tile_placement");
        panel.size(PANEL_WIDTH, PANEL_HEIGHT);
        new GuiTilePlacement(syncManager, player).addTo(panel, PANEL_WIDTH);
        return panel;
    }

    /** Registers what the section syncs. The server has to do that as well, even if it builds no widgets. */
    public GuiTilePlacement(PanelSyncManager syncManager, EntityPlayer player) {
        this.player = player;
        grid = SyncHandlers
                .intNumber(() -> PlacementSetting.getGrid(player), value -> PlacementSetting.setGrid(player, value))
                .allowC2S();
        placeMode = SyncHandlers.intNumber(
                () -> PlacementSetting.getPlaceMode(player).ordinal(),
                value -> PlacementSetting.setPlaceMode(player, value)).allowC2S();
        syncManager.syncValue("lt_tiles_grid", grid);
        syncManager.syncValue("lt_tiles_place_mode", placeMode);
    }

    /**
     * Adds the section at the top of the panel, laid out like the top of the chisel's gui. Add it last, so its opened
     * lists are drawn over what is below.
     */
    public void addTo(ModularPanel panel, int width) {
        panel.child(
                IKey.str("Shared by all tiles, except the chisel").style(EnumChatFormatting.DARK_GRAY).asWidget()
                        .pos(MARGIN, 90));
        panel.child(new Rectangle().color(0xFF8B8B8B).asWidget().pos(MARGIN, 106).size(width - 2 * MARGIN, 1));
        // Bottom up, as each opened list goes down over the selectors below it
        panel.child(gridSelector().pos(MARGIN, 60));
        panel.child(placeModeSelector().pos(MARGIN, 10));
    }

    private Flow gridSelector() {
        Flow flow = new Flow(GuiAxis.X);
        flow.size(180, 20);
        DropDownMenu picker = new DropDownMenu();
        picker.marginLeft(MARGIN).marginRight(MARGIN).size(40, 20).background(GuiTextures.BUTTON_CLEAN);
        TextButtonWidget left = new TextButtonWidget();
        TextButtonWidget right = new TextButtonWidget();
        left.size(20, 20).text("<<").background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY);
        right.size(20, 20).text(">>").background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY);
        flow.child(IKey.str("Grid:").asWidget().marginLeft(MARGIN).width(40));
        flow.child(left);
        flow.child(picker);
        flow.child(right);

        for (String size : GRID_SIZES) {
            int align = 16 / Integer.parseInt(size);
            picker.addChoice(menu -> grid.setIntValue(align), size);
        }
        left.onMouseReleased(button -> selectGrid(picker, picker.getSelectedIndex() - 1));
        right.onMouseReleased(button -> selectGrid(picker, picker.getSelectedIndex() + 1));

        String selected = String.valueOf(16 / PlacementSetting.getGrid(player));
        for (int i = 0; i < GRID_SIZES.length; i++) {
            if (GRID_SIZES[i].equals(selected)) picker.setSelectedIndex(i);
        }
        return flow;
    }

    private boolean selectGrid(DropDownMenu picker, int index) {
        index = Math.floorMod(index, GRID_SIZES.length);
        grid.setIntValue(16 / Integer.parseInt(GRID_SIZES[index]));
        picker.setSelectedIndex(index);
        return true;
    }

    private Flow placeModeSelector() {
        Flow flow = new Flow(GuiAxis.X);
        flow.size(120, 40);
        DropDownMenu picker = new DropDownMenu();
        picker.size(120, 20).background(GuiTextures.BUTTON_CLEAN);
        IKey info = new DynamicKey(() -> IKey.str(PlacementSetting.getPlaceMode(player).getInfo()));

        for (LittleTilePlaceMode mode : LittleTilePlaceMode.values()) {
            int id = mode.ordinal();
            picker.addChoice(menu -> placeMode.setIntValue(id), mode.getName());
        }
        picker.setSelectedIndex(PlacementSetting.getPlaceMode(player).ordinal());

        flow.child(picker);
        flow.child(info.asWidget().marginLeft(MARGIN));
        return flow;
    }
}
