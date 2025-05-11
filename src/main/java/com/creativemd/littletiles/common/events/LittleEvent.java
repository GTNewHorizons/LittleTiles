package com.creativemd.littletiles.common.events;

import net.minecraft.inventory.ContainerWorkbench;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LittleEvent {

    @SideOnly(Side.CLIENT)
    public static int renderPass;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPreRenderWorld(RenderWorldEvent.Pre event) {
        renderPass = event.pass;
    }

    @SubscribeEvent
    public void openContainer(PlayerOpenContainerEvent event) {
        if (event.entityPlayer.openContainer instanceof ContainerWorkbench) event.setResult(Result.ALLOW);
    }
}
