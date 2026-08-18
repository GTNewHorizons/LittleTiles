package com.creativemd.littletiles.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector2d;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.creativecore.client.rendering.ExtendedRenderBlocks;
import com.creativemd.creativecore.client.rendering.IFaceClipper;
import com.creativemd.creativecore.client.rendering.RenderHelper3D;
import com.creativemd.creativecore.common.utils.ColorUtils;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.creativecore.lib.Vector3d;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.LittleTilesFaceCuller.CullingContext;
import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.client.util3d.Mesh3dUtil;
import com.creativemd.littletiles.client.util3d.Triangle3d;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.LittleTileShapeMode;
import com.creativemd.littletiles.common.utils.LittleTilesCubeObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleTilesBlockRenderHelper {

    private static final ThreadLocal<ExtendedRenderBlocks> extraRendererThreadLocal = ThreadLocal
            .withInitial(ExtendedRenderBlocks::new);

    public static void renderShape(LittleTileShapeMode shape, double centerX, double centerY, double centerZ, Vec3 size,
            Vector3d cutoutScale, int orientation, Vector3i posCutout, Vec3 color, double alpha) {
        if (shape == LittleTileShapeMode.BOX || shape == LittleTileShapeMode.PILLAR) {
            RenderHelper3D.renderBlock(
                    centerX,
                    centerY,
                    centerZ,
                    size.xCoord,
                    size.yCoord,
                    size.zCoord,
                    0,
                    0,
                    0,
                    color.xCoord,
                    color.yCoord,
                    color.zCoord,
                    alpha);
        } else {
            if (cutoutScale == null) {
                cutoutScale = new Vector3d(size.xCoord, size.yCoord, size.zCoord);
            }
            Vector3i posSubMax = new Vector3i(
                    (int) Math.round(size.xCoord * 16),
                    (int) Math.round(size.yCoord * 16),
                    (int) Math.round(size.zCoord * 16));
            renderMesh(
                    centerX - size.xCoord / 2D,
                    centerY - size.yCoord / 2D,
                    centerZ - size.zCoord / 2D,
                    cutoutScale,
                    orientation,
                    color.xCoord,
                    color.yCoord,
                    color.zCoord,
                    alpha,
                    posCutout,
                    new Vector3i(),
                    posSubMax,
                    shape);
        }
    }

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

        GL11.glBegin(GL11.GL_TRIANGLES);
        for (Triangle3d triangle : mesh.getTriangles()) {
            GL11.glVertex3d(triangle.getP1().x, triangle.getP1().y, triangle.getP1().z);
            GL11.glVertex3d(triangle.getP2().x, triangle.getP2().y, triangle.getP2().z);
            GL11.glVertex3d(triangle.getP3().x, triangle.getP3().y, triangle.getP3().z);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    /** What became of a cutout tile. */
    private enum CutoutResult {
        /** Its mesh went into the tessellator. */
        DRAWN,
        /** Every face of it is covered - nothing to draw, and the cube must not be drawn in its place either. */
        HIDDEN,
        /** It has no usable mesh, so it falls back to being drawn as a plain cube. */
        FAILED
    }

    public static int resolveRenderColor(int cubeColor, Block block, int meta) {
        if (cubeColor != ColorUtils.WHITE) return cubeColor;
        return block.getRenderColor(meta);
    }

    private static CutoutResult renderCutout(int x, int y, int z, LittleTilesCubeObject cube, CullingContext culling,
            IBlockAccess world) {
        if (!cube.geometryCache.hasValidMesh()) {
            return CutoutResult.FAILED;
        }

        List<Triangle3d> visible = LittleTilesFaceCuller.visibleCutoutTriangles(culling, cube);
        if (visible.isEmpty()) {
            return CutoutResult.HIDDEN;
        }
        renderTriangles(x, y, z, cube, visible, world);
        return CutoutResult.DRAWN;
    }

    private static void renderTriangles(int x, int y, int z, LittleTilesCubeObject cube, List<Triangle3d> triangles,
            IBlockAccess world) {
        // cut results are cached on the tile and must not be textured or translated in place
        Mesh3d mesh = new Mesh3d(triangles).copy();
        mesh.setTextures(cube.block, cube.meta);
        mesh.translate(new Vector3d(x, y, z));
        Tessellator tess = Tessellator.instance;

        int brightness = cube.block.getMixedBrightnessForBlock(world, x, y, z);
        // Force brightness to 15 for blocks that emits.
        // This is a workaround to support blocks like Caelestis Lapis from extraUtils.
        // TODO: Investigate a better way to handle this (POC:
        // https://github.com/GTNewHorizons/LittleTiles/commits/refactor-brightness/)
        int emitted = cube.block.getLightValue(world, x, y, z);
        if (emitted > 0) brightness |= (15 << 4);
        tess.setBrightness(brightness);

        int color = resolveRenderColor(cube.color, cube.block, cube.meta);

        for (Triangle3d triangle : mesh.getTriangles()) {
            Vector3d p1 = triangle.getP1();
            Vector3d p2 = triangle.getP2();
            Vector3d p3 = triangle.getP3();
            Vector2d tex1 = triangle.getTex1();
            Vector2d tex2 = triangle.getTex2();
            Vector2d tex3 = triangle.getTex3();
            tess.setColorOpaque_I(color);
            tess.addVertexWithUV(p1.x, p1.y, p1.z, tex1.x, tex1.y);
            tess.addVertexWithUV(p2.x, p2.y, p2.z, tex2.x, tex2.y);
            tess.addVertexWithUV(p3.x, p3.y, p3.z, tex3.x, tex3.y);
            tess.addVertexWithUV(p3.x, p3.y, p3.z, tex3.x, tex3.y);
        }
    }

    /**
     * Whether any cube still has to be culled, rather than being served from its cached cut result. Gathering the
     * geometry to cull against reaches into the six neighbouring tile entities, which is not worth doing when every
     * cube of this one already knows what is visible of it.
     */
    private static boolean needsCulling(List<LittleTilesCubeObject> cubes, IFaceClipper[] coverage, int pass) {
        for (int i = 0; i < cubes.size(); i++) {
            LittleTilesCubeObject cube = cubes.get(i);
            if (!cube.block.canRenderInPass(pass)) {
                continue;
            }
            if (cube.cutoutInfo != null) {
                if (cube.geometryCache.hasValidMesh() && cube.geometryCache.getVisibleCutoutTriangles() == null) {
                    return true;
                }
            } else if (coverage[i] instanceof FaceClipper && cube.geometryCache.getBoxCullingResult() == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean renderCubes(IBlockAccess world, ArrayList<LittleTilesCubeObject> cubes, int x, int y, int z,
            Block block, RenderBlocks renderer, ForgeDirection direction) {

        final ExtendedRenderBlocks extraRenderer = extraRendererThreadLocal.get();
        extraRenderer.updateRenderer(renderer);

        final IBlockAccessFake fake = (IBlockAccessFake) extraRenderer.blockAccess;
        fake.setWorld(renderer.blockAccess, x, y, z);

        int pass = ForgeHooksClient.getWorldRenderPass();
        boolean rendered = false;

        IFaceClipper[] coverage = LittleTilesFaceCuller.computeCoverage(world, cubes, x, y, z);
        CullingContext cullingContext = needsCulling(cubes, coverage, pass)
                ? LittleTilesFaceCuller.prepareCulling(world, cubes, x, y, z)
                : null;

        try {
            for (int i = 0; i < cubes.size(); i++) {
                final LittleTilesCubeObject cube = cubes.get(i);

                if (cube.block == null || cube.meta == -1) {
                    continue;
                }
                if (!cube.block.canRenderInPass(pass)) {
                    continue;
                }

                fake.setBlock(cube.block, cube.meta);

                if (cube.cutoutInfo != null) {
                    CutoutResult result = renderCutout(x, y, z, cube, cullingContext, fake);
                    if (result == CutoutResult.DRAWN) {
                        rendered = true;
                        continue;
                    }
                    if (result == CutoutResult.HIDDEN) {
                        continue;
                    }
                    // For buggy meshes, render the default cube
                }

                // sides a mesh cuts into cannot be drawn as rectangles, the culler hands them back as triangles
                List<Triangle3d> boxTriangles = Collections.emptyList();
                if (cube.cutoutInfo == null && coverage[i] instanceof FaceClipper) {
                    FaceClipper clipper = (FaceClipper) coverage[i];
                    boxTriangles = LittleTilesFaceCuller.visibleBoxTriangles(cullingContext, cube, clipper);
                }
                rendered = true;
                extraRenderer.clearOverrideBlockTexture();
                extraRenderer.setRenderBounds(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
                extraRenderer.meta = cube.meta;
                extraRenderer.color = cube.color;
                extraRenderer.faceClipper = coverage[i];
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
                if (!boxTriangles.isEmpty()) {
                    renderTriangles(x, y, z, cube, boxTriangles, fake);
                }
            }
        } finally {
            extraRenderer.faceClipper = null;
            fake.reset();
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
            if (block == null) block = Blocks.stone;
            if (block instanceof BlockAir) block = Blocks.stone;
            renderer.setRenderBounds(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
            if (cube.block != null && !(cube.block instanceof BlockAir)) {
                block = cube.block;
                meta = 0;
            }
            int j = resolveRenderColor(cube.color, block, metadata);

            float f1 = (float) (j >> 16 & 255) / 255.0F;
            float f2 = (float) (j >> 8 & 255) / 255.0F;
            float f3 = (float) (j & 255) / 255.0F;
            float brightness = 1.0F;
            GL11.glColor4f(f1 * brightness, f2 * brightness, f3 * brightness, 1.0F);

            if (cube instanceof LittleTilesCubeObject) {
                LittleTilesCubeObject littleCube = (LittleTilesCubeObject) cube;
                Mesh3d mesh = littleCube.geometryCache == null ? null
                        : littleCube.geometryCache.getOrCreateSimpleMesh();
                if (mesh != null) {
                    mesh = mesh.copy();
                    // Recipe meshes retain their multi-block position (for example, x = 1..2 for a tile in the
                    // second block). Texture projection expects block-local coordinates; feeding those recipe-space
                    // values to IIcon interpolation samples beyond the icon and into unrelated atlas sprites.
                    Vector3d textureOffset = new Vector3d(
                            Math.floor(cube.minX),
                            Math.floor(cube.minY),
                            Math.floor(cube.minZ));
                    mesh.translate(new Vector3d(-textureOffset.x, -textureOffset.y, -textureOffset.z));
                    mesh.setTextures(block, metadata);
                    mesh.translate(textureOffset);
                    boolean lightingWasEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                    GL11.glBegin(GL11.GL_TRIANGLES);
                    for (Triangle3d triangle : mesh.getTriangles()) {
                        GL11.glTexCoord2d(triangle.getTex1().x, triangle.getTex1().y);
                        GL11.glVertex3d(triangle.getP1().x, triangle.getP1().y, triangle.getP1().z);
                        GL11.glTexCoord2d(triangle.getTex2().x, triangle.getTex2().y);
                        GL11.glVertex3d(triangle.getP2().x, triangle.getP2().y, triangle.getP2().z);
                        GL11.glTexCoord2d(triangle.getTex3().x, triangle.getTex3().y);
                        GL11.glVertex3d(triangle.getP3().x, triangle.getP3().y, triangle.getP3().z);
                    }
                    GL11.glEnd();
                    GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                    if (lightingWasEnabled) {
                        GL11.glEnable(GL11.GL_LIGHTING);
                    }
                    continue;
                }
            }

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
