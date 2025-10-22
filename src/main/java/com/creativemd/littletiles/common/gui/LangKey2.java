package com.creativemd.littletiles.common.gui;

import com.cleanroommc.modularui.drawable.text.LangKey;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LangKey2 extends LangKey {

    public LangKey2(String key) {
        super(key);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        renderer.setColor(widgetTheme.getTextColor());
        renderer.setShadow(widgetTheme.getTextShadow());
        renderer.setAlignment(Alignment.CenterLeft, width, height);
        renderer.setScale(1f);
        renderer.setPos(x + 5, y);
        renderer.draw(getFormatted());
    }
}
