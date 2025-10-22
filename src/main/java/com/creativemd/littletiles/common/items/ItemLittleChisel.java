package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.BlockValidator;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.gui.BlockDisplayWidget;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.*;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemLittleChisel extends Item implements ILittleTile, IGuiHolder<PlayerInventoryGuiData> {

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

    private List<Block> getAllBlocks() {
        List<Block> ret = new ArrayList<>();
        for (Object block : Block.blockRegistry) {
            if (BlockValidator.isBlockValid((Block) block)) {
                ret.add((Block) block);
            }
        }
        return ret;
    }

    private void selectBlock(PlayerInventoryGuiData data, Block block, int meta) {
        if (block == Blocks.air) return;
        ItemStack stack = data.getUsedItemStack();
        new LittleToolHandler(stack).setBlock(block, meta);
    }

    private BlockDisplayWidget addBlockDisplay(BlockStateSyncValue syncBlock, LittleToolHandler handler, int y) {
        BlockDisplayWidget blockDisplay = new BlockDisplayWidget();
        blockDisplay.size(150, 20).pos(5, y).marginLeft(5);

        List<Block> blocks = getAllBlocks();
        for (final Block block : blocks) {
            Item item = new ItemStack(block).getItem();
            if (item == null) {
                continue;
            }
            List<ItemStack> list = new ArrayList<>();
            block.getSubBlocks(item, block.getCreativeTabToDisplayOn(), list);

            for (ItemStack stack : list) {
                final int meta = stack.getItemDamage();
                blockDisplay.addChoice((x) -> syncBlock.setValue(block, meta), stack);
            }
        }

        blockDisplay.setSelectedStack(handler.getStack());

        return blockDisplay;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        BlockStateSyncValue syncBlock = new BlockStateSyncValue((block, meta) -> selectBlock(data, block, meta));
        syncBlock.register(syncManager, "lt_chisel_block");

        LittleToolHandler handler = new LittleToolHandler(data.getUsedItemStack());

        ModularPanel panel = ModularPanel.defaultPanel("blocks");
        panel.size(200, 300);
        panel.child(addBlockDisplay(syncBlock, handler, 75));
        return panel;
    }
}
