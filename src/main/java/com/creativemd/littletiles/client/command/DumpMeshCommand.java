package com.creativemd.littletiles.client.command;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;

public class DumpMeshCommand extends CommandBase {

    private static final String ERROR_KEY = "littletiles.command.dumpmesh.error";
    private static final String SUCCESS_KEY = "littletiles.command.dumpmesh.success";

    @Override
    public String getCommandName() {
        return "ltdumpmesh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ltdumpmesh";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;

        Mesh3d mesh = findMesh(player, mc.objectMouseOver);
        if (mesh == null || mesh.getTriangles().isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(ERROR_KEY));
            return;
        }

        File outFile = mesh.dumpMesh();
        sender.addChatMessage(new ChatComponentTranslation(SUCCESS_KEY, "logs/" + outFile.getName()));
    }

    private static Mesh3d findMesh(EntityPlayer player, MovingObjectPosition look) {
        if (look == null || look.typeOfHit != MovingObjectType.BLOCK) {
            return null;
        }
        TileEntity te = player.worldObj.getTileEntity(look.blockX, look.blockY, look.blockZ);
        if (!(te instanceof TileEntityLittleTiles)) {
            return null;
        }
        TileEntityLittleTiles teLT = (TileEntityLittleTiles) te;
        if (!teLT.updateLoadedTile(player)) {
            return null;
        }
        return teLT.loadedTile.getSimpleMesh();
    }
}
