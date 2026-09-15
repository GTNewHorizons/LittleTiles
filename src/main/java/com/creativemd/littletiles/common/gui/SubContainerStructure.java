package com.creativemd.littletiles.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.creativemd.creativecore.common.container.SubContainer;
import com.creativemd.littletiles.common.structure.LittleStructure;

public class SubContainerStructure extends SubContainer {

    public ItemStack stack;
    public int index;

    public SubContainerStructure(EntityPlayer player, ItemStack stack) {
        super(player);
        this.stack = stack;
        this.index = player.inventory.currentItem;
    }

    @Override
    public void createControls() {

    }

    @Override
    public void onGuiPacket(int controlID, NBTTagCompound nbt, EntityPlayer player) {
        if (controlID != 0 || nbt == null) return;
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound stackTag = stack.stackTagCompound;

        // If new nbt has no (valid) structure, remove it and exit
        if (!nbt.hasKey("structure", 10)) {
            stackTag.removeTag("structure");
            return;
        }

        NBTTagCompound edited = nbt.getCompoundTag("structure");
        if (!edited.hasKey("id", 8)) return;
        String id = edited.getString("id");
        if (LittleStructure.getClassByID(id) == null) return;

        // Build the new structure tag separately, so an invalid payload leaves the stack untouched
        NBTTagCompound original;
        if (stackTag.hasKey("structure", 10)) original = (NBTTagCompound) stackTag.getCompoundTag("structure").copy();
        else original = new NBTTagCompound();

        original.setString("id", id);

        // Remove old deprecated tags
        original.removeTag("ax");
        original.removeTag("ay");
        original.removeTag("az");

        // Remove old tags
        original.removeTag("avx");
        original.removeTag("avy");
        original.removeTag("avz");
        original.removeTag("axis");
        original.removeTag("ndirection");

        if ("door".equals(id)) {
            if (!edited.hasKey("avx", 3) || !edited.hasKey("avy", 3) || !edited.hasKey("avz", 3)
                    || !edited.hasKey("axis", 3) || !edited.hasKey("ndirection", 3)) return;
            int axis = edited.getInteger("axis");
            int direction = edited.getInteger("ndirection");
            // Axis IDs are X=0, Y=1, Z=2. The GUI stores the normal axis as the
            // positive ForgeDirection: UP=1, SOUTH=3, EAST=5.
            // Reject a normal parallel to the door's rotation axis.
            if (axis < 0 || axis > 2 || (direction != 1 && direction != 3 && direction != 5)
                    || direction == (axis == 0 ? 5 : axis == 1 ? 1 : 3)) return;
            int x = edited.getInteger("avx");
            int y = edited.getInteger("avy");
            int z = edited.getInteger("avz");
            if (Math.abs((long) x) > 4096 || Math.abs((long) y) > 4096 || Math.abs((long) z) > 4096) return;
            original.setInteger("avx", x);
            original.setInteger("avy", y);
            original.setInteger("avz", z);
            original.setInteger("axis", axis);
            original.setInteger("ndirection", direction);
        }

        stackTag.setTag("structure", original);
    }

}
