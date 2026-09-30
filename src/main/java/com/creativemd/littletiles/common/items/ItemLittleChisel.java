package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.text.DynamicKey;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.DropDownMenu;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.LittleDeformedBoxHelper;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.gui.BlockDisplayWidget;
import com.creativemd.littletiles.common.gui.ShapeSelectorWidget;
import com.creativemd.littletiles.common.gui.TextButtonWidget;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.BlockStateSyncValue;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockColored;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemLittleChisel extends Item implements ILittleTile, IGuiHolder<PlayerInventoryGuiData> {

    private static final int WIDGET_WIDTH = 180;
    private static final int WIDGET_MARGIN = 5;

    public ItemLittleChisel() {
        setCreativeTab(CreativeTabs.tabTools);
        hasSubtypes = true;
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTChisel";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advanced) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        info.add("Block: " + handler.getStack().getDisplayName());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return Item.getItemFromBlock(LittleTiles.blockTile)
                .onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public void rotateLittlePreview(ItemStack stack, ForgeDirection direction) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        handler.handleRotation(direction, null);
    }

    @Override
    public void flipLittlePreview(ItemStack stack, ForgeDirection direction) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        handler.handleFlip(direction, null);
    }

    @Override
    public LittleStructure getLittleStructure(ItemStack stack) {
        return null;
    }

    @Override
    public ArrayList<LittleTilePreview> getLittlePreview(ItemStack stack) {
        ArrayList<LittleTilePreview> ret = new ArrayList<>();

        LittleToolHandler handler = new LittleToolHandler(stack);
        Block block = handler.getBlock();
        int meta = handler.getMeta();

        int color = ColorUtils.WHITE;
        int sizeX = handler.getGrid();
        int sizeY = handler.getGrid();
        int sizeZ = handler.getGrid();

        LittleTileSize size;
        NBTTagCompound nbt = new NBTTagCompound();

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            size = new LittleTileSize(sizeX, sizeY, sizeZ);
        } else {
            int align = handler.getGrid();
            if (handler.isDeformedBoxShape() && LittleDeformedBoxHelper.isEditing()) {
                // The box is fully described by its corners at this point, so unlike every other shape it needs no
                // hit to preview - it stays on screen even while the player looks at nothing.
                size = buildDeformedBoxPreview(nbt);
            } else {
                LittleTileBlockPos end = PreviewRenderer.markedHit;
                if (end == null) {
                    MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;
                    if (moving == null) {
                        return null;
                    }
                    end = LittleTileBlockPos.fromMovingObjectPosition(moving, align);
                }
                LittleTileBlockPos start = PreviewRenderer.firstHit != null ? PreviewRenderer.firstHit : end;

                LittleTileBlockPos.Subtraction subtraction = end.subtract(stack, start);
                LittleTileBlockPos.Comparison comparison = start.compareTo(end);
                size = new LittleTileSize(subtraction.x, subtraction.y, subtraction.z);
                nbt.setBoolean("fromChiselPosX", !comparison.biggerOrEqualX);
                nbt.setBoolean("fromChiselPosY", !comparison.biggerOrEqualY);
                nbt.setBoolean("fromChiselPosZ", !comparison.biggerOrEqualZ);
                nbt.setInteger("fromChiselAlign", align);

                // The shape selected in the gui only becomes a concrete cutout once both hits are known,
                // so it is written here, where the preview (and everything derived from it) picks it up.
                LittleTileCutoutInfo cutoutInfo = LittleTileCutoutInfo.fromItemStack(stack, start, end);
                if (cutoutInfo != null) {
                    cutoutInfo.writeToNBT(nbt);
                }
            }
        }

        LittleTile tile;
        if (color != ColorUtils.WHITE) tile = new LittleTileBlockColored(block, meta, ColorUtils.IntToRGB(color));
        else tile = new LittleTileBlock(block, meta);

        size.writeToNBT("size", nbt);
        tile.saveTileForItem(nbt);
        LittleTilePreview preview = new LittleTilePreview(size, nbt);
        ret.add(preview);
        return ret;
    }

    /**
     * Builds the preview of the deformed box from the corners as they currently stand, so the player sees the box
     * update live while it is being shaped. Until the initial two clicks have closed a box there is nothing to deform
     * yet and the regular chisel box preview is used instead.
     */
    @SideOnly(Side.CLIENT)
    private LittleTileSize buildDeformedBoxPreview(NBTTagCompound nbt) {
        LittleDeformedBoxHelper.currentCutout().writeToNBT(nbt);

        // The tile is placed at LittleDeformedBoxHelper.placementAnchor(), which already accounts for the corner
        // bounding box's min corner, so no further backward shift is needed here.
        nbt.setBoolean("fromChiselPosX", false);
        nbt.setBoolean("fromChiselPosY", false);
        nbt.setBoolean("fromChiselPosZ", false);
        nbt.setInteger("fromChiselAlign", 1);

        return LittleDeformedBoxHelper.currentBox().getSize();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(LittleTiles.modid, mainPanel);
    }

    private void selectBlock(PlayerInventoryGuiData data, Block block, int meta) {
        if (block == Blocks.air) return;
        ItemStack stack = data.getUsedItemStack();
        new LittleToolHandler(stack).setBlock(block, meta);
    }

    private void selectGrid(PlayerInventoryGuiData data, int grid) {
        if (grid == 0) {
            return;
        }
        ItemStack stack = data.getUsedItemStack();
        new LittleToolHandler(stack).setGrid(grid);
    }

    private void selectShape(PlayerInventoryGuiData data, int shape) {
        if (shape == -1) {
            return;
        }
        ItemStack stack = data.getUsedItemStack();
        new LittleToolHandler(stack).setShape(shape);
    }

    private void selectPlaceMode(PlayerInventoryGuiData data, int placeMode) {
        if (placeMode == -1) {
            return;
        }
        ItemStack stack = data.getUsedItemStack();
        new LittleToolHandler(stack).setPlaceMode(placeMode);
    }

    private BlockDisplayWidget addBlockDisplay(PanelSyncManager syncManager, BlockStateSyncValue syncBlock,
            LittleToolHandler handler, int y) {
        BlockDisplayWidget blockDisplay = new BlockDisplayWidget(syncManager, syncBlock);
        blockDisplay.size(WIDGET_WIDTH, 20).pos(WIDGET_MARGIN, y).marginLeft(WIDGET_MARGIN);

        blockDisplay.addAllBlocks(BlockValidator::isBlockValid);

        blockDisplay.setSelectedStack(handler.getStack());

        return blockDisplay;
    }

    private Flow addGridSelector(IntSyncValue syncGrid, LittleToolHandler handler, int y) {
        Flow flow = new Flow(GuiAxis.X);
        flow.pos(WIDGET_MARGIN, y).size(WIDGET_WIDTH, 20);
        TextWidget<?> labelGrid = IKey.str("Grid:").asWidget().marginLeft(WIDGET_MARGIN).width(40);
        DropDownMenu gridPicker = new DropDownMenu();
        gridPicker.marginLeft(WIDGET_MARGIN).marginRight(WIDGET_MARGIN).size(40, 20);
        gridPicker.background(GuiTextures.BUTTON_CLEAN);
        TextButtonWidget buttonLeft = new TextButtonWidget();
        TextButtonWidget buttonRight = new TextButtonWidget();
        buttonLeft.size(20, 20).text("<<").background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY);
        buttonRight.size(20, 20).text(">>").background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY);
        flow.child(labelGrid);
        flow.child(buttonLeft);
        flow.child(gridPicker);
        flow.child(buttonRight);

        String[] gridSizes = { "1", "2", "4", "8", "16" };
        for (String size : gridSizes) {
            final int realSize = 16 / Integer.parseInt(size);
            gridPicker.addChoice(x -> syncGrid.setIntValue(realSize), size);
        }

        buttonLeft.onMouseReleased(x -> {
            int index = gridPicker.getSelectedIndex() - 1;
            if (index < 0) index = gridSizes.length - 1;
            final int realSize = 16 / Integer.parseInt(gridSizes[index]);
            syncGrid.setIntValue(realSize);
            gridPicker.setSelectedIndex(index);
            return true;
        });

        buttonRight.onMouseReleased(x -> {
            int index = gridPicker.getSelectedIndex() + 1;
            if (index >= gridSizes.length) index = 0;
            final int realSize = 16 / Integer.parseInt(gridSizes[index]);
            syncGrid.setIntValue(realSize);
            gridPicker.setSelectedIndex(index);
            return true;
        });

        String inverseGrid = String.valueOf(16 / handler.getGrid());
        for (int i = 0; i < gridSizes.length; i++) {
            if (gridSizes[i].equals(inverseGrid)) {
                gridPicker.setSelectedIndex(i);
            }
        }

        return flow;
    }

    private ShapeSelectorWidget addShapeSelector(IntSyncValue sync, LittleToolHandler handler, int y) {
        ShapeSelectorWidget shapePicker = new ShapeSelectorWidget();
        shapePicker.pos(WIDGET_MARGIN, y).size(WIDGET_WIDTH, 20).marginLeft(WIDGET_MARGIN);
        shapePicker.background(GuiTextures.BUTTON_CLEAN);

        for (LittleTileShapeMode mode : LittleTileShapeMode.values()) {
            int id = mode.ordinal();
            shapePicker.addChoice(x -> sync.setIntValue(id), mode);
        }

        int shape = handler.getShape().ordinal();
        shapePicker.setSelectedIndex(shape);

        return shapePicker;
    }

    private Flow addPlaceModeSelector(IntSyncValue sync, LittleToolHandler handler, int y) {
        Flow flow = new Flow(GuiAxis.X);
        flow.pos(WIDGET_MARGIN, y).size(120, 40);

        DropDownMenu placeModePicker = new DropDownMenu();
        placeModePicker.size(120, 20);
        placeModePicker.background(GuiTextures.BUTTON_CLEAN);

        IKey placeModeInfoKey = new DynamicKey(() -> IKey.str(handler.getPlaceMode().getInfo()));

        for (LittleTilePlaceMode mode : LittleTilePlaceMode.values()) {
            int id = mode.ordinal();
            placeModePicker.addChoice(x -> { sync.setIntValue(id); }, mode.getName());
        }

        int placeMode = handler.getPlaceMode().ordinal();
        placeModePicker.setSelectedIndex(placeMode);

        flow.child(placeModePicker);
        flow.child(placeModeInfoKey.asWidget().marginLeft(WIDGET_MARGIN));

        return flow;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        BlockStateSyncValue syncBlock = new BlockStateSyncValue((block, meta) -> selectBlock(data, block, meta));
        IntSyncValue syncGrid = SyncHandlers.intNumber(() -> 0, grid -> selectGrid(data, grid)).allowC2S();
        IntSyncValue syncShape = SyncHandlers.intNumber(() -> -1, shape -> selectShape(data, shape)).allowC2S();
        IntSyncValue syncPlaceMode = SyncHandlers.intNumber(() -> -1, placeMode -> selectPlaceMode(data, placeMode))
                .allowC2S();
        syncBlock.register(syncManager, "lt_chisel_block");
        syncManager.syncValue("lt_chisel_grid", syncGrid);
        syncManager.syncValue("lt_chisel_shape", syncShape);
        syncManager.syncValue("lt_chisel_place_mode", syncPlaceMode);

        LittleToolHandler handler = new LittleToolHandler(data.getUsedItemStack());

        ModularPanel panel = ModularPanel.defaultPanel("blocks");

        panel.size(250, 300);
        panel.child(addBlockDisplay(syncManager, syncBlock, handler, 130));
        panel.child(addShapeSelector(syncShape, handler, 95));
        panel.child(addGridSelector(syncGrid, handler, 60));
        panel.child(addPlaceModeSelector(syncPlaceMode, handler, 10));
        return panel;
    }
}
