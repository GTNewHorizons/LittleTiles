package com.creativemd.littletiles;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class LittleTilesConfig {

    private static final String CATEGORY_LITTLE_BAG = "little_bag";

    public static final int DEFAULT_MAX_BAG_MATERIALS = 200;
    /** Keeps a config that was edited by hand from creating an absurd amount of slots. */
    private static final int MAX_BAG_MATERIALS_LIMIT = 10000;

    /**
     * How many different materials a single little bag can hold to keeps the amount of slots in check.
     */
    public static int maxBagMaterials = DEFAULT_MAX_BAG_MATERIALS;

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        maxBagMaterials = config.getInt(
                "maxMaterials",
                CATEGORY_LITTLE_BAG,
                DEFAULT_MAX_BAG_MATERIALS,
                1,
                MAX_BAG_MATERIALS_LIMIT,
                "How many different materials a single little bag can hold. Every material needs a slot of its own for the tiles that do not add up to a whole block, so this is what keeps the size of the bag in check. Has to be the same on the client and on the server.");

        if (config.hasChanged()) config.save();
    }
}
