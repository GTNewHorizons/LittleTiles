package com.creativemd.littletiles.common.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.common.utils.WorldUtils;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.items.ItemColorTube;
import com.creativemd.littletiles.common.items.ItemTileContainer;
import com.creativemd.littletiles.common.items.ItemRecipe;
import com.creativemd.littletiles.common.placement.LittleTileChangeRecorder;
import com.creativemd.littletiles.common.placement.LittleTilesPlacementHistory;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlock;
import com.creativemd.littletiles.common.utils.LittleTileBlockColored;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class LittleBlockPacket extends CreativeCorePacket {

    /** DESTROY: the client harvested every tile of the block, see {@link BlockTile#takesEntireBlock}. */
    public static final String ENTIRE_BLOCK = "entireBlock";

    public int x;
    public int y;
    public int z;
    public Vec3 pos;
    public Vec3 look;
    public Action action;
    public NBTTagCompound nbt;

    public enum Action {

        ACTIVATE,
        DESTROY,
        SAW,
        COLOR,
        SPLIT,
        TOGGLE_COLLISION,
        TOGGLE_CONNECTION,
        RECIPE_PICK;

        public static Action get(int ordinal) {
            if (ordinal >= 0 && ordinal < values().length) return values()[ordinal];
            return ACTIVATE;
        }
    }

    public LittleBlockPacket() {

    }

    public LittleBlockPacket(int x, int y, int z, EntityPlayer player, Action action) {
        this(x, y, z, player, action, new NBTTagCompound());
    }

    public LittleBlockPacket(int x, int y, int z, EntityPlayer player, Action action, NBTTagCompound nbt) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
        this.pos = player.getPosition(1);
        double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
        Vec3 look = player.getLook(1.0F);
        this.look = pos.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);
        this.nbt = nbt;
    }

    @Override
    public void writeBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        writeVec3(pos, buf);
        writeVec3(look, buf);
        buf.writeInt(action.ordinal());
        writeNBT(buf, nbt);
    }

    @Override
    public void readBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        pos = readVec3(buf);
        look = readVec3(buf);
        action = Action.get(buf.readInt());
        nbt = readNBT(buf);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void executeClient(EntityPlayer player) {

    }

    @Override
    public void executeServer(EntityPlayer player) {
        TileEntity tileEntity = player.worldObj.getTileEntity(x, y, z);
        if (tileEntity instanceof TileEntityLittleTiles) {
            TileEntityLittleTiles littleEntity = (TileEntityLittleTiles) tileEntity;
            MovingObjectPosition moving = littleEntity.getMoving(pos, look, true);
            LittleTile tile = littleEntity.loadedTile;
            if (tile != null) {
                switch (action) {
                    case ACTIVATE:
                        if (tile.onBlockActivated(
                                player.worldObj,
                                x,
                                y,
                                z,
                                player,
                                moving.sideHit,
                                (float) moving.hitVec.xCoord,
                                (float) moving.hitVec.yCoord,
                                (float) moving.hitVec.zCoord))
                            BlockTile.cancelNext = true;
                        break;
                    case DESTROY:
                        // The client removes the tile on its own, so this is the only place the server
                        // can refuse, as it would a vanilla block break: claims, other protection, or a
                        // WorldEdit wand. A refused break resends the block to the client.
                        if (!(player instanceof EntityPlayerMP playerMP)) break;
                        if (ForgeHooks.onBlockBreakEvent(
                                player.worldObj,
                                playerMP.theItemInWorldManager.getGameType(),
                                playerMP,
                                x,
                                y,
                                z).isCanceled())
                            break;
                        boolean entireBlock = nbt.getBoolean(ENTIRE_BLOCK) && BlockTile.takesEntireBlock(player, tile);
                        // Breaking the entire block leaves structures alone, so only a single tile can reach
                        // further.
                        if (!entireBlock && !mayBreakWholeStructure(playerMP, tile, x, y, z)) break;
                        boolean creative = player.capabilities.isCreativeMode;
                        LittleTileChangeRecorder recorder = new LittleTileChangeRecorder(player.worldObj);
                        if (creative) recorder.watch(littleEntity.getCoord());
                        if (entireBlock) {
                            List<LittleTile> tiles = BlockTile.selectEntireBlock(littleEntity);
                            // The item is made before the tiles leave the block they are saved from
                            ItemStack drop = creative ? null : BlockTile.getEntireBlockDrop(player.worldObj, tiles);
                            littleEntity.removeTiles(tiles);
                            if (drop != null) WorldUtils.dropItem(player.worldObj, drop, x, y, z);
                        } else {
                            tile.destroy();
                            // littleEntity.removeTile(tile);
                            if (!creative) WorldUtils.dropItem(player.worldObj, tile.getDrops(), x, y, z);
                        }
                        littleEntity.update();
                        if (creative) LittleTilesPlacementHistory.recordAction(
                                player,
                                new LittleTilesPlacementHistory.PlacementAction(
                                        player.worldObj.provider.dimensionId,
                                        recorder.finish().createPlan()));
                        break;
                    case SAW:
                        try {
                            int side = nbt.getInteger("side");
                            ForgeDirection direction = ForgeDirection.getOrientation(side);
                            TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
                            if (te.updateLoadedTileServer(pos, look)
                                    && te.loadedTile.canSawResizeTile(direction, player)) {
                                LittleTileBox box;
                                if (player.isSneaking()) box = te.loadedTile.boundingBox.shrink(direction);
                                else box = te.loadedTile.boundingBox.expand(direction);

                                if (box.isBoxInsideBlock() && box.isValidBox()
                                        && te.isSpaceForLittleTile(box.getBox(), te.loadedTile)) {
                                    float ammount = te.loadedTile.boundingBox.getSize().getPercentVolume()
                                            - box.getSize().getPercentVolume();
                                    boolean success = false;
                                    if (player.isSneaking()) {
                                        if (ItemTileContainer.addBlock(
                                                player,
                                                ((LittleTileBlock) te.loadedTile).block,
                                                ((LittleTileBlock) te.loadedTile).meta,
                                                ammount))
                                            success = true;
                                    } else {
                                        if (ItemTileContainer.drainBlock(
                                                player,
                                                ((LittleTileBlock) te.loadedTile).block,
                                                ((LittleTileBlock) te.loadedTile).meta,
                                                -ammount))
                                            success = true;
                                    }

                                    if (player.capabilities.isCreativeMode || success) {
                                        te.loadedTile.boundingBox = box;
                                        te.loadedTile.updateCorner();
                                        te.update();
                                    }
                                }
                            }

                        } catch (Exception e) {
                            System.out.println("Failed to use saw!");
                            e.printStackTrace();
                        }
                        break;
                    case COLOR:
                        try {
                            TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
                            if (te.updateLoadedTileServer(pos, look)
                                    && (te.loadedTile.getClass() == LittleTileBlock.class
                                            || te.loadedTile instanceof LittleTileBlockColored)) {
                                int color = nbt.getInteger("color");
                                LittleTile currentTile = te.loadedTile;

                                int index = te.getTiles().indexOf(currentTile);
                                if (player.isSneaking()) {
                                    color = ColorUtils.WHITE;
                                    if (currentTile instanceof LittleTileBlockColored)
                                        color = ((LittleTileBlockColored) currentTile).color;
                                    ItemColorTube.setColor(player.getCurrentEquippedItem(), color);
                                } else {

                                    LittleTile newTile = LittleTileBlockColored
                                            .setColor((LittleTileBlock) currentTile, color);

                                    if (newTile != null) te.getTiles().set(index, newTile);
                                    te.update();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Failed to use color tube!");
                            e.printStackTrace();
                        }
                        break;
                    case SPLIT:
                        TileEntityLittleTiles te = (TileEntityLittleTiles) tileEntity;
                        ArrayList<LittleTile> newTiles = new ArrayList<>();
                        if (te.updateLoadedTileServer(pos, look)
                                && (te.loadedTile.getClass() == LittleTileBlock.class
                                        || te.loadedTile instanceof LittleTileBlockColored)
                                && te.loadedTile.structure == null) {
                            LittleTile oldTile = te.loadedTile;
                            LittleTileBox box = oldTile.boundingBox;
                            if (box != null) {
                                for (int littleX = box.minX; littleX < box.maxX; littleX++) {
                                    for (int littleY = box.minY; littleY < box.maxY; littleY++) {
                                        for (int littleZ = box.minZ; littleZ < box.maxZ; littleZ++) {
                                            tile = oldTile.copy();
                                            tile.boundingBox = new LittleTileBox(
                                                    littleX,
                                                    littleY,
                                                    littleZ,
                                                    littleX + 1,
                                                    littleY + 1,
                                                    littleZ + 1);
                                            tile.updateCorner();
                                            tile.te = te;
                                            newTiles.add(tile);
                                        }
                                    }
                                }
                            }

                            if (LittleTiles.maxNewTiles >= newTiles.size() - 1) {
                                te.removeTile(oldTile, false);
                                te.addTiles(newTiles);
                                te.update();
                            } else {
                                player.addChatComponentMessage(
                                        new ChatComponentText("Too much new tiles! Limit=" + LittleTiles.maxNewTiles));
                            }
                        }
                        break;
                    case RECIPE_PICK:
                        boolean pickBlock = nbt.getBoolean(ENTIRE_BLOCK) && BlockTile.takesEntireBlock(player, tile);
                        List<LittleTile> picked = pickBlock ? BlockTile.selectEntireBlock(littleEntity)
                                : Collections.singletonList(tile);
                        ItemRecipe.pickTiles(player, player.getHeldItem(), picked);
                        break;
                    case TOGGLE_COLLISION:
                        if (player.getCurrentEquippedItem() == null
                                || player.getCurrentEquippedItem().getItem() != LittleTiles.collisionTool)
                            break;
                        TileEntityLittleTiles collisionTE = (TileEntityLittleTiles) tileEntity;
                        if (collisionTE.updateLoadedTileServer(pos, look)) {
                            collisionTE.loadedTile.disableCollision = !collisionTE.loadedTile.disableCollision;
                            collisionTE.update();
                            player.addChatComponentMessage(
                                    new ChatComponentTranslation(
                                            collisionTE.loadedTile.disableCollision
                                                    ? "message.littletiles.tile_collision.disabled"
                                                    : "message.littletiles.tile_collision.enabled"));
                        }
                        break;
                    case TOGGLE_CONNECTION:
                        if (!player.capabilities.isCreativeMode || player.getCurrentEquippedItem() == null
                                || player.getCurrentEquippedItem().getItem() != LittleTiles.connectionTool)
                            break;
                        TileEntityLittleTiles connectionTE = (TileEntityLittleTiles) tileEntity;
                        if (connectionTE.updateLoadedTileServer(pos, look)) {
                            connectionTE.loadedTile.disableConnection = !connectionTE.loadedTile.disableConnection;
                            connectionTE.update();
                            player.addChatComponentMessage(
                                    new ChatComponentTranslation(
                                            connectionTE.loadedTile.disableConnection
                                                    ? "message.littletiles.tile_connection.disabled"
                                                    : "message.littletiles.tile_connection.enabled"));
                        }
                        break;
                }
            }
        }
    }

    /**
     * Breaking one tile of a structure removes all of it, possibly across several blocks, so the player must be allowed
     * to break each of those blocks, not just the clicked one. Otherwise a structure reaching into a protected claim
     * could be removed from outside it.
     *
     * @return false if any block refused, after resending the clicked block to the client
     */
    private static boolean mayBreakWholeStructure(EntityPlayerMP player, LittleTile tile, int x, int y, int z) {
        if (!tile.isStructureBlock || !tile.isLoaded()) return true;
        World world = player.worldObj;
        for (ChunkCoordinates coord : tile.structure.getTilesSortedPerBlock().getKeys()) {
            if (coord.posX == x && coord.posY == y && coord.posZ == z) continue;
            if (ForgeHooks.onBlockBreakEvent(
                    world,
                    player.theItemInWorldManager.getGameType(),
                    player,
                    coord.posX,
                    coord.posY,
                    coord.posZ).isCanceled()) {
                // The client already removed the tile it clicked; put it back as Forge does for the other block.
                player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
                TileEntity clicked = world.getTileEntity(x, y, z);
                if (clicked != null) {
                    Packet description = clicked.getDescriptionPacket();
                    if (description != null) player.playerNetServerHandler.sendPacket(description);
                }
                return false;
            }
        }
        return true;
    }
}
