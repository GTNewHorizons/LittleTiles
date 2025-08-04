package com.creativemd.littletiles.waila;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

import cpw.mods.fml.common.event.FMLInterModComms;
import mcp.mobius.waila.api.IWailaRegistrar;

public class Waila {

    public static void callbackRegister(IWailaRegistrar registrar) {
        registrar.registerBodyProvider(new WailaLittleTileHandler(), TileEntityLittleTiles.class);
    }

    public static void init() {
        FMLInterModComms.sendMessage("Waila", "register", Waila.class.getName() + ".callbackRegister");
    }
}
