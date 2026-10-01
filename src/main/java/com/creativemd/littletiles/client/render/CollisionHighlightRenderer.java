package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CollisionHighlightRenderer {

    private static final int RADIUS_CHUNKS = 2;
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final double EPSILON = 0.002;
    private static final float MIN_ALPHA = 0.4F;
    private static final float MAX_ALPHA = 0.7F;

    private final List<AxisAlignedBB> cachedBoxes = new ArrayList<>();
    private final List<PlacedMesh> cachedMeshes = new ArrayList<>();
    private final WeakHashMap<LittleTile, TileCache> tileCache = new WeakHashMap<>();
    private int ticksSinceRefresh = REFRESH_INTERVAL_TICKS;

    private static final class TileCache {

        final List<AxisAlignedBB> boxes = new ArrayList<>();
        final List<PlacedMesh> meshes = new ArrayList<>();
    }

    /** A mesh relative to its block, along with where that block is. */
    private static final class PlacedMesh {

        final Mesh3d mesh;
        final int x, y, z;

        PlacedMesh(Mesh3d mesh, int x, int y, int z) {
            this.mesh = mesh;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void emitVertex(Vector3d point, double camX, double camY, double camZ) {
            GL11.glVertex3d(x + point.x - camX, y + point.y - camY, z + point.z - camZ);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        final ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != LittleTiles.collisionTool) {
            cachedBoxes.clear();
            cachedMeshes.clear();
            tileCache.clear();
            ticksSinceRefresh = REFRESH_INTERVAL_TICKS;
            return;
        }

        if (++ticksSinceRefresh >= REFRESH_INTERVAL_TICKS) {
            refreshCache(player);
            ticksSinceRefresh = 0;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (cachedBoxes.isEmpty() && cachedMeshes.isEmpty()) return;

        final double camX = TileEntityRendererDispatcher.staticPlayerX;
        final double camY = TileEntityRendererDispatcher.staticPlayerY;
        final double camZ = TileEntityRendererDispatcher.staticPlayerZ;

        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        float alpha = MIN_ALPHA
                + (float) ((Math.sin(System.nanoTime() / 350000000D) + 1D) * 0.5D * (MAX_ALPHA - MIN_ALPHA));
        GL11.glColor4f(0.3F, 0.6F, 1.0F, alpha);

        GL11.glBegin(GL11.GL_QUADS);
        for (AxisAlignedBB b : cachedBoxes) {
            emitBoxFaces(
                    b.minX - camX - EPSILON,
                    b.minY - camY - EPSILON,
                    b.minZ - camZ - EPSILON,
                    b.maxX - camX + EPSILON,
                    b.maxY - camY + EPSILON,
                    b.maxZ - camZ + EPSILON);
        }
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLES);
        for (PlacedMesh placed : cachedMeshes) {
            for (Triangle3d t : placed.mesh.getTriangles()) {
                placed.emitVertex(t.getP1(), camX, camY, camZ);
                placed.emitVertex(t.getP2(), camX, camY, camZ);
                placed.emitVertex(t.getP3(), camX, camY, camZ);
            }
        }
        GL11.glEnd();

        GL11.glPopAttrib();
    }

    private void refreshCache(EntityPlayer player) {
        cachedBoxes.clear();
        cachedMeshes.clear();

        final World world = player.worldObj;
        final int centerChunkX = MathHelper.floor_double(player.posX) >> 4;
        final int centerChunkZ = MathHelper.floor_double(player.posZ) >> 4;

        for (int cx = centerChunkX - RADIUS_CHUNKS; cx <= centerChunkX + RADIUS_CHUNKS; cx++) {
            for (int cz = centerChunkZ - RADIUS_CHUNKS; cz <= centerChunkZ + RADIUS_CHUNKS; cz++) {
                if (!world.getChunkProvider().chunkExists(cx, cz)) continue;
                final Chunk chunk = world.getChunkFromChunkCoords(cx, cz);

                @SuppressWarnings("unchecked")
                final Iterable<TileEntity> tes = chunk.chunkTileEntityMap.values();
                for (TileEntity te : tes) {
                    if (te.isInvalid() || !(te instanceof TileEntityLittleTiles)) continue;

                    final List<LittleTile> tiles = ((TileEntityLittleTiles) te).getTiles();
                    final List<LittleTile> snapshot;
                    synchronized (tiles) {
                        snapshot = new ArrayList<>(tiles);
                    }
                    for (LittleTile tile : snapshot) {
                        if (!tile.disableCollision) continue;
                        TileCache cached = tileCache.get(tile);
                        if (cached == null) {
                            cached = new TileCache();
                            for (LittleTilesCubeObject cube : tile.getRenderingCubes()) {
                                if (cube.cutoutInfo == null) {
                                    cached.boxes.add(cube.getAxis().offset(te.xCoord, te.yCoord, te.zCoord));
                                } else {
                                    Mesh3d mesh = Mesh3dUtil.createMesh(
                                            cube.cutoutInfo,
                                            cube.minX,
                                            cube.minY,
                                            cube.minZ,
                                            cube.maxX,
                                            cube.maxY,
                                            cube.maxZ,
                                            cube.block,
                                            cube.meta);
                                    for (Triangle3d t : mesh.getTriangles()) t.inflate(EPSILON);
                                    cached.meshes.add(new PlacedMesh(mesh, te.xCoord, te.yCoord, te.zCoord));
                                }
                            }
                            tileCache.put(tile, cached);
                        }
                        cachedBoxes.addAll(cached.boxes);
                        cachedMeshes.addAll(cached.meshes);
                    }
                }
            }
        }
    }

    private static void emitBoxFaces(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        GL11.glVertex3d(minX, minY, minZ);
        GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(minX, minY, maxZ);

        GL11.glVertex3d(minX, maxY, minZ);
        GL11.glVertex3d(minX, maxY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(maxX, maxY, minZ);

        GL11.glVertex3d(minX, minY, minZ);
        GL11.glVertex3d(minX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, minY, minZ);

        GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ);

        GL11.glVertex3d(minX, minY, minZ);
        GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ);
        GL11.glVertex3d(minX, maxY, minZ);

        GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, maxZ);
        GL11.glVertex3d(maxX, minY, maxZ);
    }
}
