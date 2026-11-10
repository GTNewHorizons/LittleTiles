package com.creativemd.littletiles;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class LittleTilesConfig {

    private static final String CATEGORY_LITTLE_BAG = "little_bag";
    private static final String CATEGORY_CHISEL = "chisel";
    private static final String CATEGORY_RECIPE = "recipe";

    public static final int DEFAULT_MAX_BAG_MATERIALS = 200;
    /** Keeps a config that was edited by hand from creating an absurd amount of slots. */
    public static final int MAX_BAG_MATERIALS_LIMIT = 10000;

    /**
     * How many different materials a single little bag can hold to keep the amount of slots in check.
     */
    public static int maxBagMaterials = DEFAULT_MAX_BAG_MATERIALS;

    public static final int DEFAULT_MAX_CHISEL_BLOCKS = 100;

    /** How many blocks a single chisel placement may reach into, to keep the server from being overloaded. */
    public static int maxChiselBlocks = DEFAULT_MAX_CHISEL_BLOCKS;

    public static final int DEFAULT_MAX_RECIPE_BLOCKS = 1000;

    /** How many blocks the area of a recipe may cover, every one of them is looked at when it is saved. */
    public static int maxRecipeBlocks = DEFAULT_MAX_RECIPE_BLOCKS;

    public static final int DEFAULT_MAX_RECIPE_TILES = 1024;

    /** How many tiles a recipe may save, to keep the size of the item in check. */
    public static int maxRecipeTiles = DEFAULT_MAX_RECIPE_TILES;

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        maxBagMaterials = config.getInt(
                "maxMaterials",
                CATEGORY_LITTLE_BAG,
                DEFAULT_MAX_BAG_MATERIALS,
                1,
                MAX_BAG_MATERIALS_LIMIT,
                "How many different materials a single little bag can hold. Every material needs a slot of its own for the tiles that do not add up to a whole block, so this is what keeps the size of the bag in check. Has to be the same on the client and on the server. Lowering it keeps what bags hold already, a bag above the limit only refuses new materials until enough were taken out.");

        maxChiselBlocks = config.getInt(
                "maxBlocks",
                CATEGORY_CHISEL,
                DEFAULT_MAX_CHISEL_BLOCKS,
                1,
                Integer.MAX_VALUE,
                "How many blocks a single chisel placement may reach into. Every block becomes a tile entity of its own, so big placements can overload the server. Bigger placements are refused with a message. Should be the same on the client and on the server, the client checks it before placing as well.");

        maxRecipeTiles = config.getInt(
                "maxTiles",
                CATEGORY_RECIPE,
                DEFAULT_MAX_RECIPE_TILES,
                1,
                Integer.MAX_VALUE,
                "How many tiles a recipe may save. Every tile is stored in the item, and big items are sent to the clients again and again. Selections with more tiles are refused with a message. Recipes saved before keep their tiles, but can't be placed while they have more tiles than this.");

        maxRecipeBlocks = config.getInt(
                "maxBlocks",
                CATEGORY_RECIPE,
                DEFAULT_MAX_RECIPE_BLOCKS,
                1,
                Integer.MAX_VALUE,
                "How many blocks the area of a recipe may cover. Every block in it is looked at when the recipe is saved, which loads its chunk, so big areas can overload the server. Bigger areas are refused with a message.");

        if (config.hasChanged()) config.save();
    }
}
