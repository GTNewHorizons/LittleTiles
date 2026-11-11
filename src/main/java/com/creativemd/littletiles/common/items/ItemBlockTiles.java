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
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.LittleTilesConfig;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.client.render.LittleDeformedBoxHelper;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.material.LittleMaterial;
import com.creativemd.littletiles.common.packet.LittlePlacePacket;
import com.creativemd.littletiles.common.placement.LittleTilePlacementPlan;
import com.creativemd.littletiles.common.placement.LittleTilePlacementPlanResult;
import com.creativemd.littletiles.common.placement.LittleTilesPlacementHistory;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
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
            Block block = LittleMaterial.readFromNBT(stack.stackTagCompound).getBlock();
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

    private boolean isDeformedBoxShape(ItemStack stack) {
        return stack.getItem() == LittleTiles.chisel && new LittleToolHandler(stack).isDeformedBoxShape();
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float offsetX, float offsetY, float offsetZ) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) return false;

        boolean placingDeformedBox = false;

        MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;

        int align = 1;
        LittleTilePlaceMode placeMode = LittleTilePlaceMode.NORMAL;
        if (stack.getItem() == LittleTiles.chisel) {
            LittleToolHandler handler = new LittleToolHandler(stack);
            align = handler.getGrid();
            placeMode = handler.getPlaceMode();
        }

        LittleTileBlockPos clickedPos = LittleTileBlockPos.fromMovingObjectPosition(moving, align);
        LittleTileBlockPos pos = clickedPos;

        if (PreviewRenderer.markedHit != null) pos = PreviewRenderer.markedHit;

        if (isDeformedBoxShape(stack)) {
            // Two clicks to close the initial axis-aligned box, and a third to place it.
            if (!LittleDeformedBoxHelper.isEditing()) {
                if (PreviewRenderer.firstHit == null) {
                    PreviewRenderer.firstHit = pos;
                    return true;
                }
                LittleDeformedBoxHelper.beginBox(PreviewRenderer.firstHit, pos, align);
                PreviewRenderer.firstHit = null;
                return true;
            }

            // With a corner selected, right click warps it to where the player is looking instead of placing.
            // All corner markers remain visible while a corner is selected, including when sneaking.
            if (LittleDeformedBoxHelper.hasMarkedCorner()) {
                LittleDeformedBoxHelper.moveMarkedTo(clickedPos);
                return true;
            }

            // Invalid intermediate shapes remain editable and are rendered red, but must not become placed tiles.
            if (!LittleDeformedBoxHelper.hasValidGeometry()) {
                return true;
            }

            // The preview carries the cutout in its nbt, so the placed stack keeps it as well. Both it and the
            // placement anchor have to be read before the corner state is dropped.
            ILittleTile littleTile = (ILittleTile) stack.getItem();
            NBTTagCompound tag = (NBTTagCompound) littleTile.getLittlePreview(stack).get(0).nbt.copy();
            stack = new ItemStack(Item.getItemFromBlock(LittleTiles.blockTile));
            stack.stackTagCompound = tag;
            pos = LittleDeformedBoxHelper.placementAnchor();
            placingDeformedBox = true;
        } else if (needsTwoHits(stack)) {
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
        } else if (!PlacementHelper.hasOnlyValidMaterials(stack)) {
            return false;
        } else {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                // The server places what the player holds, only the tile a chisel makes has to be sent. Anything
                // else, like a big recipe, would not even fit into a packet.
                ItemStack held = player.getHeldItem();
                ItemStack sent = held != null && held.getItem() == LittleTiles.chisel ? stack : null;
                PacketHandler.sendPacketToServer(new LittlePlacePacket(sent, pos, PreviewRenderer.markedHit != null));
            }

            boolean placed = placeBlockAt(player, stack, world, pos, PreviewRenderer.markedHit != null, placeMode);

            if (placed && placingDeformedBox) {
                LittleDeformedBoxHelper.reset();
            }

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

    public boolean placeBlockAt(EntityPlayer player, ItemStack stack, World world, LittleTileBlockPos pos,
            boolean customPlacement, LittleTilePlaceMode placeMode) {
        // Checked before the tiles are read, so a recipe item from before the limit is not even loaded.
        boolean isRecipe = stack.getItem() == LittleTiles.recipe;
        // Placing from a recipe costs no material, so it is creative only until survival accounts for it.
        if (isRecipe && !player.capabilities.isCreativeMode) {
            if (!world.isRemote) {
                player.addChatMessage(new ChatComponentTranslation("littletiles.recipe.creative_only"));
            }
            return false;
        }
        if ((stack.getItem() == LittleTiles.multiTiles || isRecipe) && stack.hasTagCompound()
                && stack.getTagCompound().getInteger("tiles") > LittleTilesConfig.maxRecipeTiles) {
            if (!world.isRemote) {
                player.addChatMessage(
                        new ChatComponentTranslation(
                                "littletiles.recipe.too_many_tiles_to_place",
                                LittleTilesConfig.maxRecipeTiles));
            }
            return false;
        }

        ArrayList<PreviewTile> previews = PlacementHelper.getPreviewTiles(player, stack, pos, customPlacement);
        if (isChiselTooBig(player, previews)) {
            // The client checks the same limit, so only the server tells the player.
            if (!world.isRemote) {
                player.addChatMessage(
                        new ChatComponentTranslation("littletiles.chisel.too_big", LittleTilesConfig.maxChiselBlocks));
            }
            return false;
        }

        LittleStructure structure = null;
        if (stack.getItem() instanceof ILittleTile) {
            structure = ((ILittleTile) stack.getItem()).getLittleStructure(stack);
        } else if (Block.getBlockFromItem(stack.getItem()) instanceof ILittleTile) {
            structure = ((ILittleTile) Block.getBlockFromItem(stack.getItem())).getLittleStructure(stack);
        }

        if (structure != null) {
            // Placed from a recipe, the structure still drops as the multi tiles item it used to be crafted into.
            structure.dropStack = isRecipe ? new ItemStack(LittleTiles.multiTiles) : stack.copy();
            if (isRecipe) {
                structure.dropStack.stackTagCompound = (NBTTagCompound) stack.stackTagCompound.copy();
                structure.dropStack.stackTagCompound.removeTag(ItemRecipe.UNLOCKED);
            }
            structure.dropStack.stackSize = 1;
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

        boolean shouldRecordHistory = !world.isRemote && player.capabilities.isCreativeMode;
        LittleTilePlacementPlanResult placementResult = plan.applyPlan(world, player, stack, structure);
        if (placementResult.hasChanges()) {
            ItemStack currentStack = player.inventory.mainInventory[player.inventory.currentItem];
            boolean isChisel = currentStack != null && currentStack.getItem() == LittleTiles.chisel;
            if (!player.capabilities.isCreativeMode && !isChisel && !isRecipe) {
                currentStack.stackSize--;
                if (currentStack.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
            }

            if (shouldRecordHistory) {
                LittleTilesPlacementHistory.recordAction(
                        player,
                        new LittleTilesPlacementHistory.PlacementAction(
                                world.provider.dimensionId,
                                placementResult.createPlan()));
            }
            return true;
        }
        return false;
    }

    /** Every block a chisel placement reaches into becomes a tile entity, so big placements overload the server. */
    private static boolean isChiselTooBig(EntityPlayer player, ArrayList<PreviewTile> previews) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != LittleTiles.chisel) return false;
        long blocks = 0;
        for (PreviewTile preview : previews) {
            blocks += preview.getBlockCount();
        }
        return blocks > LittleTilesConfig.maxChiselBlocks;
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
        LittleTilePreview preview = LittleTilePreview.getPreviewFromNBT(stack.stackTagCompound);
        if (preview == null) return cubes;
        // The item shows the tile centered, wherever in its block it came from
        preview.box = null;
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
        // A single tile's box never needs flipping, but a chiseled mesh shape still does.
        if (!stack.hasTagCompound()) return;
        if (stack.stackTagCompound.hasKey("cutoutOrientation")) {
            LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(stack.stackTagCompound);
            new LittleToolHandler(stack).handleFlip(direction, oldSize);
        }
    }

}
