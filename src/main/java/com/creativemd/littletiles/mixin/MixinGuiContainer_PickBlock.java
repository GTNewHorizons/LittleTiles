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
import com.creativemd.littletiles.client.ChiselPickGuiHelper;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.items.ItemLittleChisel;
import com.creativemd.littletiles.common.packet.LittleCursorItemUpdatePacket;
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

        ItemStack targetStack = ChiselPickGuiHelper.getNEIStackMouseOver((GuiContainer) (Object) this);
        if (targetStack == null) {
            // Fallback to the default slot detection.
            Slot slot = littletiles$invokeGetSlotAtPosition(mouseX, mouseY);
            if (slot != null) {
                targetStack = slot.getStack();
            }
        }
        if (targetStack == null) {
            return;
        }

        Block block = Block.getBlockFromItem(targetStack.getItem());
        if (!BlockValidator.isBlockValid(block)) {
            return;
        }

        // Will need additional handling if we want to support additional meta (like Forestry wood, which is not stored
        // in the ItemStack's damage value)
        new LittleToolHandler(cursorStack).setBlock(block, targetStack.getItemDamage());
        PacketHandler.sendPacketToServer(new LittleCursorItemUpdatePacket(cursorStack.getTagCompound()));

        ci.cancel();
    }

    @Invoker("getSlotAtPosition")
    protected abstract Slot littletiles$invokeGetSlotAtPosition(int x, int y);
}
