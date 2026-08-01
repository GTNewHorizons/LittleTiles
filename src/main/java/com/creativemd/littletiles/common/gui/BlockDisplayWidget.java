package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.SingleChildWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.creativemd.littletiles.common.utils.BlockStateSyncValue;

public class BlockDisplayWidget extends SingleChildWidget<BlockDisplayWidget> implements Interactable {

    private static final IKey NONE = IKey.str("None");
    private IDrawable arrowClosed;
    private IDrawable arrowOpened;
    private final List<ItemStack> stacks = new ArrayList<>();
    private final DropDownWrapper menu = new DropDownWrapper(stacks);
    public static final int scrollWidth = 9;
    private final BlockStateSyncValue syncBlock;
    private final boolean isClient;

    public static final int WIDGET_MARGIN = 5;

    public BlockDisplayWidget(PanelSyncManager syncManager, BlockStateSyncValue syncBlock) {
        menu.setEnabled(false);
        menu.background(GuiTextures.BUTTON_CLEAN);
        child(menu);
        setArrows(GuiTextures.ARROW_UP, GuiTextures.ARROW_DOWN);
        this.syncBlock = syncBlock;
        isClient = syncManager.isClient();
    }

    public int getSelectedIndex() {
        return menu.getCurrentIndex();
    }

    public BlockDisplayWidget setSelectedIndex(int index) {
        menu.setCurrentIndex(index);
        return getThis();
    }

    public BlockDisplayWidget setArrows(IDrawable arrowClosed, IDrawable arrowOpened) {
        this.arrowClosed = arrowClosed;
        this.arrowOpened = arrowOpened;
        return getThis();
    }

    public BlockDisplayWidget addChoice(BlockDisplayWidget.ItemSelected onSelect, ItemStack stack) {
        ButtonWidget<?> button = new ButtonWidget<>();
        ItemDisplayWidget itemDisplay = new ItemDisplayWidget();
        itemDisplay.item(stack);
        itemDisplay.addTooltipElement(stack.getDisplayName());
        button.child(itemDisplay);

        int index = menu.count;
        menu.addItem(button);
        stacks.add(stack);

        button.pos((index % scrollWidth) * 18, (index / scrollWidth) * 18);

        button.onMouseReleased(m -> {
            menu.setOpened(false);
            menu.setCurrentIndex(index);
            onSelect.selected(this);
            return true;
        });
        return getThis();
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (!menu.isOpen()) {
            menu.setOpened(true);
            menu.setEnabled(true);
            return Result.SUCCESS;
        }
        menu.setOpened(false);
        menu.setEnabled(false);
        return Result.SUCCESS;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry widgetTheme) {
        super.draw(context, widgetTheme);
        Area area = getArea();
        WidgetTheme theme = getWidgetTheme(context.getTheme()).getTheme();
        int smallerSide = Math.min(area.width, area.height);
        IWidget selectedItem = menu.getSelectedItem();
        if (selectedItem != null) {
            IWidget child = selectedItem.getChildren().get(0);
            boolean oldEnabled = selectedItem.isEnabled();
            selectedItem.setEnabled(true);
            child.drawBackground(context, widgetTheme);
            child.draw(context, widgetTheme);
            child.drawForeground(context);
            ItemStack stack = stacks.get(getSelectedIndex());
            IKey name = IKey.str(stack.getDisplayName());
            name.draw(context, 25, 0, 0, area.height, theme);
            selectedItem.setEnabled(oldEnabled);
        } else {
            NONE.draw(context, 0, 0, area.width, area.height, theme);
        }

        int arrowSize = smallerSide / 2;
        if (menu.isOpen()) {
            arrowOpened.draw(context, area.width - arrowSize - 5, arrowSize / 2, arrowSize, arrowSize, theme);
        } else {
            arrowClosed.draw(context, area.width - arrowSize - 5, arrowSize / 2, arrowSize, arrowSize, theme);
        }
    }

    @Override
    public BlockDisplayWidget background(IDrawable... background) {
        menu.background(background);
        return super.background(background);
    }

