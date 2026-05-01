package com.creativemd.littletiles.common.gui;

import java.util.ArrayList;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.gui.SubGui;
import com.creativemd.creativecore.common.gui.controls.GuiButton;
import com.creativemd.creativecore.common.gui.controls.GuiItemListBox;
import com.creativemd.creativecore.common.gui.event.ControlClickEvent;
import com.creativemd.littletiles.common.items.ItemRecipe;
import com.creativemd.littletiles.common.items.ItemTileContainer.BlockEntry;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

public class SubGuiWrench extends SubGui {

    @Override
    public void createControls() {
        controls.add(new GuiButton("Craft", 70, 5, 40));
        controls.add(
                new GuiItemListBox("missing", container.player, 10, 30, 100, 50, new ArrayList<>(), new ArrayList<>()));
    }

    @CustomEventSubscribe
    public void onControlClicked(ControlClickEvent event) {
        if (event.source.is("Craft")) {
            ItemStack stack1 = ((SubContainerWrench) container).basic.getStackInSlot(0);
            ItemStack stack2 = ((SubContainerWrench) container).basic.getStackInSlot(1);

            GuiItemListBox listBox = (GuiItemListBox) getControl("missing");
            listBox.clear();

            if (stack1 != null) {
                if (stack1.getItem() instanceof ItemRecipe) {
                    boolean enough = true;

                    ArrayList<BlockEntry> entries = SubContainerWrench.getContentofStack(stack2);
                    ArrayList<BlockEntry> missing = new ArrayList<>();
                    if (!container.player.capabilities.isCreativeMode) {
                        ArrayList<LittleTilePreview> tiles = ItemRecipe.getPreview(stack1);
                        if (tiles != null) {
                            missing.addAll(SubContainerWrench.getMissing(tiles, entries));
                        }
                    }

                    for (BlockEntry blockEntry : missing) {
                        listBox.add(SubGuiTileContainer.getStringOfValue(blockEntry.value), blockEntry.getItemStack());
                    }

                    if (missing.size() > 0) return;
                }
            }
            sendPacketToServer(0, new NBTTagCompound());
        }
    }

    @Override
    public void drawOverlay(FontRenderer fontRenderer) {

    }

}
