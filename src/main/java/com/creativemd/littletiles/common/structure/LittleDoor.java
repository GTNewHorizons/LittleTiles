package com.creativemd.littletiles.common.structure;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.Rotation;
import com.creativemd.creativecore.common.utils.RotationUtils;
import com.creativemd.creativecore.common.utils.RotationUtils.Axis;
import com.creativemd.littletiles.common.gui.DoorConfigWidget;
import com.creativemd.littletiles.common.gui.StructureConfigWidget;
import com.creativemd.littletiles.common.gui.TileViewerWidget;
import com.creativemd.littletiles.common.placement.LittleTilePlacementPlan;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTilePlaceMode;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.small.LittleTileBox;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;
import com.creativemd.littletiles.utils.PreviewTile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LittleDoor extends LittleStructure {

    @Override
    protected void loadFromNBTExtra(NBTTagCompound nbt) {
        if (nbt.hasKey("ax")) {
            axisVec = new LittleTileVec("a", nbt);
            if (mainTile != null) axisVec.subVec(mainTile.cornerVec);
        } else {
            axisVec = new LittleTileVec("av", nbt);
        }
        axis = Axis.getAxis(nbt.getInteger("axis"));
        normalDirection = ForgeDirection.getOrientation(nbt.getInteger("ndirection"));
    }

    @Override
    protected void writeToNBTExtra(NBTTagCompound nbt) {
        // axisPoint.writeToNBT("a", nbt);
        axisVec.writeToNBT("av", nbt);
        nbt.setInteger("axis", axis.toInt());
        nbt.setInteger("ndirection", RotationUtils.getIndex(normalDirection));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public StructureConfigWidget createConfigWidget(ItemStack stack, LittleStructure structure) {
        return new DoorConfigWidget(stack, structure instanceof LittleDoor ? (LittleDoor) structure : null);
    }

    public ForgeDirection normalDirection;
    public Axis axis;
    public LittleTileVec axisVec;

    public LittleTileVec getAxisVec() {
        LittleTileVec newAxisVec = axisVec.copy();
        newAxisVec.addVec(mainTile.cornerVec);
        return newAxisVec;
    }

    @Override
    public ArrayList<PreviewTile> getSpecialTiles() {
        ArrayList<PreviewTile> boxes = new ArrayList<>();
        LittleTileBox box = new LittleTileBox(
                axisVec.x,
                axisVec.y,
                axisVec.z,
                axisVec.x + 1,
                axisVec.y + 1,
                axisVec.z + 1);

        boxes.add(new PreviewTileAxis(box, null, axis));
        return boxes;
    }

    @Override
    public void onFlip(World world, EntityPlayer player, ItemStack stack, ForgeDirection direction) {
        LittleTileBox box = new LittleTileBox(
                axisVec.x,
                axisVec.y,
                axisVec.z,
                axisVec.x + 1,
                axisVec.y + 1,
                axisVec.z + 1);
        box.flipBoxWithCenter(direction, null);
        axisVec = box.getMinVec();
    }

    @Override
    public void onRotate(World world, EntityPlayer player, ItemStack stack, ForgeDirection direction) {
        LittleTileBox box = new LittleTileBox(
                axisVec.x,
                axisVec.y,
                axisVec.z,
                axisVec.x + 1,
                axisVec.y + 1,
                axisVec.z + 1);
        box.rotateBox(direction);
        axisVec = box.getMinVec();
        // ForgeDirection axisDirection = axis.getDirection();
        this.axis = Axis.getAxis(RotationUtils.rotateForgeDirection(axis.getDirection(), direction));
        // this.axis = Axis.getAxis(direction.getRotation(axis.getDirection()));
        this.normalDirection = RotationUtils.rotateForgeDirection(normalDirection, direction);
        // if(RotationUtils.isNegative(normalDirection))
        // normalDirection = normalDirection.getOpposite();
        /*
         * Axis before = axis; if(axisDirection == Axis.AxisY) { switch(axis) { case AxisX: this.axis = Axis.AxisY;
         * break; case AxisY: this.axis = Axis.AxisX; break; default: break; } }else{ switch(axis) { case AxisX:
         * this.axis = Axis.AxisZ; break; case AxisZ: this.axis = Axis.AxisX; break; default: break; } }
         */
        // if(before != axis)
        // updateNormalDirection();
    }

    /*
     * public static LittleTileVec getSize(ArrayList<PreviewTile> previews) { byte minX = LittleTile.maxPos; byte minY =
     * LittleTile.maxPos; byte minZ = LittleTile.maxPos; byte maxX = LittleTile.minPos; byte maxY = LittleTile.minPos;
     * byte maxZ = LittleTile.minPos; for (int i = 0; i < previews.size(); i++) { PreviewTile tile = previews.get(i);
     * minX = (byte) Math.min(minX, tile.box.minX); minY = (byte) Math.min(minY, tile.box.minY); minZ = (byte)
     * Math.min(minZ, tile.box.minZ); maxX = (byte) Math.max(maxX, tile.box.maxX); maxY = (byte) Math.max(maxY,
     * tile.box.maxY); maxZ = (byte) Math.max(maxZ, tile.box.maxZ); } return new LittleTileVec(maxX-minX, maxY-minY,
     * maxZ-minZ); }
     */

    public boolean tryToPlacePreviews(World world, EntityPlayer player, int x, int y, int z, Rotation direction,
            ArrayList<PreviewTile> defaultpreviews, boolean inverse) {
        LittleTileVec axisPoint = getAxisVec();
        LittleTileVec internalOffset = new LittleTileVec(
                axisPoint.x - x * 16,
                axisPoint.y - y * 16,
                axisPoint.z - z * 16);

        /*
         * LittleTileVec missingOffset = new LittleTileVec(0, 0, 0); switch (direction) { case EAST:
         * //internalOffset.x++; break; case WEST: //missingOffset.x++; break; case UP: //internalOffset.y++; break;
         * case DOWN: //missingOffset.y++; break; case SOUTH: //internalOffset.z++; break; case NORTH:
         * missingOffset.z--; break; default: break; } LittleTileVec missingInfOffset = missingOffset.copy();
         * missingInfOffset.invert();
         */
        ArrayList<PreviewTile> previews = new ArrayList<>();
        // LittleTileVec blockOffset = new LittleTileVec(8, 8, 8);
        // LittleTileVec blockInvOffset = blockOffset.copy();
        // blockInvOffset.invert();
        for (PreviewTile defaultpreview : defaultpreviews) {
            PreviewTile box = defaultpreview.copy();

            // box.box.addOffset(missingOffset);
            box.box.rotateBoxWithCenter(direction, Vec3.createVectorHelper(1 / 32D, 1 / 32D, 1 / 32D));
            // box.box.rotateBoxWithCenter(direction, Vec3.createVectorHelper(0, 0, 0));
            // box.box.addOffset(missingInfOffset);
            // box.box.addOffset(blockInvOffset);
            // box.box.addOffset(new LittleTileVec(0, 0, 1));
            box.box.addOffset(internalOffset);
            previews.add(box);
        }

        /*
         * LittleTileVec size = getSize(previews); switch(direction) { case SOUTH: size.invert(); size.x = 0; size.y =
         * 0; //size.z = 0; break; default: break; }
         */

        /*
         * for (int i = 0; i < previews.size(); i++) { //PreviewTile box = previews.get(i);
         * previews.get(i).box.addOffset(size); }
         */

        LittleDoor structure = new LittleDoor();
        structure.dropStack = dropStack.copy();
        structure.axisVec = new LittleTileVec(0, 0, 0);
        structure.setTiles(new ArrayList<>());
        structure.axis = this.axis;

        ForgeDirection rotationAxis = this.axis.getDirection();
        if (inverse) rotationAxis = rotationAxis.getOpposite();
        structure.normalDirection = this.normalDirection.getRotation(rotationAxis);

        /*
         * Axis directionAxis = Axis.getAxis(this.axis); switch(directionAxis) { case AxisX: if(directionAxis ==
         * Axis.AxisY) structure.normalAxis = Axis.AxisZ; else structure.normalAxis = Axis.AxisY; break; case AxisY:
         * if(directionAxis == Axis.AxisZ) structure.normalAxis = Axis.AxisX; else structure.normalAxis = Axis.AxisZ;
         * break; case AxisZ: if(directionAxis == Axis.AxisY) structure.normalAxis = Axis.AxisX; else
         * structure.normalAxis = Axis.AxisY; break; default: break; }
         */

        LittleTilePlacementPlan plan = new LittleTilePlacementPlan();
        plan.fillPlan(world, x, y, z, previews, structure, LittleTilePlaceMode.NORMAL);
        if (plan.canApplyPlan() && plan.applyPlan(world, player, null, structure).hasPlacedTiles()) {
            ArrayList<LittleTile> tiles = getTiles();
            for (LittleTile littleTile : tiles) {
                littleTile.te.update();
            }
            tiles.clear();
            tiles = structure.getTiles();
            for (LittleTile tile : tiles) {
                tile.te.combineTiles();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, LittleTile tile, int x, int y, int z, EntityPlayer player, int side,
            float moveX, float moveY, float moveZ) {
        if (axis != null) {
            if (!hasLoaded()) {
                player.addChatComponentMessage(
                        new ChatComponentText("Cannot interact with door! Not all tiles are loaded!"));
                return true;
            }
            ArrayList<PreviewTile> previews = new ArrayList<>();

            LittleTileVec axisPoint = getAxisVec();

            int mainX = axisPoint.x / 16;
            int mainY = axisPoint.y / 16;
            int mainZ = axisPoint.z / 16;

            // LittleTileVec internalOffset = new LittleTileVec(axis.x-mainX*16, axis.y-mainY*16, axis.z-mainZ*16);
            // internalOffset.invert();
            LittleTileVec invaxis = axisPoint.copy();
            // internalOffset.invert();
            // invaxis.addVec(internalOffset);
            invaxis.invert();

            // invaxis.addVec(internalOffset);
            ArrayList<LittleTile> tiles = getTiles();
            for (LittleTile tileOfList : tiles) {
                if (tileOfList.boundingBox != null) {
                    NBTTagCompound nbt = new NBTTagCompound();
                    tileOfList.saveTile(nbt);
                    LittleTileBox box = tileOfList.boundingBox.copy();
                    box.addOffset(
                            new LittleTileVec(
                                    tileOfList.te.xCoord * 16,
                                    tileOfList.te.yCoord * 16,
                                    tileOfList.te.zCoord * 16));
                    box.addOffset(invaxis);
                    // box.addOffset(internalOffset);
                    // box.set(-box.minX, -box.minY, -box.minZ, -box.maxX, -box.maxY, -box.maxZ);
                    // box.addOffset(internalOffset);
                    PreviewTile preview = new PreviewTile(box, new LittleTilePreview(box, nbt));
                    previews.add(preview);
                    // tiles.get(i).boundingBoxes.get(j).rotateBox(direction);
                }

            }

            previews.add(new PreviewTileAxis(new LittleTileBox(0, 0, 0, 1, 1, 1), null, axis));

            // Calculate rotation
            Rotation rotation = Rotation.EAST;
            // Axis directionAxis = Axis.getAxis(axis);
            double playerRotation = MathHelper.wrapAngleTo180_float(player.rotationYaw);
            boolean rotX = playerRotation <= -90 || playerRotation >= 90;
            boolean rotY = player.rotationPitch > 0;
            boolean rotZ = playerRotation > 0 && playerRotation <= 180;
            boolean inverse = false;

            switch (axis) {
                case AxisX:
                    // System.out.println(player.rotationPitch);
                    System.out.println(normalDirection);
                    rotation = Rotation.UPX;
                    switch (normalDirection) {
                        case UP:
                            if (!rotY) rotation = Rotation.DOWNX;
                            break;
                        case DOWN:
                            if (rotY) rotation = Rotation.DOWNX;
                            break;
                        case SOUTH:
                            if (!rotX) rotation = Rotation.DOWNX;
                            break;
                        case NORTH:
                            if (rotX) rotation = Rotation.DOWNX;
                            break;
                        default:
                            break;
                    }
                    inverse = rotation == Rotation.UPX;
                    break;
                case AxisY:
                    rotation = Rotation.SOUTH;
                    switch (normalDirection) {
                        case EAST:
                            if (rotX) rotation = Rotation.NORTH;
                            break;
                        case WEST:
                            if (!rotX) rotation = Rotation.NORTH;
                            break;
                        case SOUTH:
                            if (!rotZ) rotation = Rotation.NORTH;
                            break;
                        case NORTH:
                            if (rotZ) rotation = Rotation.NORTH;
                            break;
                        default:
                            break;
                    }
                    inverse = rotation == Rotation.SOUTH;
                    break;
                case AxisZ:
                    // System.out.println(player.rotationPitch);
                    // System.out.println(normalDirection);
                    rotation = Rotation.UP;
                    switch (normalDirection) {
                        case EAST:
                            if (rotZ) rotation = Rotation.DOWN;
                            break;
                        case WEST:
                            if (!rotZ) rotation = Rotation.DOWN;
                            break;
                        case UP:
                            if (!rotY) rotation = Rotation.DOWN;
                            break;
                        case DOWN:
                            if (rotY) rotation = Rotation.DOWN;
                            break;
                        default:
                            break;
                    }
                    inverse = rotation == Rotation.UP;
                    break;
                default:
                    break;
            }

            /*
             * for (int i = 0; i < previews.size(); i++) { previews.get(i).box.addOffset(internalOffset); }
             */

            for (LittleTile value : tiles) {
                value.te.removeTile(value);
            }

            if (tryToPlacePreviews(world, player, mainX, mainY, mainZ, rotation, previews, !inverse)) return true;
            else if (tryToPlacePreviews(world, player, mainX, mainY, mainZ, rotation.getOpposite(), previews, inverse))
                return true;

            for (LittleTile littleTile : tiles) {
                littleTile.te.addTile(littleTile);
            }
            return true;

            // tiles.get(i).te.combineTiles();
        }
        return true;
    }

    public void updateNormalDirection() {
        switch (axis) {
            case AxisX:
                normalDirection = Axis.AxisZ.getDirection();
                break;
            case AxisY:
                normalDirection = Axis.AxisX.getDirection();
                break;
            case AxisZ:
                normalDirection = Axis.AxisY.getDirection();
                break;
            default:
                break;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public LittleStructure parseStructure(StructureConfigWidget config) {
        LittleDoor door = new LittleDoor();
        TileViewerWidget viewer = ((DoorConfigWidget) config).viewer;
        door.axisVec = new LittleTileVec(viewer.axisX, viewer.axisY, viewer.axisZ);
        door.axis = viewer.axisDirection;
        door.normalDirection = viewer.normalAxis.getDirection();
        // door.updateNormalDirection();
        return door;
    }

}
