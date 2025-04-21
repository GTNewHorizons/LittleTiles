package com.creativemd.creativecore.client.rendering;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.creativemd.creativecore.common.utils.RotationUtils;

public class EffectUtils {

    public static void spawnParticles(World world, String name, int x, int y, int z, Vec3 offset, Vec3 motion,
            ForgeDirection direction) {
        RotationUtils.applyVectorRotation(offset, direction);
        RotationUtils.applyVectorRotation(motion, direction);

        world.spawnParticle(
                name,
                x + 0.5 + offset.xCoord,
                y + 0.5 + offset.yCoord,
                z + 0.5 + offset.zCoord,
                motion.xCoord,
                motion.yCoord,
                motion.zCoord);
    }

}
