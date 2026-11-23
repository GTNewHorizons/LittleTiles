package com.creativemd.littletiles.common.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemLittleSaw extends Item {

    public ItemLittleSaw() {
        setCreativeTab(CreativeTabs.tabTools);
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add("Right Click to increase and Shift + Right Click to decrease the size of a placed tile");
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTSaw";
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles littleEntity) {
            if (!world.isRemote) {
                LittleTile tile = littleEntity.getHitTile(player);
                ForgeDirection direction = ForgeDirection.getOrientation(side);
                if (tile instanceof LittleTileBlock tileBlock && tile.canSawResizeTile(direction, player)) {
                    LittleTileBox box;
                    if (player.isSneaking()) box = tile.boundingBox.shrink(direction);
                    else box = tile.boundingBox.expand(direction);

                    if (box.isBoxInsideBlock() && box.isValidBox()
                            && littleEntity.isSpaceForLittleTile(box.getBox(), tile)) {
                        float ammount = tile.boundingBox.getSize().getPercentVolume()
                                - box.getSize().getPercentVolume();
                        boolean success = false;
                        if (player.isSneaking()) {
                            if (ItemTileContainer.addBlock(player, tileBlock.block, tileBlock.meta, ammount))
                                success = true;
                        } else {
                            if (ItemTileContainer.drainBlock(player, tileBlock.block, tileBlock.meta, -ammount))
                                success = true;
                        }

                        if (player.capabilities.isCreativeMode || success) {
                            tile.boundingBox = box;
                            tile.updateCorner();
                            littleEntity.update();
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

}
