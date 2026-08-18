package com.creativemd.littletiles.client.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import org.joml.Vector3i;

import com.creativemd.littletiles.client.util3d.Mesh3d;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileCutoutInfo;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;

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

        LittleTile tile = findTile(player, mc.objectMouseOver);
        Mesh3d mesh = tile == null ? null : tile.getSimpleMesh();
        if (mesh == null || mesh.getTriangles().isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(ERROR_KEY));
            return;
        }

        File outFile = mesh.dumpMesh();
        dumpMetadata(outFile, tile, mesh);
        sender.addChatMessage(new ChatComponentTranslation(SUCCESS_KEY, "logs/" + outFile.getName()));
    }

    private static LittleTile findTile(EntityPlayer player, MovingObjectPosition look) {
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
        return teLT.loadedTile;
    }

    /** Writes the tile state needed to reproduce and diagnose the mesh beside the OBJ using the same base name. */
    private static void dumpMetadata(File objFile, LittleTile tile, Mesh3d mesh) {
        String objName = objFile.getName();
        String baseName = objName.endsWith(".obj") ? objName.substring(0, objName.length() - 4) : objName;
        File metaFile = new File(objFile.getParentFile(), baseName + ".meta");

        LittleTileBox box = tile.boundingBox;
        LittleTileCutoutInfo cutout = tile.getCutoutInfo();
        StringBuilder meta = new StringBuilder();
        meta.append("format=LittleTiles mesh debug metadata v1\n");
        meta.append("mesh.triangleCount=").append(mesh.getTriangles().size()).append('\n');
        meta.append("tile.class=").append(tile.getClass().getName()).append('\n');
        NBTTagCompound tileNbt = new NBTTagCompound();
        tile.saveTile(tileNbt);
        meta.append("tile.nbt=").append(tileNbt).append('\n');

        if (tile.te != null) {
            meta.append("tileEntity.blockPos=")
                    .append(tile.te.xCoord).append(',').append(tile.te.yCoord).append(',').append(tile.te.zCoord)
                    .append('\n');
        }

        if (box != null) {
            meta.append("boundingBox.grid.min=")
                    .append(box.minX).append(',').append(box.minY).append(',').append(box.minZ).append('\n');
            meta.append("boundingBox.grid.max=")
                    .append(box.maxX).append(',').append(box.maxY).append(',').append(box.maxZ).append('\n');
            meta.append("boundingBox.grid.size=")
                    .append(box.maxX - box.minX).append(',')
                    .append(box.maxY - box.minY).append(',')
                    .append(box.maxZ - box.minZ).append('\n');
            meta.append("boundingBox.block.min=")
                    .append(box.minX / 16.0).append(',').append(box.minY / 16.0).append(',').append(box.minZ / 16.0)
                    .append('\n');
            meta.append("boundingBox.block.max=")
                    .append(box.maxX / 16.0).append(',').append(box.maxY / 16.0).append(',').append(box.maxZ / 16.0)
                    .append('\n');
        }

        if (cutout != null) {
            meta.append("cutout.type=").append(cutout.type).append('\n');
            appendVector(meta, "cutout.size", cutout.size);
            appendVector(meta, "cutout.pos", cutout.pos);
            meta.append("cutout.orientation=").append(cutout.orientation).append('\n');
            meta.append("cutout.thickness=").append(cutout.thickness).append('\n');
            meta.append("cutout.faceStart=").append(cutout.faceStart).append('\n');
            meta.append("cutout.faceEnd=").append(cutout.faceEnd).append('\n');
            meta.append("cutout.negativeAxes=")
                    .append(cutout.negX).append(',').append(cutout.negY).append(',').append(cutout.negZ).append('\n');
            if (cutout.corners != null) {
                LittleTileBox cornerBounds = LittleTileBox.fromPoints(cutout.corners);
                meta.append("cutout.cornerBounds.min=")
                        .append(cornerBounds.minX).append(',')
                        .append(cornerBounds.minY).append(',')
                        .append(cornerBounds.minZ).append('\n');
                meta.append("cutout.cornerBounds.max=")
                        .append(cornerBounds.maxX).append(',')
                        .append(cornerBounds.maxY).append(',')
                        .append(cornerBounds.maxZ).append('\n');
                for (int i = 0; i < cutout.corners.length; i++) {
                    appendVector(meta, "cutout.corner." + i, cutout.corners[i]);
                }
            }

            NBTTagCompound cutoutNbt = new NBTTagCompound();
            cutout.writeToNBT(cutoutNbt);
            meta.append("cutout.nbt=").append(cutoutNbt).append('\n');
        } else {
            meta.append("cutout=null\n");
        }

        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write(meta.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to dump mesh metadata to " + metaFile.getAbsolutePath(), e);
        }
    }

    private static void appendVector(StringBuilder output, String name, Vector3i vector) {
        if (vector == null) {
            output.append(name).append("=null\n");
        } else {
            output.append(name).append('=')
                    .append(vector.x).append(',').append(vector.y).append(',').append(vector.z).append('\n');
        }
    }
}
