package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.SingleChildWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;

public class ShapeSelectorWidget extends SingleChildWidget<ShapeSelectorWidget> implements Interactable {

    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 8;
    private static final int PREVIEW_SIZE = 14;
    private static final int ROW_CONTENT_GUTTER = 12;
    private static final Vector3i FULL_TILE = new Vector3i(16, 16, 16);
    private static final Vector3i ZERO = new Vector3i();
    private static final Map<LittleTileShapeMode, Mesh3d> PREVIEW_MESHES = new EnumMap<>(LittleTileShapeMode.class);

    private IDrawable arrowClosed;
    private IDrawable arrowOpened;
    private final DropDownWrapper menu = new DropDownWrapper();

    public ShapeSelectorWidget() {
        menu.setEnabled(false);
        menu.background(GuiTextures.BUTTON_CLEAN);
        child(menu);
        setArrows(GuiTextures.ARROW_UP, GuiTextures.ARROW_DOWN);
    }

    public int getSelectedIndex() {
        return menu.getCurrentIndex();
    }

    public ShapeSelectorWidget setSelectedIndex(int index) {
        menu.setCurrentIndex(index);
        return getThis();
    }

    public ShapeSelectorWidget setArrows(IDrawable closed, IDrawable opened) {
        this.arrowClosed = closed;
        this.arrowOpened = opened;
        return getThis();
    }

    public ShapeSelectorWidget addChoice(ShapeSelected onSelect, LittleTileShapeMode shape) {
        ButtonWidget<?> button = new ButtonWidget<>();
        ShapePreviewWidget preview = new ShapePreviewWidget(shape);
        int itemWidth = Math.max(0, (getArea().width <= 0 ? 180 : getArea().width) - ROW_CONTENT_GUTTER);
        button.size(itemWidth, ROW_HEIGHT);
        preview.size(itemWidth, ROW_HEIGHT);

        int index = menu.count;
        menu.addItem(button);

        button.child(preview);
        button.pos(0, index * ROW_HEIGHT);
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
        int smallerSide = Math.min(area.width, area.height);
        IWidget selectedItem = menu.getSelectedItem();
        if (selectedItem != null) {
            IWidget child = selectedItem.getChildren().get(0);
            boolean oldEnabled = selectedItem.isEnabled();
            selectedItem.setEnabled(true);
            child.drawBackground(context, widgetTheme);
            child.draw(context, widgetTheme);
            child.drawForeground(context);
            selectedItem.setEnabled(oldEnabled);
        }

        int arrowSize = smallerSide / 2;
        IDrawable currentArrow = menu.isOpen() ? arrowOpened : arrowClosed;
        currentArrow.draw(
                context,
                area.width - arrowSize - 5,
                arrowSize / 2,
                arrowSize,
                arrowSize,
                getWidgetTheme(context.getTheme()).getTheme());
    }

    @Override
    public ShapeSelectorWidget background(IDrawable... background) {
        menu.background(background);
        return super.background(background);
    }

    private static Mesh3d getPreviewMesh(LittleTileShapeMode shape) {
        Mesh3d mesh = PREVIEW_MESHES.get(shape);
        if (mesh == null) {
            mesh = createPreviewMesh(shape);
            PREVIEW_MESHES.put(shape, mesh);
            mesh.setTextures(Blocks.quartz_block, 0);
        }
        return mesh;
    }

    private static Mesh3d createPreviewMesh(LittleTileShapeMode shape) {
        if (shape == LittleTileShapeMode.BOX) {
            return Mesh3dUtil.createBoxMesh();
        }

        LittleTileCutoutInfo cutoutInfo = new LittleTileCutoutInfo();
        cutoutInfo.type = shape;
        cutoutInfo.size = new Vector3i(FULL_TILE);
        cutoutInfo.pos = ZERO;
        cutoutInfo.orientation = 0;

        if (shape == LittleTileShapeMode.PILLAR) {
            cutoutInfo.thickness = 5;
            cutoutInfo.faceStart = ForgeDirection.SOUTH;
            cutoutInfo.faceEnd = ForgeDirection.UP;
            Mesh3d mesh = Mesh3dUtil
                    .createMesh(cutoutInfo, new Vector3d(1, 1, 1), new Vector3d(), ZERO, ZERO, FULL_TILE, null, 0, 0);
            return mesh;
        }

        Mesh3d mesh = Mesh3dUtil
                .createMesh(cutoutInfo, new Vector3d(1, 1, 1), new Vector3d(), ZERO, ZERO, FULL_TILE, null, 0, 0);
        return mesh;
    }

    private static void drawTexturedPreview(Mesh3d mesh, int x, int y, int size) {
        if (mesh == null) {
            return;
        }

        GL11.glPushMatrix();
        float inventoryScale = size * 0.625F;
        GL11.glTranslatef(x - size * 0.125F, y + size * 0.1875F, 0.0F);
        GL11.glScalef(inventoryScale, inventoryScale, inventoryScale);
        GL11.glColor4d(1, 1, 1, 1);
        mesh.renderIcon();
        GL11.glPopMatrix();
    }

    private static class ShapePreviewWidget extends TextWidget<ShapePreviewWidget> {

        private final LittleTileShapeMode shape;

        public ShapePreviewWidget(LittleTileShapeMode shape) {
            super(IKey.str(shape.getName()));
            this.shape = shape;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry widgetTheme) {
            Area area = getArea();
            WidgetTheme theme = getWidgetTheme(context.getTheme()).getTheme();
            int previewX = 3;
            int previewY = Math.max(0, (area.height - PREVIEW_SIZE) / 2);
            drawTexturedPreview(getPreviewMesh(shape), previewX, previewY, PREVIEW_SIZE);
            getKey().draw(context, PREVIEW_SIZE + 8, 0, 0, area.height, theme);
        }
    }

    private static class DropDownWrapper extends ScrollWidget<ShapeSelectorWidget.DropDownWrapper> {

        private final List<ButtonWidget<?>> children = new ArrayList<>();
        private boolean open;
        private int count = 0;
        private int currentIndex = -1;
        private final ScrollWidget<?> scroll = new ScrollWidget<>(new VerticalScrollData());
        private final ParentWidget<?> panel = new ParentWidget<>();

        public DropDownWrapper() {
            panel.addChild(scroll, 0);
            scroll.pos(4, 4);
        }

        @Override
        public void onUpdate() {
            if (!open) {
                setEnabled(false);
            }
        }

        public void setOpened(boolean open) {
            this.open = open;
            rebuild();
        }

        public boolean isOpen() {
            return open;
        }

        public void addItem(ButtonWidget<?> widget) {
            children.add(widget);
            scroll.addChild(widget, count);
            count++;
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
        public ShapeSelectorWidget.DropDownWrapper background(IDrawable... background) {
            panel.background(background);
            return super.background();
        }

        @Override
        public void onResized() {
            super.onResized();
            if (!isValid()) {
                return;
            }

            int width = getParentArea().width;
            int visibleRows = Math.min(Math.max(count, 1), VISIBLE_ROWS);
            scroll.size(width - 8, visibleRows * ROW_HEIGHT);
            scroll.getScrollArea().getScrollY().setScrollSize(count * ROW_HEIGHT);

            panel.size(width, visibleRows * ROW_HEIGHT + 8);
            size(width, visibleRows * ROW_HEIGHT + 8);
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
    public interface ShapeSelected {

        void selected(ShapeSelectorWidget menu);
    }
}
