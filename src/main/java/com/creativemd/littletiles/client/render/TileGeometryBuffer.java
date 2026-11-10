package com.creativemd.littletiles.client.render;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Textured geometry of tiles, uploaded once into a vertex buffer, in the coordinates of their cubes. The geometry
 * {@link LittleTilesBlockRenderHelper#renderInventoryCubes} draws.
 * <p>
 * Items share one buffer between their inventory icon and their placement preview in the world, see {@link #get}.
 * Owns a GL buffer, call {@link #delete()} once it is no longer used. Render thread only.
 */
@SideOnly(Side.CLIENT)
public class TileGeometryBuffer {

    // position (3 floats), texture (2 floats), color (4 unsigned bytes), normal (3 floats)
    private static final int TEXTURE_OFFSET = 12;
    private static final int COLOR_OFFSET = 20;
    private static final int NORMAL_OFFSET = 24;
    private static final int STRIDE = 36;

    private static final int CACHE_SIZE = 256;

    /**
     * Keyed by the NBT of the item, which holds its tiles, so changing it in place - rotating a recipe, for example -
     * simply misses. A single segment, so the eviction is least recently used across all entries.
     */
    private static final Cache<NBTTagCompound, TileGeometryBuffer> cache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .concurrencyLevel(1)
            .removalListener((RemovalListener<NBTTagCompound, TileGeometryBuffer>) removal -> removal.getValue().delete())
            .build();

    private int buffer;
    private final int vertices;
    private final double largestSide;
    private final Vec3 min;

    /**
     * The buffer of the item, built if needed, or null if it has no NBT and so no tiles. Its geometry is that of
     * {@link ITilesRenderer#getRenderingCubes}.
     */
    public static TileGeometryBuffer get(ItemStack stack) {
        if (stack.stackTagCompound == null) return null;
        TileGeometryBuffer buffer = cache.getIfPresent(stack.stackTagCompound);
        if (buffer == null) {
            buffer = new TileGeometryBuffer(
                    ((ITilesRenderer) stack.getItem()).getRenderingCubes(stack),
                    Block.getBlockFromItem(stack.getItem()));
            cache.put((NBTTagCompound) stack.stackTagCompound.copy(), buffer);
        }
        return buffer;
    }

    /** Drops every buffer, needed when the texture atlas changes. */
    public static void clearCache() {
        cache.invalidateAll();
    }

    public TileGeometryBuffer(ArrayList<CubeObject> cubes, Block parBlock) {
        Vec3 size = CubeObject.getSizeOfCubes(cubes);
        largestSide = Math.max(size.xCoord, Math.max(size.yCoord, size.zCoord));
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        for (CubeObject cube : cubes) {
            minX = Math.min(minX, cube.minX);
            minY = Math.min(minY, cube.minY);
            minZ = Math.min(minZ, cube.minZ);
        }
        min = cubes.isEmpty() ? Vec3.createVectorHelper(0, 0, 0) : Vec3.createVectorHelper(minX, minY, minZ);

        List<Part> parts = new ArrayList<>();
        for (CubeObject cube : cubes) {
            int metadata = cube.meta != -1 ? cube.meta : 0;
            Block block = parBlock == null || parBlock instanceof BlockAir ? Blocks.stone : parBlock;
            if (cube.block != null && !(cube.block instanceof BlockAir)) block = cube.block;
            int color = LittleTilesBlockRenderHelper.resolveRenderColor(cube.color, block, metadata);
            boolean topTintOnly = LittleTilesBlockRenderHelper.tintsTopFaceOnly(block);

            Mesh3d mesh = null;
            if (cube instanceof LittleTilesCubeObject) {
                LittleTilesCubeObject littleCube = (LittleTilesCubeObject) cube;
                mesh = littleCube.geometryCache == null ? null : littleCube.geometryCache.getOrCreateSimpleMesh();
            }
            if (mesh == null) {
                mesh = Mesh3dUtil.createBoxMesh();
                mesh.scale(new Vector3d(cube.maxX - cube.minX, cube.maxY - cube.minY, cube.maxZ - cube.minZ));
                mesh.translate(new Vector3d(cube.minX, cube.minY, cube.minZ));
            } else {
                // cached on the tile, must not be textured in place
                mesh = mesh.copy();
            }
            // Recipe geometry retains its multi-block position (for example, x = 1..2 for a tile in the second block).
            // Texture projection expects block-local coordinates, recipe-space values would sample beyond the icon
            // into unrelated atlas sprites.
            Vector3d textureOffset = new Vector3d(Math.floor(cube.minX), Math.floor(cube.minY), Math.floor(cube.minZ));
            mesh.translate(new Vector3d(-textureOffset.x, -textureOffset.y, -textureOffset.z));
            mesh.setTextures(block, metadata);
            mesh.translate(textureOffset);

            parts.add(new Part(mesh, color, topTintOnly));
        }

        vertices = countVertices(parts);
        ByteBuffer data = BufferUtils.createByteBuffer(vertices * STRIDE);
        putParts(data, parts);
        data.flip();

        buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private static int countVertices(List<Part> parts) {
        int count = 0;
        for (Part part : parts) count += part.mesh.getTriangles().size() * 3;
        return count;
    }

    private static void putParts(ByteBuffer data, List<Part> parts) {
        for (Part part : parts) {
            for (Triangle3d triangle : part.mesh.getTriangles()) {
                int color = part.topTintOnly && triangle.getFaceDirection() != ForgeDirection.UP ? ColorUtils.WHITE
                        : part.color;
                Vector3d normal = triangle.getNormal();
                putVertex(data, triangle.getP1(), triangle.getTex1().x, triangle.getTex1().y, color, normal);
                putVertex(data, triangle.getP2(), triangle.getTex2().x, triangle.getTex2().y, color, normal);
                putVertex(data, triangle.getP3(), triangle.getTex3().x, triangle.getTex3().y, color, normal);
            }
        }
    }

    private static void putVertex(ByteBuffer data, Vector3d position, double u, double v, int color,
            Vector3d normal) {
        data.putFloat((float) position.x);
        data.putFloat((float) position.y);
        data.putFloat((float) position.z);
        data.putFloat((float) u);
        data.putFloat((float) v);
        data.put((byte) (color >> 16 & 255));
        data.put((byte) (color >> 8 & 255));
        data.put((byte) (color & 255));
        data.put((byte) 255);
        data.putFloat((float) normal.x);
        data.putFloat((float) normal.y);
        data.putFloat((float) normal.z);
    }

    /** Side length of the cube the geometry fits into, to scale it down to a single block. */
    public double getLargestSide() {
        return largestSide;
    }

    /** The lowest corner of all cubes. */
    public Vec3 getMin() {
        return min;
    }

    public void render() {
        if (buffer == 0 || vertices == 0) return;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE, 0);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE, TEXTURE_OFFSET);
        GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, STRIDE, COLOR_OFFSET);
        GL11.glNormalPointer(GL11.GL_FLOAT, STRIDE, NORMAL_OFFSET);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices);

        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        // the current color is undefined after drawing with a color array
        GL11.glColor4f(1, 1, 1, 1);
    }

    public void delete() {
        if (buffer != 0) GL15.glDeleteBuffers(buffer);
        buffer = 0;
    }

    private static class Part {

        private final Mesh3d mesh;
        private final int color;
        private final boolean topTintOnly;

        private Part(Mesh3d mesh, int color, boolean topTintOnly) {
            this.mesh = mesh;
            this.color = color;
            this.topTintOnly = topTintOnly;
        }
    }
}
