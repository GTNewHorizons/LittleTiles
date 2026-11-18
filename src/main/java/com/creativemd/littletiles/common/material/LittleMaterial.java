package com.creativemd.littletiles.common.material;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.BlockValidator;

/**
 * The material little tiles are made of: a block and its meta.
 */
public class LittleMaterial {

    private static final String BLOCK_TAG = "block";
    private static final String META_TAG = "meta";

    public final String blockName;
    public final int meta;

    public LittleMaterial(String blockName, int meta) {
        this.blockName = blockName;
        this.meta = meta;
    }

    public LittleMaterial(Block block, int meta) {
        this(Block.blockRegistry.getNameForObject(block), meta);
    }

    /** The block this material is made of, or null if it does not exist right now, e.g. because a mod was removed. */
    public Block getBlock() {
        return blockName == null ? null : Block.getBlockFromName(blockName);
    }

    /**
     * This material after the block migrations registered to Postea, e.g. for a block of a removed mod that was
     * replaced by the one of another mod. Returns this material itself if no migration applies.
     */
    public LittleMaterial migrate() {
        if (LittleTiles.posteaCompat == null || blockName == null || blockName.isEmpty()) return this;
        LittleMaterial migrated = LittleTiles.posteaCompat.transformBlock(blockName, meta);
        return migrated != null ? migrated : this;
    }

    /** Whether this material can be stored at all, see {@link BlockValidator}. */
    public boolean isValid() {
        return BlockValidator.isBlockValid(getBlock());
    }

    /** Creates a stack of this material, or null if the block does not exist anymore. */
    public ItemStack createItemStack(int blocks) {
        Block block = getBlock();
        if (block == null) return null;
        return new ItemStack(block, blocks, meta);
    }

    /** Identifies the material in nbt, so that the same block and meta always end up in the same place. */
    public String getKey() {
        return blockName + "@" + meta;
    }

    /** Reads the material and applies the block migrations to it, see {@link #migrate()}. */
    public static LittleMaterial readFromNBT(NBTTagCompound nbt) {
        return new LittleMaterial(nbt.getString(BLOCK_TAG), nbt.getInteger(META_TAG)).migrate();
    }

    /**
     * Rewrites the block and meta stored in the given nbt, e.g. of a tile, if a block migration applies to them, see
     * {@link #migrate()}.
     */
    public static void migrateNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(BLOCK_TAG)) return;
        LittleMaterial original = new LittleMaterial(nbt.getString(BLOCK_TAG), nbt.getInteger(META_TAG));
        LittleMaterial migrated = original.migrate();
        if (migrated != original) migrated.writeToNBT(nbt);
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString(BLOCK_TAG, blockName);
        nbt.setInteger(META_TAG, meta);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LittleMaterial material)) return false;

        return meta == material.meta && Objects.equals(blockName, material.blockName);
    }

    @Override
    public int hashCode() {
        return (blockName == null ? 0 : blockName.hashCode()) * 31 + meta;
    }
}