    public void setSelectedStack(ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (ItemStack.areItemStacksEqual(stacks.get(i), stack)) {
                setSelectedIndex(i);
            }
        }
    }

    public void addBlock(Block block, int meta) {
        ItemStack stack = new ItemStack(block, 1, meta);
        addChoice((x) -> syncBlock.setValue(block, meta), stack);
    }

    public void addAllBlocks(Predicate<Block> filter) {
        // getSubBlocks is client only
        if (!isClient) {
            return;
        }
        for (Object blocko : Block.blockRegistry) {
            Block block = (Block) blocko;
            if (filter.test(block)) {
                Item item = new ItemStack(block).getItem();
                if (item == null) {
                    continue;
                }
                List<ItemStack> list = new ArrayList<>();
                block.getSubBlocks(item, block.getCreativeTabToDisplayOn(), list);

                for (ItemStack stack : list) {
                    final int meta = stack.getItemDamage();
                    addBlock(block, meta);
                }
            }
        }
    }

    private static class DropDownWrapper extends ScrollWidget<BlockDisplayWidget.DropDownWrapper> {

        private static final int SEARCH_ROW_HEIGHT = 20;
        private static final int SEARCH_LABEL_WIDTH = 52;
        private static final int SCROLL_ROWS = 4;
        private static final int DROPDOWN_HEIGHT = 145;

        private final List<ButtonWidget<?>> children = new ArrayList<>();
        private boolean open;
        private int count = 0;
        private int currentIndex = -1;

        private final ScrollWidget<?> scroll = new ScrollWidget<>(new VerticalScrollData());
        private final TextFieldWidget text_search = new TextFieldWidget();
        private final TextWidget<?> label_search = IKey.lang("key.littletiles.search").asWidget();
        private final ParentWidget<?> panel = new ParentWidget<>();
        private final Flow flow = new Flow(GuiAxis.X);
        private final List<ItemStack> stacks;
        private int visibleSize = 0;
        private String lastFilter;

        public DropDownWrapper(List<ItemStack> stacks) {
            this.stacks = stacks;

            flow.pos(WIDGET_MARGIN, WIDGET_MARGIN);
            flow.size(1, SEARCH_ROW_HEIGHT);

            label_search.width(SEARCH_LABEL_WIDTH);
            text_search.marginLeft(WIDGET_MARGIN).marginRight(WIDGET_MARGIN);

            flow.addChild(label_search, 0);
            flow.addChild(text_search, 1);
            panel.addChild(flow, 0);
            panel.addChild(scroll, 1);

            scroll.pos(WIDGET_MARGIN, WIDGET_MARGIN * 2 + SEARCH_ROW_HEIGHT);
            scroll.size(1, SCROLL_ROWS * 18);

            text_search.onUpdateListener(x -> updateFilter());
        }

        public void updateFilter() {
            visibleSize = 0;
            String str_search = text_search.getText().toLowerCase();
            if (lastFilter != null && !lastFilter.equals(str_search)) {
                scroll.getScrollArea().getScrollY().scrollTo(scroll.getScrollArea(), 0);
            }
            lastFilter = str_search;
            for (int i = 0; i < children.size(); i++) {
                ButtonWidget<?> child = children.get(i);
                ItemStack stack = stacks.get(i);
                String str_stack = stack.getDisplayName().toLowerCase();
                boolean found = str_stack.contains(str_search);
                child.setEnabled(found);
                if (found) {
                    child.pos((visibleSize % scrollWidth) * 18, (visibleSize / scrollWidth) * 18);
                    visibleSize++;
                }
            }
        }

        @Override
        public void onUpdate() {
            if (!open) {
                setEnabled(false);
            }
        }

        public void setOpened(boolean open) {
            this.open = open;
            text_search.value(new StringValue(""));
            rebuild();
        }

        public boolean isOpen() {
            return open;
        }

        public void addItem(ButtonWidget<?> widget) {
            children.add(widget);
            scroll.addChild(widget, count);
            count++;
            visibleSize++;
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        @Override
        public List<IWidget> getChildren() {
            List<IWidget> ret = new ArrayList<>();
            ret.add(panel);
            return ret;
        }

        public IWidget getSelectedItem() {
            if (currentIndex < 0 || currentIndex >= count) {
                return null;
            }
            return children.get(currentIndex);
        }

        @Override
        public BlockDisplayWidget.DropDownWrapper background(IDrawable... background) {
            panel.background(background);
            return super.background();
        }

        @Override
        public void onResized() {
            super.onResized();
            if (!isValid()) return;

            int width = Math.max(getParentArea().width, scrollWidth * 18 + 2 * WIDGET_MARGIN);
            int minContentHeight = WIDGET_MARGIN * 3 + SEARCH_ROW_HEIGHT + SCROLL_ROWS * 18;
            int dropdownHeight = Math.max(DROPDOWN_HEIGHT, minContentHeight);
            int scrollHeight = dropdownHeight - (WIDGET_MARGIN * 3 + SEARCH_ROW_HEIGHT);

            flow.pos(WIDGET_MARGIN, WIDGET_MARGIN);
            flow.size(Math.max(1, width - 2 * WIDGET_MARGIN), SEARCH_ROW_HEIGHT);
            text_search.width(Math.max(1, width - 2 * WIDGET_MARGIN - SEARCH_LABEL_WIDTH - WIDGET_MARGIN));

            scroll.pos(WIDGET_MARGIN, WIDGET_MARGIN * 2 + SEARCH_ROW_HEIGHT);
            scroll.size(Math.max(1, width - 2 * WIDGET_MARGIN), scrollHeight);
            scroll.getScrollArea().getScrollY().setScrollSize((visibleSize + scrollWidth - 1) / scrollWidth * 18);

            panel.size(width, dropdownHeight);
            size(width, dropdownHeight);
            pos(0, getParentArea().height);

            List<IWidget> children = getChildren();
            for (IWidget child : children) {
                child.setEnabled(open);
            }
        }

        private void rebuild() {
            WidgetTree.resize(this);
        }
    }

    @FunctionalInterface
    public interface ItemSelected {

        void selected(BlockDisplayWidget menu);
    }
}
