package com.creativemd.creativecore.common.gui.controls;

public class GuiSteppedSlider extends GuiAnalogeSlider {

    public GuiSteppedSlider(String name, int x, int y, int width, int height, int rotation, int min, int max,
            int value) {
        super(name, x, y, width, height, rotation, value, min, max);
    }

    public GuiSteppedSlider(String name, int x, int y, int width, int height, int min, int max, int value) {
        this(name, x, y, width, height, 0, min, max, value);
    }

    @Override
    public String getTextByValue() {
        return ((int) value) + "";
    }

    @Override
    public void mouseMove(int posX, int posY, int button) {
        super.mouseMove(posX, posY, button);
        value = (int) value;
    }

    public void setValue(float value) {
        super.setValue((int) value);
    }
}
