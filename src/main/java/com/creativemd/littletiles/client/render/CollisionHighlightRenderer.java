package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CollisionHighlightRenderer {

    private static final int RADIUS_CHUNKS = 2;
    private static final int REFRESH_INTERVAL_TICKS = 5;
    private static final double EPSILON = 0.002;

    private final List<double[]> cachedBoxes = new ArrayList<>();
    private int ticksSinceRefresh = REFRESH_INTERVAL_TICKS;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;
        if (player == null || player.worldObj == null) return;

        final ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != LittleTiles.collisionTool) {
            cachedBoxes.clear();
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
        if (cachedBoxes.isEmpty()) return;

        final double camX = TileEntityRendererDispatcher.staticPlayerX;
        final double camY = TileEntityRendererDispatcher.staticPlayerY;
        final double camZ = TileEntityRendererDispatcher.staticPlayerZ;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glColor4f(0.3F, 0.6F, 1.0F, 0.35F);

        GL11.glBegin(GL11.GL_QUADS);
        for (double[] b : cachedBoxes) {
            emitBoxFaces(
                b[0] - camX - EPSILON, b[1] - camY - EPSILON, b[2] - camZ - EPSILON,
                b[3] - camX + EPSILON, b[4] - camY + EPSILON, b[5] - camZ + EPSILON);
        }
        GL11.glEnd();

        GL11.glPopAttrib();
    }

    private void refreshCache(EntityPlayer player) {
        cachedBoxes.clear();

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
                    if (!(te instanceof TileEntityLittleTiles)) continue;

                    final List<LittleTile> tiles = ((TileEntityLittleTiles) te).getTiles();
                    final List<LittleTile> snapshot;
                    synchronized (tiles) {
                        snapshot = new ArrayList<>(tiles);
                    }
                    for (LittleTile tile : snapshot) {
                        if (!tile.disableCollision || tile.boundingBox == null) continue;
                        cachedBoxes.add(new double[] {
                            te.xCoord + tile.boundingBox.minX / 16D,
                            te.yCoord + tile.boundingBox.minY / 16D,
                            te.zCoord + tile.boundingBox.minZ / 16D,
                            te.xCoord + tile.boundingBox.maxX / 16D,
                            te.yCoord + tile.boundingBox.maxY / 16D,
                            te.zCoord + tile.boundingBox.maxZ / 16D });
                    }
                }
            }
        }
    }

    private static void emitBoxFaces(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        GL11.glVertex3d(minX, minY, minZ); GL11.glVertex3d(maxX, minY, minZ);
        GL11.glVertex3d(maxX, minY, maxZ); GL11.glVertex3d(minX, minY, maxZ);

        GL11.glVertex3d(minX, maxY, minZ); GL11.glVertex3d(minX, maxY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ); GL11.glVertex3d(maxX, maxY, minZ);

        GL11.glVertex3d(minX, minY, minZ); GL11.glVertex3d(minX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, minZ); GL11.glVertex3d(maxX, minY, minZ);

        GL11.glVertex3d(minX, minY, maxZ); GL11.glVertex3d(maxX, minY, maxZ);
        GL11.glVertex3d(maxX, maxY, maxZ); GL11.glVertex3d(minX, maxY, maxZ);

        GL11.glVertex3d(minX, minY, minZ); GL11.glVertex3d(minX, minY, maxZ);
        GL11.glVertex3d(minX, maxY, maxZ); GL11.glVertex3d(minX, maxY, minZ);

        GL11.glVertex3d(maxX, minY, minZ); GL11.glVertex3d(maxX, maxY, minZ);
        GL11.glVertex3d(maxX, maxY, maxZ); GL11.glVertex3d(maxX, minY, maxZ);
    }
}
