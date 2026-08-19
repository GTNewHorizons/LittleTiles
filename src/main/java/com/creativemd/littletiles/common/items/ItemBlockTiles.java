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
import net.minecraft.util.ChunkCoordinates;
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
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleTilesPlacementHistory;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
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

        if (needsTwoHits(stack)) {
            if (PreviewRenderer.firstHit == null && PreviewRenderer.markedHit == null) {
                PreviewRenderer.firstHit = pos;
                return true;
            }

            // The preview carries the cutout in its nbt, so the placed stack keeps it as well.
            ILittleTile littleTile = (ILittleTile) stack.getItem();
            NBTTagCompound tag = (NBTTagCompound) littleTile.getLittlePreview(stack).get(0).nbt.copy();
            stack = new ItemStack(Item.getItemFromBlock(LittleTiles.blockTile));
            stack.stackTagCompound = tag;
            PreviewRenderer.firstHit = null;
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
                    new LittlePlacePacket(stack, pos, PreviewRenderer.markedHit != null, placeMode));

            placeBlockAt(player, stack, world, pos, PreviewRenderer.markedHit != null, placeMode);

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
            LittleTilePlaceMode placeMode) {
        LittleTilePlacementPlan plan = new LittleTilePlacementPlan();
        plan.fillPlan(world, x, y, z, previews, structure, placeMode);
        if (!plan.canApplyPlan()) {
            return false;
        }

        return plan.applyPlan(world, player, stack, structure, unplaceableTiles);
    }

    public boolean placeBlockAt(EntityPlayer player, ItemStack stack, World world, LittleTileBlockPos pos,
            boolean customPlacement, LittleTilePlaceMode placeMode) {
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

        LittleTilePlacementPlan plan = new LittleTilePlacementPlan();
        plan.fillPlan(world, x, y, z, previews, structure, placeMode);
        if (!plan.canApplyPlan()) {
            return false;
        }

        ArrayList<LittleTile> unplaceableTiles = new ArrayList<>();
        ArrayList<ChunkCoordinates> plannedCoords = collectPlannedCoords(previews, x, y, z);
        ItemStack heldBeforePlace = player.inventory.mainInventory[player.inventory.currentItem];
        boolean isStencilChiselNonCreative = !player.capabilities.isCreativeMode && heldBeforePlace != null
                && heldBeforePlace.getItem() == LittleTiles.chisel
                && placeMode == LittleTilePlaceMode.STENCIL;
        boolean shouldRecordHistory = !world.isRemote && !isStencilChiselNonCreative;
        ArrayList<LittleTilesPlacementHistory.BlockSnapshot> beforeStates = null;
        if (shouldRecordHistory) {
            beforeStates = LittleTilesPlacementHistory.captureSnapshots(world, plannedCoords);
        }

        if (plan.applyPlan(world, player, stack, structure, unplaceableTiles)) {
            ItemStack currentStack = player.inventory.mainInventory[player.inventory.currentItem];
            boolean isChisel = currentStack != null && currentStack.getItem() == LittleTiles.chisel;
            if (!player.capabilities.isCreativeMode && !isChisel) {
                currentStack.stackSize--;
                if (currentStack.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
            }

            if (shouldRecordHistory) {
                ArrayList<LittleTilesPlacementHistory.BlockSnapshot> afterStates = LittleTilesPlacementHistory
                        .captureSnapshots(world, plannedCoords);
                LittleTilesPlacementHistory.recordPlacement(
                        player,
                        new LittleTilesPlacementHistory.PlacementAction(
                                world.provider.dimensionId,
                                beforeStates,
                                afterStates));
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

    private static ArrayList<ChunkCoordinates> collectPlannedCoords(ArrayList<PreviewTile> previews, int x, int y,
            int z) {
        ArrayList<ChunkCoordinates> coords = new ArrayList<>();
        LittleTilePlacementPlan.SplitPreviewMap splitMap = new LittleTilePlacementPlan.SplitPreviewMap();
        for (PreviewTile preview : previews) {
            if (!preview.split(splitMap, x, y, z)) {
                return coords;
            }
        }
        for (ChunkCoordinates coord : splitMap.keySet()) {
            coords.add(new ChunkCoordinates(coord.posX, coord.posY, coord.posZ));
        }
        return coords;
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
        LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(stack.stackTagCompound);
        LittleTilePreview.rotatePreview(stack.stackTagCompound, direction);
        new LittleToolHandler(stack).handleRotation(direction, oldSize);
    }

    @Override
    public ArrayList<CubeObject> getRenderingCubes(ItemStack stack) {
        ArrayList<CubeObject> cubes = new ArrayList<>();
        if (!stack.hasTagCompound()) return cubes;
        LittleTile tile = LittleTile.CreateandLoadTile(null, null, stack.stackTagCompound);
        if (tile != null) {
            cubes.addAll(tile.getRenderingCubes());
            return cubes;
        }
        Block block = Block.getBlockFromName(stack.stackTagCompound.getString("block"));
        int meta = stack.stackTagCompound.getInteger("meta");
        LittleTileSize size = new LittleTileSize("size", stack.stackTagCompound);
        if (!(block instanceof BlockAir)) {
            CubeObject cube = new LittleTileBox(new LittleTileVec(8, 8, 8), size, true).getCube();
            cube.block = block;
            cube.meta = meta;
            if (stack.stackTagCompound.hasKey("color")) cube.color = stack.stackTagCompound.getInteger("color");
            cubes.add(cube);
        }
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
