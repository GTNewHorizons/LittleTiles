package com.creativemd.littletiles.common.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.TriangleBoundingBoxIntersect;
import com.creativemd.littletiles.client.util3d.TriangleRayIntersect;
import com.creativemd.littletiles.client.util3d.TriangleTriangleIntersect;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityLittleTiles extends TileEntity {

    public static List<LittleTile> createTileList() {
        return Collections.synchronizedList(new ArrayList<LittleTile>());
    }

    private List<LittleTile> tiles = createTileList();

    public void setTiles(List<LittleTile> tiles) {
        this.tiles = tiles;
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) updateCustomRenderer();
    }

    public List<LittleTile> getTiles() {
        return tiles;
    }

    public ArrayList<LittleTile> customRenderingTiles = new ArrayList<>();

    public boolean needsLightUpdate = true;

    public boolean removeTile(LittleTile tile) {
        return removeTile(tile, true);
    }

    public boolean removeTile(LittleTile tile, boolean cleanupTileEntityIfLast) {
        boolean result = tiles.remove(tile);
        updateTiles(cleanupTileEntityIfLast);
        return result;
    }

    public void addTiles(ArrayList<LittleTile> tiles) {
        this.tiles.addAll(tiles);
        updateTiles();
    }

    public boolean addTile(LittleTile tile) {
        boolean result = tiles.add(tile);
        updateTiles();
        return result;
    }

    public void updateTiles() {
        updateTiles(true);
    }

    public void updateTiles(boolean cleanupTileEntityIfLast) {
        if (worldObj != null) {
            needsLightUpdate = true;
            final int lastLight = lastMaxLightValue;

            if (lastLight != getMaxLightValue()) {
                worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
            }

            update();
            updateNeighbor();
            if (!worldObj.isRemote && tiles.isEmpty() && cleanupTileEntityIfLast) {
                worldObj.setBlockToAir(xCoord, yCoord, zCoord);
            }
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) updateCustomRenderer();

    }

    @SideOnly(Side.CLIENT)
    public void updateCustomRenderer() {
        customRenderingTiles.clear();
        for (LittleTile tile : tiles) {
            if (tile.needCustomRendering()) customRenderingTiles.add(tile);
        }
    }

    public void updateNeighbor() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) updateRender();
        worldObj.notifyBlockChange(xCoord, yCoord, zCoord, LittleTiles.blockTile);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        double renderDistance = 0;
        for (LittleTile tile : tiles) {
            renderDistance = Math.max(renderDistance, tile.getMaxRenderDistanceSquared());
        }
        return renderDistance;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double minX = xCoord;
        double minY = yCoord;
        double minZ = zCoord;
        double maxX = xCoord + 1;
        double maxY = yCoord + 1;
        double maxZ = zCoord + 1;
        List<LittleTile> snapshot;
        synchronized (tiles) {
            snapshot = new ArrayList<>(tiles);
        }
        for (LittleTile tile : snapshot) {
            AxisAlignedBB box = tile.getRenderBoundingBox();
            minX = Math.min(box.minX, minX);
            minY = Math.min(box.minY, minY);
            minZ = Math.min(box.minZ, minZ);
            maxX = Math.max(box.maxX, maxX);
            maxY = Math.max(box.maxY, maxY);
            maxZ = Math.max(box.maxZ, maxZ);
        }
        return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Used for **/
    public LittleTile loadedTile = null;

    /** Used for placing a tile and can be used if a "cable" can connect to a direction */
    public boolean isSpaceForLittleTile(CubeObject cube) {
        return isSpaceForLittleTile(cube.getAxis());
    }

    /** Used for placing a tile and can be used if a "cable" can connect to a direction */
    public boolean isSpaceForLittleTile(AxisAlignedBB alignedBB, LittleTile ignoreTile) {
        for (LittleTile tile : tiles) {
            if (tile.boundingBox != null) {
                if (ignoreTile != tile && alignedBB.intersectsWith(tile.boundingBox.getBox())) return false;
            }

        }
        return true;
    }

    public boolean isSpaceForLittleTile(LittleTileBox boxNewTile, LittleTileCutoutInfo cutoutNewTile) {
        AxisAlignedBB aabbNewTile = boxNewTile.getBox();
        Mesh3d meshNewTile = null;
        if (!tiles.isEmpty() && cutoutNewTile != null) {
            meshNewTile = Mesh3dUtil.meshFromTile(boxNewTile, cutoutNewTile);
        }

        for (LittleTile tile : tiles) {
            // read once: both can be reassigned while this runs, and the mesh has to describe the box it is tested
            // against rather than a later revision of it
            LittleTileBox boxOldTile = tile.boundingBox;
            LittleTileCutoutInfo cutoutOldTile = tile.getCutoutInfo();
            if (boxOldTile == null) {
                continue;
            }

            // Skip all checks if bounding boxes don't even collide
            if (!aabbNewTile.intersectsWith(boxOldTile.getBox())) {
                continue;
            }

            Mesh3d meshOldTile = cutoutOldTile == null ? null : Mesh3dUtil.meshFromTile(boxOldTile, cutoutOldTile);

            if (meshOldTile == null) {
                if (meshNewTile == null) {
                    // Normal box-box collision
                    return false;
                } else {
                    // Special box-mesh collision
                    if (TriangleBoundingBoxIntersect.intersect(meshNewTile, boxOldTile)) {
                        return false;
                    }
                }
            } else {
                if (meshNewTile == null) {
                    // Special mesh-box collision
                    if (TriangleBoundingBoxIntersect.intersect(meshOldTile, boxNewTile)) {
                        return false;
                    }
                } else {
                    // Special mesh-mesh collision
                    if (TriangleTriangleIntersect.meshIntersection(meshNewTile, meshOldTile)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /** Used for placing a tile and can be used if a "cable" can connect to a direction */
    public boolean isSpaceForLittleTile(AxisAlignedBB alignedBB) {
        return isSpaceForLittleTile(alignedBB, null);
    }

    public boolean isSpaceForLittleTile(LittleTileBox box) {
        return isSpaceForLittleTile(box.getBox());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (tiles != null) tiles.clear();
        tiles = createTileList();
        int count = nbt.getInteger("tilesCount");
        for (int i = 0; i < count; i++) {
            NBTTagCompound tileNBT = nbt.getCompoundTag("t" + i);
            LittleTile tile = LittleTile.CreateandLoadTile(this, worldObj, tileNBT);
            if (tile != null) tiles.add(tile);
        }
        updateTiles();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        for (int i = 0; i < tiles.size(); i++) {
            NBTTagCompound tileNBT = new NBTTagCompound();
            tiles.get(i).saveTile(tileNBT);
            nbt.setTag("t" + i, tileNBT);
        }
        nbt.setInteger("tilesCount", tiles.size());
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < tiles.size(); i++) {
            NBTTagCompound tileNBT = new NBTTagCompound();
            NBTTagCompound packet = new NBTTagCompound();
            tiles.get(i).saveTile(tileNBT);
            tiles.get(i).updatePacket(packet);
            tileNBT.setTag("update", packet);
            nbt.setTag("t" + i, tileNBT);
        }
        nbt.setInteger("tilesCount", tiles.size());
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, nbt);
    }

    public LittleTile getTile(LittleTileVec vec) {
        return getTile((byte) vec.x, (byte) vec.y, (byte) vec.z);
    }

    public LittleTile getTile(byte minX, byte minY, byte minZ) {
        for (LittleTile tile : tiles) {
            if (tile.cornerVec.x == minX && tile.cornerVec.y == minY && tile.cornerVec.z == minZ) return tile;
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        tiles.clear();
        int count = pkt.func_148857_g().getInteger("tilesCount");
        for (int i = 0; i < count; i++) {
            NBTTagCompound tileNBT = pkt.func_148857_g().getCompoundTag("t" + i);
            LittleTile tile = LittleTile.CreateandLoadTile(this, worldObj, tileNBT);
            if (tile != null) tiles.add(tile);
        }
        updateTiles();
    }

    public MovingObjectPosition getMoving(EntityPlayer player) {
        return getMoving(player, false);
    }

    public MovingObjectPosition getMoving(EntityPlayer player, boolean loadTile) {
        MovingObjectPosition hit = null;

        Vec3 pos = player.getPosition(1);
        double d0 = player.capabilities.isCreativeMode ? 5.0F : 4.5F;
        Vec3 look = player.getLook(1.0F);
        Vec3 vec32 = pos.addVector(look.xCoord * d0, look.yCoord * d0, look.zCoord * d0);
        return getMoving(pos, vec32, loadTile);
    }

    public MovingObjectPosition getMoving(Vec3 pos, Vec3 look, boolean loadTile) {
        MovingObjectPosition hit = null;
        float EPSILON = 0.0001f; // Need to check overlapping tiles with exactly the same box distance too.
        float lastCutoutDistance = Float.POSITIVE_INFINITY;
        for (LittleTile tile : tiles) {
            if (tile.boundingBox != null) {
                MovingObjectPosition Temphit = tile.boundingBox.getBox().getOffsetBoundingBox(xCoord, yCoord, zCoord)
                        .calculateIntercept(pos, look);
                if (Temphit != null) {
                    if (hit == null || hit.hitVec.distanceTo(pos) > Temphit.hitVec.distanceTo(pos) - EPSILON) {
                        boolean isHit = true;
                        if (tile.getCutoutInfo() != null) {
                            // Resolved once. Asking a second time would be a different question: the tile can drop
                            // its mesh in between, and the answer here decides whether the ray hit at all.
                            Mesh3d mesh = tile.getSimpleMesh();
                            // A tile with no usable mesh is drawn as its plain box, so the ray has to hit it as one.
                            // That is what the zero distance does - it is the nearest a cutout hit can be.
                            float distance = 0;
                            if (mesh != null && !mesh.getTriangles().isEmpty()) {
                                distance = TriangleRayIntersect.intersects(mesh, xCoord, yCoord, zCoord, pos, look);
                            }
                            isHit = distance < lastCutoutDistance;
                            if (isHit) {
                                lastCutoutDistance = distance;
                            }
                        }
                        if (isHit) {
                            hit = Temphit;
                            if (loadTile) loadedTile = tile;
                        }
                    }
                }
            }
        }
        return hit;
    }

    public boolean updateLoadedTile(EntityPlayer player) {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) return false;
        loadedTile = null;
        getMoving(player, true);
        return loadedTile != null;
    }

    public boolean updateLoadedTileServer(Vec3 pos, Vec3 look) {
        loadedTile = null;
        getMoving(pos, look, true);
        return loadedTile != null;
    }

    @SideOnly(Side.CLIENT)
    public void checkClientLoadedTile(double distance) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 pos = mc.thePlayer.getPosition(1);
        if (mc.objectMouseOver.hitVec.distanceTo(pos) < distance) loadedTile = null;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        if (super.shouldRenderInPass(pass)) {
            return customRenderingTiles.size() > 0;
        }
        return false;
    }

    public void update() {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) updateRender();

        worldObj.markTileEntityChunkModified(this.xCoord, this.yCoord, this.zCoord, this);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @SideOnly(Side.CLIENT)
    public void updateRender() {
        invalidateCutCaches();
        needsLightUpdate = true;
        invalidateNeighbourRender();
    }

    @SideOnly(Side.CLIENT)
    private void invalidateNeighbourRender() {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity neighbour = worldObj
                    .getTileEntity(xCoord + side.offsetX, yCoord + side.offsetY, zCoord + side.offsetZ);
            if (neighbour instanceof TileEntityLittleTiles) {
                ((TileEntityLittleTiles) neighbour).invalidateCutCaches();
            }
        }
        worldObj.markBlockRangeForRenderUpdate(xCoord - 1, yCoord - 1, zCoord - 1, xCoord + 1, yCoord + 1, zCoord + 1);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null && FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            invalidateNeighbourRender();
        }
    }

    @SideOnly(Side.CLIENT)
    private void invalidateCutCaches() {
        synchronized (tiles) {
            for (LittleTile tile : tiles) {
                tile.invalidateClientCutCache();
            }
        }
    }

    public ChunkCoordinates getCoord() {
        return new ChunkCoordinates(xCoord, yCoord, zCoord);
    }

    public void combineTiles(LittleStructure structure) {
        int size = 0;
        while (size != tiles.size()) {
            size = tiles.size();
            int i = 0;
            while (i < tiles.size()) {
                if (tiles.get(i).structure != structure) {
                    i++;
                    continue;
                }

                int j = 0;

                while (j < tiles.size()) {
                    if (tiles.get(j).structure != structure) {
                        j++;
                        continue;
                    }

                    if (i != j && tiles.get(i).boundingBox != null
                            && tiles.get(j).boundingBox != null
                            && tiles.get(i).canBeCombined(tiles.get(j))
                            && tiles.get(j).canBeCombined(tiles.get(i))) {
                        LittleTileBox box = tiles.get(i).boundingBox.combineBoxes(tiles.get(j).boundingBox);
                        if (box != null) {
                            tiles.get(i).boundingBox = box;
                            tiles.get(i).combineTiles(tiles.get(j));
                            tiles.get(i).updateCorner();
                            tiles.remove(j);
                            if (i > j) i--;
                            continue;
                        }
                    }
                    j++;
                }
                i++;
            }
        }
        update();
    }

    public void combineTiles() {
        int size = 0;
        while (size != tiles.size()) {
            size = tiles.size();
            int i = 0;
            while (i < tiles.size()) {
                int j = 0;
                while (j < tiles.size()) {
                    if (i != j && tiles.get(i).boundingBox != null
                            && tiles.get(j).boundingBox != null
                            && tiles.get(i).canBeCombined(tiles.get(j))
                            && tiles.get(j).canBeCombined(tiles.get(i))) {
                        LittleTileBox box = tiles.get(i).boundingBox.combineBoxes(tiles.get(j).boundingBox);
                        if (box != null) {
                            tiles.get(i).boundingBox = box;
                            tiles.get(i).combineTiles(tiles.get(j));
                            tiles.get(i).updateCorner();
                            tiles.remove(j);
                            if (i > j) i--;
                            continue;
                        }
                    }
                    j++;
                }
                i++;
            }
        }
        update();
    }

    private boolean first = true;
    private int lastMaxLightValue;

    public int getMaxLightValue() {
        if (!needsLightUpdate) {
            return lastMaxLightValue;
        }
        if (!first) return 0;
        int light = 0;
        for (LittleTile tile : getTiles()) {
            first = false;
            int tempLight = tile.getLightValue(worldObj, xCoord, yCoord, zCoord);
            first = true;
            if (tempLight > light) light = tempLight;
        }
        lastMaxLightValue = light;
        needsLightUpdate = false;
        return light;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }
}
