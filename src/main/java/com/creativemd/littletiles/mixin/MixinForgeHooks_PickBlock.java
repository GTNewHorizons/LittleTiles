package com.creativemd.littletiles.mixin;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.packet.LittleItemUpdatePacket;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(ForgeHooks.class)
public class MixinForgeHooks_PickBlock {

    @Unique
    private static void littletiles$setBlock(ItemStack stack, Block block, int meta) {
        new LittleToolHandler(stack).setBlock(block, meta);
        Minecraft mc = Minecraft.getMinecraft();

        PacketHandler.sendPacketToServer(new LittleItemUpdatePacket(stack.getTagCompound()));
    }

    @WrapOperation(
            method = "onPickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getPickBlock(Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"),
            remap = false)
    private static ItemStack pickBlock(Block instance, MovingObjectPosition target, World world, int x, int y, int z,
            EntityPlayer player, Operation<ItemStack> original) {
        ItemStack stack = player.getHeldItem();
        if (stack != null && stack.getItem() == LittleTiles.chisel) {
            if (BlockValidator.isBlockValid(instance)) {
                int meta = instance.getDamageValue(world, x, y, z);
                littletiles$setBlock(stack, instance, meta);
                return null;
            }
            if (instance == LittleTiles.blockTile) {
                if (BlockTile.loadTileEntity(world, x, y, z) && BlockTile.tempEntity.updateLoadedTile(player)) {
                    LittleTileBlock tileBlock = (LittleTileBlock) BlockTile.tempEntity.loadedTile;
                    try {
                        littletiles$setBlock(stack, tileBlock.block, tileBlock.meta);
                        return null;
                    } catch (Exception ignored) {

                    }
                }
            }
        }
        return original.call(instance, target, world, x, y, z, player);
    }
}
