package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.WorldUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.packet.LittlePlacePacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.utils.PreviewTile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBlockTiles extends ItemBlock implements ILittleTile, ITilesRenderer {

    public ItemBlockTiles(Block block) {
        super(block);
        hasSubtypes = true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack stack) {
        String result = super.getItemStackDisplayName(stack);
        if (stack.stackTagCompound != null) {
            result += " (x=" + stack.stackTagCompound.getByte("sizex")
                    + ",y="
                    + stack.stackTagCompound.getByte("sizey")
                    + "z="
                    + stack.stackTagCompound.getByte("sizez")
                    + ")";
        }
        return result;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getUnlocalizedName(ItemStack stack) {
        if (stack.stackTagCompound != null) {
            Block block = Block.getBlockFromName(stack.stackTagCompound.getString("block"));
            if (block == null) {
                return "block.LTBlocks.missingblock";
            }
            return block.getUnlocalizedName();
        }
        return super.getUnlocalizedName(stack);
    }

    private boolean needsTwoHits(ItemStack stack) {
        return stack.getItem() == LittleTiles.chisel;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float offsetX, float offsetY, float offsetZ) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) return false;

        MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;

        int align = 1;
        LittleTilePlaceMode placeMode = LittleTilePlaceMode.NORMAL;
        if (stack.getItem() == LittleTiles.chisel) {
            LittleToolHandler handler = new LittleToolHandler(stack);
            align = handler.getGrid();
            placeMode = handler.getPlaceMode();
        }

        LittleTileBlockPos pos = LittleTileBlockPos.fromMovingObjectPosition(moving, align);

        if (PreviewRenderer.markedHit != null) pos = PreviewRenderer.markedHit;

        LittleTileCutoutInfo cutoutInfo = null;

        if (needsTwoHits(stack)) {
            if (PreviewRenderer.firstHit == null) {
                if (PreviewRenderer.markedHit == null) {
                    PreviewRenderer.firstHit = pos;
                    return true;
                }
                cutoutInfo = LittleTileCutoutInfo.fromItemStack(stack, pos, pos);
            } else {
                cutoutInfo = LittleTileCutoutInfo.fromItemStack(stack, PreviewRenderer.firstHit, pos);

                ILittleTile littleTile = (ILittleTile) stack.getItem();

                NBTTagCompound tag = (NBTTagCompound) littleTile.getLittlePreview(stack).get(0).nbt.copy();
                stack = new ItemStack(Item.getItemFromBlock(LittleTiles.blockTile));
                stack.stackTagCompound = tag;
                PreviewRenderer.firstHit = null;
            }
        } else {
            cutoutInfo = LittleTileCutoutInfo.loadFromNBT(stack.stackTagCompound);
        }

        x = pos.getPosX();
        y = pos.getPosY();
        z = pos.getPosZ();

        if (stack.stackSize == 0) {
            return false;
        } else if (!player.canPlayerEdit(x, y, z, side, stack)) {
            return false;
        } else if (y == 255) {
            return false;
        } else {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) PacketHandler.sendPacketToServer(
                    new LittlePlacePacket(stack, pos, PreviewRenderer.markedHit != null, cutoutInfo, placeMode));

            placeBlockAt(player, stack, world, pos, PreviewRenderer.markedHit != null, cutoutInfo, placeMode);

            PreviewRenderer.markedHit = null;

            return true;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconregister) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item stack, CreativeTabs tab, List list) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean func_150936_a(World world, int xin, int yin, int zin, int side, EntityPlayer player,
            ItemStack stack) {

        MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;

        LittleTileBlockPos pos = LittleTileBlockPos.fromMovingObjectPosition(moving, 1);
        if (PreviewRenderer.markedHit != null) pos = PreviewRenderer.markedHit;

        final int x = pos.getPosX();
        final int y = pos.getPosY();
        final int z = pos.getPosZ();
        final Block block = world.getBlock(x, y, z);
        return block.isReplaceable(world, x, y, z) || PlacementHelper.canBePlacedInsideBlock(player, x, y, z);
    }

    public static boolean placeTiles(World world, EntityPlayer player, ArrayList<PreviewTile> previews,
            LittleStructure structure, int x, int y, int z, ItemStack stack, ArrayList<LittleTile> unplaceableTiles,
            LittleTileCutoutInfo cutoutInfo, LittleTilePlaceMode placeMode) {
        LittleTilePlacementPlan plan = new LittleTilePlacementPlan();
        plan.fillPlan(world, x, y, z, previews, structure, placeMode, cutoutInfo);
        if (!plan.canApplyPlan()) {
            return false;
        }

        return plan.applyPlan(world, player, stack, structure, unplaceableTiles);
    }

    public boolean placeBlockAt(EntityPlayer player, ItemStack stack, World world, LittleTileBlockPos pos,
            boolean customPlacement, LittleTileCutoutInfo cutoutInfo, LittleTilePlaceMode placeMode) {
        ArrayList<PreviewTile> previews = PlacementHelper.getPreviewTiles(player, stack, pos, customPlacement);

        LittleStructure structure = null;
        if (stack.getItem() instanceof ILittleTile) {
            structure = ((ILittleTile) stack.getItem()).getLittleStructure(stack);
        } else if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile) {
            structure = ((ILittleTile) Block.getBlockFromItem(stack.getItem())).getLittleStructure(stack);
        }

        if (structure != null) {
            structure.dropStack = stack.copy();
            structure.setTiles(new ArrayList<>());
        }

        int x = pos.getPosX();
        int y = pos.getPosY();
        int z = pos.getPosZ();

        ArrayList<LittleTile> unplaceableTiles = new ArrayList<>();
        if (placeTiles(world, player, previews, structure, x, y, z, stack, unplaceableTiles, cutoutInfo, placeMode)) {
            ItemStack currentStack = player.inventory.mainInventory[player.inventory.currentItem];
            boolean isChisel = currentStack != null && currentStack.getItem() == LittleTiles.chisel;
            if (!player.capabilities.isCreativeMode && !isChisel) {
                currentStack.stackSize--;
                if (currentStack.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
            }

            if (!world.isRemote) {
                for (LittleTile unplaceableTile : unplaceableTiles) {
                    if (!(unplaceableTile instanceof LittleTileBlock) && !ItemTileContainer.addBlock(
                            player,
                            ((LittleTileBlock) unplaceableTile).block,
                            ((LittleTileBlock) unplaceableTile).meta,
                            (float) unplaceableTile.getPercentVolume()))
                        WorldUtils.dropItem(world, unplaceableTile.getDrops(), x, y, z);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public ArrayList<LittleTilePreview> getLittlePreview(ItemStack stack) {
        LittleTilePreview preview = LittleTilePreview.getPreviewFromNBT(stack.stackTagCompound);
        if (preview == null) return null;
        ArrayList<LittleTilePreview> previews = new ArrayList<>();
        previews.add(preview);
        return previews;
    }

    @Override
    public void rotateLittlePreview(ItemStack stack, ForgeDirection direction) {
        if (!stack.hasTagCompound()) return;
        NBTTagCompound old = (NBTTagCompound) stack.stackTagCompound.copy();
        LittleTilePreview.rotatePreview(stack.stackTagCompound, direction);
        new LittleToolHandler(stack).handleRotation(direction, old);
    }

    @Override
    public ArrayList<CubeObject> getRenderingCubes(ItemStack stack) {
        ArrayList<CubeObject> cubes = new ArrayList<>();
        if (!stack.hasTagCompound()) return cubes;
        LittleTilePreview preview = LittleTilePreview.getPreviewFromNBT(stack.stackTagCompound);
        if (preview == null) return cubes;
        CubeObject cube = preview.getCubeBlock();
        if (!(cube.block instanceof BlockAir)) cubes.add(cube);
        return cubes;
    }

    @Override
    public boolean hasBackground(ItemStack stack) {
        return false;
    }

    @Override
    public LittleStructure getLittleStructure(ItemStack stack) {
        return null;
    }

    @Override
    public void flipLittlePreview(ItemStack stack, ForgeDirection direction) {
        // No need to flip one single tile!
    }

}
