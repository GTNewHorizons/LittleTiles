package com.creativemd.littletiles.common.gui;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.common.structure.LittleDoor;

/**
 * Door settings: the rotation axis, its position and the normal the door opens towards.
 */
public class DoorConfigWidget extends StructureConfigWidget {

    private static final int VIEWER_SIZE = 120;
    private static final int BUTTON_X = VIEWER_SIZE + 5;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ARROW_SIZE = 20;

    public final TileViewerWidget viewer;

    public DoorConfigWidget(ItemStack stack, LittleDoor door) {
        viewer = new TileViewerWidget(stack);
        if (door != null) {
            viewer.axisDirection = door.axis;
            viewer.axisX = door.axisVec.x;
            viewer.axisY = door.axisVec.y;
            viewer.axisZ = door.axisVec.z;
            viewer.normalAxis = Axis.getAxis(door.normalDirection);
        }
        viewer.visibleAxis = true;
        viewer.updateViewDirection();
        child(viewer.pos(0, 0).size(VIEWER_SIZE, VIEWER_SIZE));

        child(button("Swap axis", 0, changing(this::swapAxis)));
        child(button("Swap normal", 1, changing(viewer::changeNormalAxis)));
        child(button("Reset view", 2, viewer::resetView));
        child(button("Flip view", 3, () -> viewer.viewDirection = viewer.viewDirection.getOpposite()));

        int arrowX = BUTTON_X + (BUTTON_WIDTH - ARROW_SIZE) / 2;
        int arrowY = 4 * (BUTTON_HEIGHT + 2) + 4;
        child(arrow("^", arrowX, arrowY, changing(this::moveUp)));
        child(arrow("<", arrowX - ARROW_SIZE, arrowY + ARROW_SIZE, changing(this::moveLeft)));
        child(arrow("v", arrowX, arrowY + ARROW_SIZE, changing(this::moveDown)));
        child(arrow(">", arrowX + ARROW_SIZE, arrowY + ARROW_SIZE, changing(this::moveRight)));
    }

    /** The action, reporting the change of the door after it. The view buttons only move the view. */
    private Runnable changing(Runnable action) {
        return () -> {
            action.run();
            changed();
        };
    }

    private static ButtonWidget<?> button(String text, int row, Runnable action) {
        return new ButtonWidget<>().overlay(IKey.str(text)).pos(BUTTON_X, row * (BUTTON_HEIGHT + 2))
                .size(BUTTON_WIDTH, BUTTON_HEIGHT).onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    private static ButtonWidget<?> arrow(String text, int x, int y, Runnable action) {
        return new ButtonWidget<>().overlay(IKey.str(text)).pos(x, y).size(ARROW_SIZE, ARROW_SIZE)
                .addTooltipElement("Move axis").onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    private void swapAxis() {
        switch (viewer.axisDirection) {
            case AxisX:
                viewer.axisDirection = Axis.AxisY;
                break;
            case AxisY:
                viewer.axisDirection = Axis.AxisZ;
                break;
            case AxisZ:
                viewer.axisDirection = Axis.AxisX;
                break;
            default:
                break;
        }
        viewer.updateViewDirection();
        viewer.updateNormalAxis();
    }

    private void moveLeft() {
        if (viewer.axisDirection == Axis.AxisX) viewer.axisZ++;
        else viewer.axisX--;
    }

    private void moveRight() {
        if (viewer.axisDirection == Axis.AxisX) viewer.axisZ--;
        else viewer.axisX++;
    }

    private void moveUp() {
        if (viewer.axisDirection == Axis.AxisY) viewer.axisZ--;
        else viewer.axisY++;
    }

    private void moveDown() {
        if (viewer.axisDirection == Axis.AxisY) viewer.axisZ++;
        else viewer.axisY--;
    }
}
