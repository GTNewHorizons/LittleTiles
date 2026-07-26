package com.creativemd.creativecore.client.rendering;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.client.block.IBlockAccessFake;
import com.creativemd.creativecore.common.utils.ColorUtils;

public class ExtendedRenderBlocks extends RenderBlocks {

    public int meta;

    public int side = -1;
    public int color = ColorUtils.WHITE;
    public IFaceClipper faceClipper;

    @Override
    public IIcon getBlockIcon(Block par1Block, IBlockAccess par2IBlockAccess, int par3, int par4, int par5, int par6) {
        if (side != -1) par6 = side;
        return this.getIconSafe(par1Block.getIcon(par6, meta));
    }

    public ExtendedRenderBlocks() {
        blockAccess = new IBlockAccessFake(null);
    }

    public ExtendedRenderBlocks(RenderBlocks renderer) {
        super();
        this.blockAccess = renderer.blockAccess;
        updateRenderer(renderer);
    }

    public void updateRenderer(RenderBlocks renderer) {
        this.overrideBlockTexture = renderer.overrideBlockTexture;
        this.flipTexture = renderer.flipTexture;
        this.field_152631_f = renderer.field_152631_f;
        this.renderAllFaces = renderer.renderAllFaces;
        this.useInventoryTint = renderer.useInventoryTint;
        this.renderFromInside = renderer.renderFromInside;
        this.enableAO = renderer.enableAO;
    }

    @Override
    public void renderFaceYNeg(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.DOWN, block, x, y, z, icon);
    }

    @Override
    public void renderFaceYPos(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.UP, block, x, y, z, icon);
    }

    @Override
    public void renderFaceZNeg(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.NORTH, block, x, y, z, icon);
    }

    @Override
    public void renderFaceZPos(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.SOUTH, block, x, y, z, icon);
    }

    @Override
    public void renderFaceXNeg(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.WEST, block, x, y, z, icon);
    }

    @Override
    public void renderFaceXPos(Block block, double x, double y, double z, IIcon icon) {
        renderClippedFace(ForgeDirection.EAST, block, x, y, z, icon);
    }

    private void renderClippedFace(ForgeDirection side, Block block, double x, double y, double z, IIcon icon) {
        List<FacePiece> pieces = faceClipper != null ? faceClipper.getFacePieces(side) : null;
        if (pieces == null) {
            renderWholeFace(side, block, x, y, z, icon);
            return;
        }

        double oldMinX = renderMinX;
        double oldMinY = renderMinY;
        double oldMinZ = renderMinZ;
        double oldMaxX = renderMaxX;
        double oldMaxY = renderMaxY;
        double oldMaxZ = renderMaxZ;
        try {
            for (FacePiece piece : pieces) {
                setFacePieceBounds(side, piece);
                renderWholeFace(side, block, x, y, z, icon);
            }
        } finally {
            renderMinX = oldMinX;
            renderMinY = oldMinY;
            renderMinZ = oldMinZ;
            renderMaxX = oldMaxX;
            renderMaxY = oldMaxY;
            renderMaxZ = oldMaxZ;
        }
    }

    private void renderWholeFace(ForgeDirection side, Block block, double x, double y, double z, IIcon icon) {
        switch (side) {
            case DOWN -> super.renderFaceYNeg(block, x, y, z, icon);
            case UP -> super.renderFaceYPos(block, x, y, z, icon);
            case NORTH -> super.renderFaceZNeg(block, x, y, z, icon);
            case SOUTH -> super.renderFaceZPos(block, x, y, z, icon);
            case WEST -> super.renderFaceXNeg(block, x, y, z, icon);
            case EAST -> super.renderFaceXPos(block, x, y, z, icon);
            default -> {}
        }
    }

    private void setFacePieceBounds(ForgeDirection side, FacePiece piece) {
        switch (side) {
            case DOWN, UP -> {
                renderMinX = piece.minPlaneX / 16.0D;
                renderMaxX = piece.maxPlaneX / 16.0D;
                renderMinZ = piece.minPlaneY / 16.0D;
                renderMaxZ = piece.maxPlaneY / 16.0D;
            }
            case NORTH, SOUTH -> {
                renderMinX = piece.minPlaneX / 16.0D;
                renderMaxX = piece.maxPlaneX / 16.0D;
                renderMinY = piece.minPlaneY / 16.0D;
                renderMaxY = piece.maxPlaneY / 16.0D;
            }
            case WEST, EAST -> {
                renderMinZ = piece.minPlaneX / 16.0D;
                renderMaxZ = piece.maxPlaneX / 16.0D;
                renderMinY = piece.minPlaneY / 16.0D;
                renderMaxY = piece.maxPlaneY / 16.0D;
            }
            default -> {}
        }
    }

    @Override
    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        int l = this.color;
        if (this.color == ColorUtils.WHITE) l = block.colorMultiplier(this.blockAccess, x, y, z);
        float f = (float) (l >> 16 & 255) / 255.0F;
        float f1 = (float) (l >> 8 & 255) / 255.0F;
        float f2 = (float) (l & 255) / 255.0F;

        if (EntityRenderer.anaglyphEnable) {
            float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
            float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
            float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
            f = f3;
            f1 = f4;
            f2 = f5;
        }

        return Minecraft.isAmbientOcclusionEnabled() && block.getLightValue() == 0
                ? (this.partialRenderBounds
                        ? this.renderStandardBlockWithAmbientOcclusionPartial(block, x, y, z, f, f1, f2)
                        : this.renderStandardBlockWithAmbientOcclusion(block, x, y, z, f, f1, f2))
                : this.renderStandardBlockWithColorMultiplier(block, x, y, z, f, f1, f2);
    }

}
