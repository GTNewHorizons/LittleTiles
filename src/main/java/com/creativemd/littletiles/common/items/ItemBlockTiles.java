package com.creativemd.littletiles.common.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.common.utils.HashMapList;
import com.creativemd.creativecore.common.utils.WorldUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.packet.LittlePlacePacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTile.LittleTilePosition;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.PlacementHelper;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileCoord;
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

        PlacementHelper helper = PlacementHelper.getInstance(player);

        MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;

        int align = 1;
        if (stack.getItem() == LittleTiles.chisel) {
            align = new LittleToolHandler(stack).getGrid();
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
                    new LittlePlacePacket(stack, pos, PreviewRenderer.markedHit != null, cutoutInfo));

            placeBlockAt(player, stack, world, pos, helper, PreviewRenderer.markedHit != null, cutoutInfo);

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
    public boolean func_150936_a(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        Block block = world.getBlock(x, y, z);

        MovingObjectPosition moving = Minecraft.getMinecraft().objectMouseOver;

        PlacementHelper helper = PlacementHelper.getInstance(player);
        LittleTileBlockPos pos = LittleTileBlockPos.fromMovingObjectPosition(moving, 1);
        if (PreviewRenderer.markedHit != null) pos = PreviewRenderer.markedHit;

        x = pos.getPosX();
        y = pos.getPosY();
        z = pos.getPosZ();
        block = world.getBlock(x, y, z);
        return block.isReplaceable(world, x, y, z)
                || PlacementHelper.getInstance(player).canBePlacedInsideBlock(x, y, z);
    }

    public static HashMapList<ChunkCoordinates, PreviewTile> getSplittedTiles(ArrayList<PreviewTile> tiles, int x,
            int y, int z) {
        HashMapList<ChunkCoordinates, PreviewTile> splitted = new HashMapList<>();
        for (PreviewTile tile : tiles) {
            if (!tile.split(splitted, x, y, z)) return null;
        }
        return splitted;
    }

    public static boolean canPlaceTiles(World world, HashMapList<ChunkCoordinates, PreviewTile> splitted,
            ArrayList<ChunkCoordinates> coordsToCheck) {
        for (ChunkCoordinates coord : coordsToCheck) {
            TileEntity mainTile = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
            if (mainTile instanceof TileEntityLittleTiles) {

                ArrayList<PreviewTile> tiles = splitted.getValues(coord);
                if (tiles != null) {
                    for (PreviewTile tile : tiles) if (tile.needsCollisionTest()
                            && !((TileEntityLittleTiles) mainTile).isSpaceForLittleTile(tile.box))
                        return false;
                }
            } else if (!(world.getBlock(coord.posX, coord.posY, coord.posZ) instanceof BlockTile)
                    && !world.getBlock(coord.posX, coord.posY, coord.posZ).getMaterial().isReplaceable())
                return false;
        }
        return true;
    }

    public static boolean placeTiles(World world, EntityPlayer player, ArrayList<PreviewTile> previews,
            LittleStructure structure, int x, int y, int z, ItemStack stack, ArrayList<LittleTile> unplaceableTiles,
            LittleTileCutoutInfo cutoutInfo) {

        HashMapList<ChunkCoordinates, PreviewTile> splitted = getSplittedTiles(previews, x, y, z);
        if (splitted == null) return false;

        ArrayList<ChunkCoordinates> coordsToCheck;
        if (structure != null) {
            coordsToCheck = splitted.getKeys();
        } else {
            coordsToCheck = new ArrayList<>();
            coordsToCheck.add(new ChunkCoordinates(x, y, z));
        }
        ArrayList<SoundType> soundsToBePlayed = new ArrayList<>();
        if (canPlaceTiles(world, splitted, coordsToCheck)) {
            LittleTilePosition pos = null;

            for (int i = 0; i < splitted.size(); i++) {
                ChunkCoordinates coord = splitted.getKey(i);
                ArrayList<PreviewTile> placeTiles = splitted.getValues(i);
                boolean hascollideBlock = false;
                for (PreviewTile tile : placeTiles) {
                    if (tile.needsCollisionTest()) {
                        hascollideBlock = true;
                        break;
                    }
                }
                if (hascollideBlock) {
                    if (!(world.getBlock(coord.posX, coord.posY, coord.posZ) instanceof BlockTile)
                            && world.getBlock(coord.posX, coord.posY, coord.posZ).getMaterial().isReplaceable())
                        world.setBlock(coord.posX, coord.posY, coord.posZ, LittleTiles.blockTile, 0, 3);

                    TileEntity te = world.getTileEntity(coord.posX, coord.posY, coord.posZ);
                    if (te instanceof TileEntityLittleTiles) {
                        TileEntityLittleTiles teLT = (TileEntityLittleTiles) te;

                        for (PreviewTile placeTile : placeTiles) {

                            LittleTileCutoutInfo cutoutInfoCurrent = null;
                            if (cutoutInfo != null) {
                                LittleTileBox originalBox = previews.get(0).box;
                                LittleTileBox currentBox = placeTile.box;

                                cutoutInfoCurrent = new LittleTileCutoutInfo(cutoutInfo);
                                cutoutInfoCurrent.pos.x += (x - coord.posX) * 16 + originalBox.minX - currentBox.minX;
                                cutoutInfoCurrent.pos.y += (y - coord.posY) * 16 + originalBox.minY - currentBox.minY;
                                cutoutInfoCurrent.pos.z += (z - coord.posZ) * 16 + originalBox.minZ - currentBox.minZ;

                                Mesh3d mesh = Mesh3dUtil.createMesh(
                                        0,
                                        0,
                                        0,
                                        cutoutInfoCurrent,
                                        currentBox.minX / 16.0,
                                        currentBox.minY / 16.0,
                                        currentBox.minZ / 16.0,
                                        currentBox.maxX / 16.0,
                                        currentBox.maxY / 16.0,
                                        currentBox.maxZ / 16.0,
                                        null,
                                        0);
                                if (mesh.getTriangles().isEmpty()) {
                                    continue;
                                }
                            }

                            LittleTile LT = placeTile.placeTile(player, stack, teLT, structure, unplaceableTiles);
                            if (LT != null) {
                                LT.setCutoutInfo(cutoutInfoCurrent);
                                if (!soundsToBePlayed.contains(LT.getSound())) soundsToBePlayed.add(LT.getSound());
                                if (structure != null) {
                                    if (pos == null) {
                                        structure.mainTile = LT;
                                        LT.isMainBlock = true;
                                        LT.updateCorner();
                                        pos = new LittleTilePosition(coord, LT.cornerVec.copy());
                                    } else LT.coord = new LittleTileCoord(teLT, pos.coord, pos.position);
                                }
                            }
                        }

                        if (structure != null) teLT.combineTiles(structure);
                    }
                }
            }
            for (SoundType soundType : soundsToBePlayed) {
                world.playSoundEffect(
                        (float) player.posX,
                        (float) player.posY,
                        (float) player.posZ,
                        soundType.func_150496_b(),
                        (soundType.getVolume() + 1.0F) / 2.0F,
                        soundType.getPitch() * 0.8F);
            }
            return true;
        }
        return false;
    }

    public boolean placeBlockAt(EntityPlayer player, ItemStack stack, World world, LittleTileBlockPos pos,
            PlacementHelper helper, boolean customPlacement, LittleTileCutoutInfo cutoutInfo) {
        ArrayList<PreviewTile> previews = helper.getPreviewTiles(stack, pos, customPlacement);

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
        if (placeTiles(world, player, previews, structure, x, y, z, stack, unplaceableTiles, cutoutInfo)) {
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
        ArrayList<LittleTilePreview> previews = new ArrayList<>();
        previews.add(LittleTilePreview.getPreviewFromNBT(stack.stackTagCompound));
        return previews;
    }

    @Override
    public void rotateLittlePreview(ItemStack stack, ForgeDirection direction) {
        NBTTagCompound old = (NBTTagCompound) stack.stackTagCompound.copy();
        LittleTilePreview.rotatePreview(stack.stackTagCompound, direction);
        new LittleToolHandler(stack).handleRotation(direction, old);
    }

    @Override
    public ArrayList<CubeObject> getRenderingCubes(ItemStack stack) {
        ArrayList<CubeObject> cubes = new ArrayList<>();
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
