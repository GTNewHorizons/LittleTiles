package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.*;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemLittleChisel extends Item implements ILittleTile {

    public ItemLittleChisel() {
        setCreativeTab(CreativeTabs.tabTools);
        hasSubtypes = true;
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTChisel";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advanced) {
        LittleToolHandler handler = new LittleToolHandler(stack);
        info.add("Block: " + handler.getStack().getDisplayName());
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        return Item.getItemFromBlock(LittleTiles.blockTile)
                .onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public void rotateLittlePreview(ItemStack stack, ForgeDirection direction) {

    }

    @Override
    public void flipLittlePreview(ItemStack stack, ForgeDirection direction) {

    }

    @Override
    public LittleStructure getLittleStructure(ItemStack stack) {
        return null;
    }

    @Override
    public ArrayList<LittleTilePreview> getLittlePreview(ItemStack stack) {
        ArrayList<LittleTilePreview> ret = new ArrayList<>();

        LittleToolHandler handler = new LittleToolHandler(stack);
        Block block = handler.getBlock();
        int meta = handler.getMeta();

        int color = ColorUtils.WHITE;
        int sizeX = 1;
        int sizeY = 1;
        int sizeZ = 1;

        LittleTileSize size;
        NBTTagCompound nbt = new NBTTagCompound();

        if (PreviewRenderer.firstHit == null) {
            size = new LittleTileSize(sizeX, sizeY, sizeZ);
        } else {
            MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;
            LittleTileBlockPos pos = null;
            if (moving != null) {
                pos = LittleTileBlockPos.fromMovingObjectPosition(moving);
            }
            if (PreviewRenderer.markedHit != null) {
                pos = PreviewRenderer.markedHit;
            }
            if (pos == null) {
                return null;
            }
            LittleTileBlockPos.Subtraction subtraction = pos.subtract(PreviewRenderer.firstHit);
            int sx = Math.max(Math.abs(subtraction.x) + 1, sizeX);
            int sy = Math.max(Math.abs(subtraction.y) + 1, sizeY);
            int sz = Math.max(Math.abs(subtraction.z) + 1, sizeZ);
            LittleTileBlockPos.Comparison comparison = PreviewRenderer.firstHit.compareTo(pos);
            size = new LittleTileSize(sx, sy, sz);
            nbt.setBoolean("fromChiselPosX", !comparison.biggerOrEqualX);
            nbt.setBoolean("fromChiselPosY", !comparison.biggerOrEqualY);
            nbt.setBoolean("fromChiselPosZ", !comparison.biggerOrEqualZ);
        }

        LittleTile tile;
        if (color != ColorUtils.WHITE) tile = new LittleTileBlockColored(block, meta, ColorUtils.IntToRGB(color));
        else tile = new LittleTileBlock(block, meta);

        size.writeToNBT("size", nbt);
        tile.saveTile(nbt);
        LittleTilePreview preview = new LittleTilePreview(size, nbt);
        ret.add(preview);
        return ret;
    }
}
