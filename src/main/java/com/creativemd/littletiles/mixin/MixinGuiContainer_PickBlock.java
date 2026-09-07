package com.creativemd.littletiles.mixin;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.items.ItemLittleChisel;
import com.creativemd.littletiles.common.packet.LittleCursorBlockUpdatePacket;
import com.creativemd.littletiles.common.utils.LittleToolHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer_PickBlock {

    // Middle Click on an item with the Little Chisel picked up switches to that block. Intercepts the top-level
    // mouseClicked so it works for real inventory slots and, when NEI is present, NEI item panels
    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void littletiles$onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton != 2) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }

        ItemStack cursorStack = player.inventory.getItemStack();
        if (cursorStack == null || !(cursorStack.getItem() instanceof ItemLittleChisel)) {
            return;
        }

        // Sink middle click as soon as chisel is picked up
        ci.cancel();

        ItemStack hoveredStack = null;
        // get the hovered stack from the active container
        try {
            // try regular container
            Slot hoveredSlot = littletiles$invokeGetSlotAtPosition(mouseX, mouseY);

            // get the stack
            if (hoveredSlot != null) {
                hoveredStack = hoveredSlot.getStack();
            }

            // try NEI
            if (hoveredStack == null && LittleTiles.neiCompat != null && !LittleTiles.neiCompat.isNEIHidden()) {
                hoveredStack = LittleTiles.neiCompat.getStackMouseOver((GuiContainer) (Object) this);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (hoveredStack == null) {
            return;
        }

        Block block = Block.getBlockFromItem(hoveredStack.getItem());
        if (!BlockValidator.isBlockValid(block)) {
            return;
        }
        final int meta = hoveredStack.getItem().getMetadata(hoveredStack.getItemDamage());
        new LittleToolHandler(cursorStack).setBlock(block, meta);

        PacketHandler.sendPacketToServer(new LittleCursorBlockUpdatePacket(Block.getIdFromBlock(block), meta));
    }

    @Invoker("getSlotAtPosition")
    protected abstract Slot littletiles$invokeGetSlotAtPosition(int x, int y);
}
