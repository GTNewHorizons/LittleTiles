package com.creativemd.littletiles.common.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.common.packet.LittleBlockPacket;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileTileEntity;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTile extends BlockContainer {

    private Random rand;

    public BlockTile(Material material) {
        super(material);
        setCreativeTab(CreativeTabs.tabDecorations);
        rand = new Random();
    }

    @SideOnly(Side.CLIENT)
    public static Minecraft mc;

    @Override
    @SideOnly(Side.CLIENT)
    public boolean renderAsNormalBlock() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    /*
     * @Override
     * @SideOnly(Side.CLIENT) public int getRenderBlockPass() { return 1; }
     * @Override
     * @SideOnly(Side.CLIENT) public boolean canRenderInPass(int pass) { return pass == getRenderBlockPass(); }
     */

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isBed(IBlockAccess world, int x, int y, int z, EntityLivingBase player) {
        if (loadTileEntity(world, x, y, z)) {
            for (LittleTile tile : tempEntity.getTiles()) {
                if (tile.isBed(world, x, y, z, player)) return true;
            }
        }
        return false;
    }

    @Override
    public ChunkCoordinates getBedSpawnPosition(IBlockAccess world, int x, int y, int z, EntityPlayer player) {
        if (world instanceof World) return new ChunkCoordinates(x, y, z);
        return null;
    }

    @Override
    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
        AxisAlignedBB bb = entity.boundingBox;
        int mX = MathHelper.floor_double(bb.minX);
        int mY = MathHelper.floor_double(bb.minY);
        int mZ = MathHelper.floor_double(bb.minZ);
        for (int y2 = mY; y2 < bb.maxY; y2++) {
            for (int x2 = mX; x2 < bb.maxX; x2++) {
                for (int z2 = mZ; z2 < bb.maxZ; z2++) {
                    TileEntity te = world.getTileEntity(x, y, z2);
                    if (te instanceof TileEntityLittleTiles) {
                        TileEntityLittleTiles littleTE = (TileEntityLittleTiles) te;
                        List<LittleTile> tiles = littleTE.getTiles();
                        for (LittleTile tile : tiles) {
                            if (tile.isLadder()) {
                                if (tile.boundingBox != null) {
                                    LittleTileBox box = tile.boundingBox.copy();
                                    box.addOffset(new LittleTileVec(x2 * 16, y2 * 16, z2 * 16));
                                    double expand = 0.0001;
                                    if (bb.intersectsWith(box.getBox().expand(expand, expand, expand))) return true;
                                }
                            }

                        }
                    }
                    /*
                     * block = world.getBlock(x2, y2, z2); if (block != null && block.isLadder(world, x2, y2, z2,
                     * entity)) { return true; }
                     */
                }
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderType() {
        return LittleTilesClient.modelID;
    }

    @Override
    public boolean isNormalCube() {
        return false;
    }

    @Override
    public int getMobilityFlag() {
        return 2;
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(player)
                && tempEntity.loadedTile instanceof LittleTileBlock) {
            return ((LittleTileBlock) tempEntity.loadedTile).block
                    .getPlayerRelativeBlockHardness(player, world, x, y, z);
        }
        return super.getBlockHardness(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(mc.thePlayer)) {
            try { // Why try? because the number of tiles can change while this method is called
                return tempEntity.loadedTile.getSelectedBox().getOffsetBoundingBox(x, y, z);
            } catch (Exception ignored) {

            }
        }
        return AxisAlignedBB.getBoundingBox(x, y, z, x, y, z);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB axis, List list,
            Entity entity) {
        if (loadTileEntity(world, x, y, z)) {
            for (LittleTile tile : tempEntity.getTiles()) {
                if (tile.boundingBox != null) {
                    AxisAlignedBB box = tile.boundingBox.getBox().getOffsetBoundingBox(x, y, z);
                    if (axis.intersectsWith(box)) list.add(box);
                }

            }
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBlockBounds(0, 0, 0, 0, 0, 0);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        if (loadTileEntity(world, x, y, z) && tempEntity.getTiles().size() == 0)
            super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (loadTileEntity(world, x, y, z)) for (LittleTile tile : tempEntity.getTiles()) {
            tile.randomDisplayTick(world, x, y, z, random);
        }
    }

    public static boolean cancelNext = false;

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float moveX,
            float moveY, float moveZ) {
        if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(player)) {
            try {
                if (world.isRemote) PacketHandler.sendPacketToServer(new LittleBlockPacket(x, y, z, player, 0));
                return tempEntity.loadedTile.onBlockActivated(world, x, y, z, player, side, moveX, moveY, moveZ);
            } catch (Exception ignored) {

            }
        }
        if (cancelNext) {
            cancelNext = false;
            return true;
        }
        return false;
    }

    /*
     * public int isProvidingWeakPower(IBlockAccess p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int
     * p_149709_5_) { return 0; } public boolean canProvidePower() { return false; } public int
     * isProvidingStrongPower(IBlockAccess p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int
     * p_149748_5_) { return 0; } public boolean hasComparatorInputOverride() { return false; } public int
     * getComparatorInputOverride(World p_149736_1_, int p_149736_2_, int p_149736_3_, int p_149736_4_, int p_149736_5_)
     * { return 0; }
     */

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List items) {
        /*
         * ItemStack stack = new ItemStack(LittleTiles.blockTile); LittleTile tile = new LittleTile(Blocks.stone, 0, new
         * LittleTileVec(1, 1, 1)); for (byte x = 1; x <= 16; x++) for (byte y = 1; y <= 16; y++) for (byte z = 1; z <=
         * 16; z++) { tile.size = new LittleTileVec(x, y, z); ItemStack newStack = stack.copy();
         * newStack.stackTagCompound = new NBTTagCompound(); tile.save(newStack.stackTagCompound); items.add(newStack);
         * }
         */
    }

    // TODO Add this once it's important
    // public void fillWithRain(World p_149639_1_, int p_149639_2_, int p_149639_3_, int p_149639_4_) {}

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        try { // Why try? because the number of tiles can change while this method is called
            if (loadTileEntity(world, x, y, z)) {
                return tempEntity.getMaxLightValue();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        // TODO Add before a prerelease
        return false;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(player)) {
                try {
                    tempEntity.loadedTile.destroy();
                    NBTTagCompound nbt = new NBTTagCompound();
                    tempEntity.writeToNBT(nbt);
                    PacketHandler.sendPacketToServer(new LittleBlockPacket(x, y, z, player, 1));
                    tempEntity.updateRender();
                } catch (Exception ignored) {

                }
            }

        }
        return true;
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        return removedByPlayer(world, player, x, y, z);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        if (loadTileEntity(world, x, y, z)) return tempEntity.getTiles().size() == 0;
        return true;
    }

    @Override
    /** Blocks will drop before this method is called */
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if (loadTileEntity(world, x, y, z)) {
            for (LittleTile tile : tempEntity.getTiles()) {
                stacks.addAll(tile.getDrops());
            }
        }
        return stacks;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(player)) {
            try {
                ArrayList<ItemStack> drops = tempEntity.loadedTile.getDrops();
                if (drops.size() > 0) return drops.get(0);
            } catch (Exception ignored) {

            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    public IIcon overrideIcon;

    private float getSizeForSide(AxisAlignedBB box, int side) {
        double size = 1;
        switch (side) {
            case 0:
            case 1:
                size = (box.maxX - box.minX + box.maxZ - box.minZ) / 2;
                break;
            case 2:
            case 3:
                size = (box.maxX - box.minX + box.maxY - box.minY) / 2;
                break;
            case 4:
            case 5:
                size = (box.maxY - box.minY + box.maxZ - box.minZ) / 2;
                break;
            case -1:
                size = (box.maxX - box.minX + box.maxY - box.minY + box.maxZ - box.minZ) / 3;
                break;
        }
        size = 0.333 + 0.666 * size;
        return (float) size;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {
        try { // Why try? because the loaded tile can change while setting this icon
            if (loadTileEntity(worldObj, target.blockX, target.blockY, target.blockZ)
                    && tempEntity.updateLoadedTile(mc.thePlayer)) {

                AxisAlignedBB box = tempEntity.loadedTile.getSelectedBox();

                int meta = 0;
                if (tempEntity.loadedTile.boundingBox != null) {
                    meta = tempEntity.loadedTile.boundingBox.getCube().meta;
                }

                float f = 0.1F;
                double d0 = target.blockX + rand.nextDouble() * (box.maxX - box.minX - (f * 2.0)) + f + box.minX;
                double d1 = target.blockY + rand.nextDouble() * (box.maxY - box.minY - (f * 2.0)) + f + box.minY;
                double d2 = target.blockZ + rand.nextDouble() * (box.maxZ - box.minZ - (f * 2.0)) + f + box.minZ;

                switch (target.sideHit) {
                    case 0:
                        d1 = target.blockY + box.minY - f;
                        break;
                    case 1:
                        d1 = target.blockY + box.maxY + f;
                        break;
                    case 2:
                        d2 = target.blockZ + box.minZ - f;
                        break;
                    case 3:
                        d2 = target.blockZ + box.maxZ + f;
                        break;
                    case 4:
                        d0 = target.blockX + box.minX - f;
                        break;
                    case 5:
                        d0 = target.blockX + box.maxX + f;
                        break;
                }

                EntityDiggingFX fx = new EntityDiggingFX(worldObj, d0, d1, d2, 0, 0, 0, this, meta);
                fx.applyColourMultiplier(target.blockX, target.blockY, target.blockZ);
                fx.multiplyVelocity(0.2F);
                // Shrink particles for smaller tiles
                float size = getSizeForSide(box, target.sideHit);
                fx.multipleParticleScaleBy(0.6F * size);
                fx.setParticleIcon(tempEntity.loadedTile.getIcon(0));
                effectRenderer.addEffect(fx);

                return true;
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        try { // Why try? because the loaded tile can change while setting this icon
            if (loadTileEntity(world, x, y, z) && tempEntity.updateLoadedTile(mc.thePlayer)) {
                AxisAlignedBB box = tempEntity.loadedTile.getSelectedBox();
                meta = 0;
                if (tempEntity.loadedTile.boundingBox != null) {
                    meta = tempEntity.loadedTile.boundingBox.getCube().meta;
                }

                byte b0 = 4;

                for (int i1 = 0; i1 < b0; ++i1) {
                    for (int j1 = 0; j1 < b0; ++j1) {
                        for (int k1 = 0; k1 < b0; ++k1) {
                            double radiusX = (box.maxX - box.minX);
                            double radiusY = (box.maxY - box.minY);
                            double radiusZ = (box.maxZ - box.minZ);
                            double f1 = (i1 + 0.5) / b0 * radiusX;
                            double f2 = (i1 + 0.5) / b0 * radiusY;
                            double f3 = (i1 + 0.5) / b0 * radiusZ;
                            double d0 = x + box.minX + f1;
                            double d1 = y + box.minY + f2;
                            double d2 = z + box.minZ + f3;
                            EntityDiggingFX fx = new EntityDiggingFX(world, d0, d1, d2, f1, f2, f3, this, meta);
                            fx.applyColourMultiplier(x, y, z);
                            // Shrink particles for smaller tiles
                            float size = getSizeForSide(box, -1);
                            fx.multipleParticleScaleBy(size);
                            fx.setParticleIcon(tempEntity.loadedTile.getIcon(0));
                            effectRenderer.addEffect(fx);
                        }
                    }
                }
                return true;
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (overrideIcon != null) {
            // overrideIcon = null;
            return overrideIcon;
        } else return Blocks.stone.getBlockTextureFromSide(0); // mc.getTextureMapBlocks().getAtlasSprite("MISSING");
    }

    /*
     * TODO Add once it's important public boolean canSustainPlant(IBlockAccess world, int x, int y, int z,
     * ForgeDirection direction, IPlantable plantable) { }
     */

    /*
     * public int getLightOpacity(IBlockAccess world, int x, int y, int z) { }
     */

    /*
     * public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) { return
     * RotationHelper.rotateVanillaBlock(this, worldObj, x, y, z, axis); } public ForgeDirection[]
     * getValidRotations(World worldObj, int x, int y, int z) { }
     */

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        for (int i = 0; i < 6; i++) {
            if (!isSideSolid(world, x, y, z, ForgeDirection.getOrientation(i))) return false;
        }
        return true;
    }

    @Override
    public float getEnchantPowerBonus(World world, int x, int y, int z) {
        float bonus = 0F;
        if (loadTileEntity(world, x, y, z)) {
            for (LittleTile tile : tempEntity.getTiles()) {
                bonus += tile.getEnchantPowerBonus(world, x, y, z) * tile.getPercentVolume();
            }
        }
        return bonus;
    }

    /*
     * public void onEntityCollidedWithBlock(World p_149670_1_, int p_149670_2_, int p_149670_3_, int p_149670_4_,
     * Entity p_149670_5_) {}
     */

    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 vec1, Vec3 vec2) {
        if (loadTileEntity(world, x, y, z)) {
            try { // Why try? because the number of tiles can change while this method is called
                MovingObjectPosition moving = tempEntity.getMoving(vec1, vec2, false);

                if (moving != null) {
                    moving.blockX = x;
                    moving.blockY = y;
                    moving.blockZ = z;
                    return moving;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityLittleTiles();
    }

    public static TileEntityLittleTiles tempEntity;

    public static boolean loadTileEntity(IBlockAccess world, int x, int y, int z) {
        if (world == null) {
            tempEntity = null;
            return false;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles) tempEntity = (TileEntityLittleTiles) tileEntity;
        else tempEntity = null;
        return tempEntity != null;
    }

    public static TileEntity getTileEntityInWorld(IBlockAccess world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles
                && ((TileEntityLittleTiles) tileEntity).loadedTile instanceof LittleTileTileEntity) {
            return ((LittleTileTileEntity) ((TileEntityLittleTiles) tileEntity).loadedTile).tileEntity;
        }
        return tileEntity;
    }

    public static LittleTile getLittleTileInWorld(IBlockAccess world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles) {
            return ((TileEntityLittleTiles) tileEntity).loadedTile;
        }
        return null;
    }

}
