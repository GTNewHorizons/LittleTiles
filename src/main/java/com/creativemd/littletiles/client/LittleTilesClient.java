package com.creativemd.littletiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.client.render.BlockOverlayRenderer;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.client.render.SpecialBlockTilesRenderer;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.server.LittleTilesServer;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LittleTilesClient extends LittleTilesServer {

    public static int modelID;
    public static SpecialBlockTilesRenderer renderer = new SpecialBlockTilesRenderer();
    public static KeyBinding flip = new KeyBinding("key.littletiles.flip", 0, "key.categories.littletiles");
    public static boolean pressedFlip = false;
    public static KeyBinding mark = new KeyBinding("key.littletiles.mark", 0, "key.categories.littletiles");
    public static boolean pressedMark = false;
    public static KeyBinding up = new KeyBinding("key.littletiles.rotateup", 0, "key.categories.littletiles");
    public static boolean pressedUp = false;
    public static KeyBinding down = new KeyBinding("key.littletiles.rotatedown", 0, "key.categories.littletiles");
    public static boolean pressedDown = false;
    public static KeyBinding right = new KeyBinding("key.littletiles.rotateright", 0, "key.categories.littletiles");
    public static boolean pressedRight = false;
    public static KeyBinding left = new KeyBinding("key.littletiles.rotateleft", 0, "key.categories.littletiles");
    public static boolean pressedLeft = false;
    public static KeyBinding toolConfig = new KeyBinding(
            "key.littletiles.tool_config",
            0,
            "key.categories.littletiles");
    public static boolean pressedToolConfig = false;

    @Override
    public void loadSide() {
        modelID = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(modelID, renderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLittleTiles.class, new SpecialBlockTilesRenderer());
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(LittleTiles.blockTile), renderer);
        MinecraftForgeClient.registerItemRenderer(LittleTiles.recipe, renderer);
        MinecraftForgeClient.registerItemRenderer(LittleTiles.multiTiles, renderer);
        BlockTile.mc = Minecraft.getMinecraft();
        FMLCommonHandler.instance().bus().register(new PreviewRenderer());
        MinecraftForge.EVENT_BUS.register(new PreviewRenderer());
        ClientRegistry.registerKeyBinding(up);
        ClientRegistry.registerKeyBinding(down);
        ClientRegistry.registerKeyBinding(right);
        ClientRegistry.registerKeyBinding(left);
        ClientRegistry.registerKeyBinding(flip);
        ClientRegistry.registerKeyBinding(mark);
        ClientRegistry.registerKeyBinding(toolConfig);
        MinecraftForgeClient.registerItemRenderer(LittleTiles.chisel, new BlockOverlayRenderer());
    }

}
