package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.tileentity.TileEntity;

import org.joml.Vector2d;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.creativecore.client.rendering.ExtendedRenderBlocks;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleTilesBlockRenderHelper {

    private static final ThreadLocal<ExtendedRenderBlocks> extraRendererThreadLocal = ThreadLocal
            .withInitial(ExtendedRenderBlocks::new);

    public static void renderMesh(double x, double y, double z, Vector3d cutoutScale, int orientation, double red,
            double green, double blue, double alpha, Vector3i posCutout, Vector3i posSubMin, Vector3i posSubMax,
            LittleTileShapeMode shapeMode) {
        LittleTileCutoutInfo cutoutInfo = new LittleTileCutoutInfo();
        cutoutInfo.type = shapeMode;
        cutoutInfo.size = new Vector3i(
                (int) Math.round(cutoutScale.x * 16),
                (int) Math.round(cutoutScale.y * 16),
                (int) Math.round(cutoutScale.z * 16));
        Mesh3d mesh = Mesh3dUtil.createMesh(
                cutoutInfo,
                cutoutScale,
                new Vector3d(),
                posCutout,
                posSubMin,
                posSubMax,
                null,
                0,
                orientation);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glColor4d(red, green, blue, alpha);

        for (Triangle3d triangle : mesh.getTriangles()) {
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex3d(triangle.getP1().x, triangle.getP1().y, triangle.getP1().z);
            GL11.glVertex3d(triangle.getP2().x, triangle.getP2().y, triangle.getP2().z);
            GL11.glVertex3d(triangle.getP3().x, triangle.getP3().y, triangle.getP3().z);
            GL11.glEnd();
        }

        GL11.glPopMatrix();
    }

    private static boolean renderCutout(int x, int y, int z, LittleTilesCubeObject cube, IBlockAccess world) {
        Mesh3d mesh = Mesh3dUtil.createMesh(
                x,
                y,
                z,
                cube.cutoutInfo,
                cube.minX,
                cube.minY,
                cube.minZ,
                cube.maxX,
                cube.maxY,
                cube.maxZ,
                cube.block,
                cube.meta);
        Tessellator tess = Tessellator.instance;

        int brightness = cube.block.getMixedBrightnessForBlock(world, x, y, z);
        tess.setBrightness(brightness);
        for (Triangle3d triangle : mesh.getTriangles()) {
            Vector3d p1 = triangle.getP1();
            Vector3d p2 = triangle.getP2();
            Vector3d p3 = triangle.getP3();
            Vector2d tex1 = triangle.getTex1();
            Vector2d tex2 = triangle.getTex2();
            Vector2d tex3 = triangle.getTex3();
            tess.setColorOpaque_F(1, 1, 1);
            tess.addVertexWithUV(p1.x, p1.y, p1.z, tex1.x, tex1.y);
            tess.addVertexWithUV(p2.x, p2.y, p2.z, tex2.x, tex2.y);
            tess.addVertexWithUV(p3.x, p3.y, p3.z, tex3.x, tex3.y);
            tess.addVertexWithUV(p3.x, p3.y, p3.z, tex3.x, tex3.y);
        }
        return !mesh.getTriangles().isEmpty();
    }

    private static void addNeighbouringCubes(IBlockAccess world, ArrayList<ArrayList<LittleTilesCubeObject>> neighbours, int x, int y, int z, int dx, int dy, int dz)
    {
        TileEntity tileEntity = world.getTileEntity(x + dx, y + dy, z + dz);
        if (tileEntity instanceof TileEntityLittleTiles) {
            TileEntityLittleTiles little = (TileEntityLittleTiles) tileEntity;
            List<LittleTile> tiles = little.getTiles();
            List<LittleTile> snapshot;
            synchronized (tiles) {
                snapshot = new ArrayList<>(tiles);
            }
            for (LittleTile tile : snapshot) {
                ArrayList<LittleTilesCubeObject> neighbourCubeObjects = new ArrayList<>();
                neighbourCubeObjects = tile.getRenderingCubes();
                // Ignore any neighbouring cubes that are not adjacent to the block border
                for (LittleTilesCubeObject cube : neighbourCubeObjects) {
                    if (dx == 1)
                    {
                        if (cube.gridMinX != 0) continue;
                        neighbours.get(ForgeDirection.EAST.ordinal()).addAll(neighbourCubeObjects);
                    }
                    else if (dx == -1)
                    {
                        if (cube.gridMaxX != 16) continue;
                        neighbours.get(ForgeDirection.WEST.ordinal()).addAll(neighbourCubeObjects);
                    }
                    else if (dy == 1)
                    {
                        if (cube.gridMinY != 0) continue;
                        neighbours.get(ForgeDirection.UP.ordinal()).addAll(neighbourCubeObjects);
                    }
                    else if (dy == -1)
                    {
                        if (cube.gridMaxY != 16) continue;
                        neighbours.get(ForgeDirection.DOWN.ordinal()).addAll(neighbourCubeObjects);
                    }
                    else if (dz == 1)
                    {
                        if (cube.gridMinZ != 0) continue;
                        neighbours.get(ForgeDirection.SOUTH.ordinal()).addAll(neighbourCubeObjects);
                    }
                    else if (dz == -1)
                    {
                        if (cube.gridMaxZ != 16) continue;
                        neighbours.get(ForgeDirection.NORTH.ordinal()).addAll(neighbourCubeObjects);
                    }
                }
            }
        }
    }

    public static boolean renderCubes(IBlockAccess world, ArrayList<LittleTilesCubeObject> cubes, int x, int y, int z,
            Block block, RenderBlocks renderer, ForgeDirection direction) {

        final ExtendedRenderBlocks extraRenderer = extraRendererThreadLocal.get();
        extraRenderer.updateRenderer(renderer);

        final IBlockAccessFake fake = (IBlockAccessFake) extraRenderer.blockAccess;
        fake.world = renderer.blockAccess;

        int pass = ForgeHooksClient.getWorldRenderPass();
        boolean rendered = false;

        ArrayList<ArrayList<LittleTilesCubeObject>> neighbours = new ArrayList<ArrayList<LittleTilesCubeObject>>(6);
        for (int i = 0; i < 6; i++)
        {
            neighbours.add(i, new ArrayList<>());
        }
        addNeighbouringCubes(world, neighbours, x, y, z, 1, 0, 0);
        addNeighbouringCubes(world, neighbours, x, y, z, -1, 0, 0);
        addNeighbouringCubes(world, neighbours, x, y, z, 0, 1, 0);
        addNeighbouringCubes(world, neighbours, x, y, z, 0, -1, 0);
        addNeighbouringCubes(world, neighbours, x, y, z, 0, 0, 1);
        addNeighbouringCubes(world, neighbours, x, y, z, 0, 0, -1);

        LittleTilesFaceCuller.computeHiddenSides(cubes, neighbours);

        try {
            for (int i = 0; i < cubes.size(); i++) {
                final LittleTilesCubeObject cube = cubes.get(i);
                if (!cube.block.canRenderInPass(pass)) {
                    continue;
                }
                rendered = true;

                if (cube.cutoutInfo != null) {
                    if (renderCutout(x, y, z, cube, world)) {
                        continue;
                    }
                    // For buggy meshes, render the default cube
                }

                if (cube.block != null && cube.meta != -1) {
                    extraRenderer.clearOverrideBlockTexture();
                    extraRenderer.setRenderBounds(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
                    extraRenderer.meta = cube.meta;
                    fake.overrideMeta = cube.meta;
                    extraRenderer.color = cube.color;
                    extraRenderer.hiddenSides = cube.hiddenSides;
                    extraRenderer.lockBlockBounds = true;
                    if (LittleTiles.angelicaCompat != null) {
                        LittleTiles.angelicaCompat.setShaderMaterialOverride(cube.block, cube.meta);
                    }
                    extraRenderer.field_152631_f = true;
                    extraRenderer.renderBlockAllFaces(cube.block, x, y, z);
                    extraRenderer.field_152631_f = false;
                    if (LittleTiles.angelicaCompat != null) {
                        LittleTiles.angelicaCompat.resetShaderMaterialOverride();
                    }
                    extraRenderer.lockBlockBounds = false;
                    extraRenderer.color = ColorUtils.WHITE;
                }
            }
        } finally {
            extraRenderer.hiddenSides = 0;
            fake.world = null;
        }
        return rendered;
    }

    public static void renderInventoryCubes(RenderBlocks renderer, ArrayList<CubeObject> cubes, Block parBlock,
            int meta) {
        Tessellator tesselator = Tessellator.instance;
        for (int i = 0; i < cubes.size(); i++) {
            final CubeObject cube = cubes.get(i);
            int metadata = 0;
            if (cube.meta != -1) metadata = cube.meta;
            Block block = parBlock;
            if (block instanceof BlockAir) block = Blocks.stone;
            renderer.setRenderBounds(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
            if (cube.block != null && !(cube.block instanceof BlockAir)) {
                block = cube.block;
                meta = 0;
            }

            int j = block.getRenderColor(metadata);
            if (cube.color != ColorUtils.WHITE) j = cube.color;

            float f1 = (float) (j >> 16 & 255) / 255.0F;
            float f2 = (float) (j >> 8 & 255) / 255.0F;
            float f3 = (float) (j & 255) / 255.0F;
            float brightness = 1.0F;
            GL11.glColor4f(f1 * brightness, f2 * brightness, f3 * brightness, 1.0F);

            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            tesselator.startDrawingQuads();
            tesselator.setNormal(0.0F, -1.0F, 0.0F);
            renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(0, metadata));
            tesselator.draw();
            tesselator.startDrawingQuads();
            tesselator.setNormal(0.0F, 1.0F, 0.0F);
            renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(1, metadata));
            tesselator.draw();
            tesselator.startDrawingQuads();
            tesselator.setNormal(0.0F, 0.0F, -1.0F);
            renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(2, metadata));
            tesselator.draw();
            tesselator.startDrawingQuads();
            tesselator.setNormal(0.0F, 0.0F, 1.0F);
            renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(3, metadata));
            tesselator.draw();
            tesselator.startDrawingQuads();
            tesselator.setNormal(-1.0F, 0.0F, 0.0F);
            renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, block.getIcon(4, metadata));
            tesselator.draw();
            tesselator.startDrawingQuads();
            tesselator.setNormal(1.0F, 0.0F, 0.0F);
            renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, block.getIcon(5, metadata));
            tesselator.draw();
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        }
    }

}
