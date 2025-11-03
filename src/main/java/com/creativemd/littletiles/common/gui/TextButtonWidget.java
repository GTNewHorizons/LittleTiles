package com.creativemd.littletiles.common.gui;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

public class TextButtonWidget extends ButtonWidget<TextButtonWidget> {

    private TextWidget currentTextWidget;

    public TextButtonWidget text(String text) {
        currentTextWidget = new TextButtonTextWidget(IKey.str(text));
        currentTextWidget.align(Alignment.Center);
        return child(currentTextWidget);
    }

    @Override
    public void onMouseStartHover() {
        currentTextWidget.style(EnumChatFormatting.WHITE);
    }

    @Override
    public void onMouseEndHover() {
        currentTextWidget.style(EnumChatFormatting.BLACK);
    }

    private static class TextButtonTextWidget extends TextWidget {

        public TextButtonTextWidget(IKey key) {
            super(key);
        }

        @Override
        public void onMouseStartHover() {
            style(EnumChatFormatting.WHITE);
        }

        @Override
        public void onMouseEndHover() {
            style(EnumChatFormatting.BLACK);
        }
    }
}
