package com.creativemd.creativecore.common.event;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.event.world.WorldEvent;

import com.creativemd.creativecore.common.container.ContainerSub;
import com.creativemd.creativecore.common.multiblock.IMultiBlock;
import com.creativemd.creativecore.common.multiblock.MultiBlockStructure;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class TickHandler {

    private int defaultScale;
    private boolean changed;
    public final ArrayList<CreativeCoreEventBus> ClientEvents = new ArrayList<>();
    public final ArrayList<CreativeCoreEventBus> ServerEvents = new ArrayList<>();

    @SideOnly(Side.SERVER)
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent tick) {
        if (tick.phase == Phase.START) { // Remove all Structures which doesn't have any connections
            try {
                for (int i = 0; i < ServerEvents.size(); i++) {
                    for (int j = 0; j < ServerEvents.get(i).eventsToRaise.size(); j++) {
                        ServerEvents.get(i).raiseEvent(ServerEvents.get(i).eventsToRaise.get(j), true);
                    }
                    ServerEvents.get(i).eventsToRaise.clear();
                }
            } catch (Exception e) {
                // It is ready to crash
            }
        } else if (tick.phase == Phase.END) { // Remove all Structures which doesn't have any connections
            int i = 0;
            while (i < MultiBlockStructure.allstructures.size()) {
                if (MultiBlockStructure.allstructures.get(i).connections.isEmpty()) {
                    MultiBlockStructure.allstructures.remove(i);
                } else if (!MultiBlockStructure.allstructures.get(i).isValid()) {
                    for (int j = 0; j < MultiBlockStructure.allstructures.get(i).connections.size(); j++) {
                        ((IMultiBlock) MultiBlockStructure.allstructures.get(i).connections).setStructure(null);
                    }
                    MultiBlockStructure.allstructures.remove(i);
                } else i++;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == Phase.START) {
            try {
                for (int i = 0; i < ClientEvents.size(); i++) {
                    for (int j = 0; j < ClientEvents.get(i).eventsToRaise.size(); j++) {
                        ClientEvents.get(i).raiseEvent(ClientEvents.get(i).eventsToRaise.get(j), true);
                    }
                    ClientEvents.get(i).eventsToRaise.clear();
                }
            } catch (Exception e) {
                // It is ready to crash
            }
        }

        if (mc.thePlayer != null && mc.thePlayer.openContainer instanceof ContainerSub
                && ((ContainerSub) mc.thePlayer.openContainer).gui != null) {
            if (event.phase == Phase.START) {
                if (!changed) defaultScale = mc.gameSettings.guiScale;
                int maxScale = ((ContainerSub) mc.thePlayer.openContainer).gui.getMaxScale(mc);
                int scale = Math.min(defaultScale, maxScale);
                if (defaultScale == 0) scale = maxScale;
                if (scale != mc.gameSettings.guiScale) {
                    changed = true;
                    mc.gameSettings.guiScale = scale;
                    ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
                    int k = scaledresolution.getScaledWidth();
                    int l = scaledresolution.getScaledHeight();
                    mc.currentScreen.setWorldAndResolution(mc, k, l);

                    // mc.loadingScreen = new LoadingScreenRenderer(mc);
                    // mc.updateFramebufferSize();
                }
            }

            // if(event.phase == Phase.END)
            // {
            // mc.gameSettings.guiScale = defaultScale;
            // }
        } else if (event.phase == Phase.START && changed) {
            changed = false;
            mc.gameSettings.guiScale = defaultScale;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) this.ClientEvents.clear();
    }
}
