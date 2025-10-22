package com.creativemd.littletiles.common.utils;

import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import net.minecraft.block.Block;

import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandlers;

public class BlockStateSyncValue {

    private final LongSyncValue sync;

    public BlockStateSyncValue(BiConsumer<Block, Integer> callbackChanged) {
        LongSupplier getter = () -> 0;
        LongConsumer setter = (x) -> callbackChanged
                .accept(Block.getBlockById((int) (x >> 32)), (int) (x & 0xFFFFFFFFL));
        sync = SyncHandlers.longNumber(getter, setter);
    }

    public void setValue(Block block, int meta) {
        sync.setValue(((long) Block.getIdFromBlock(block) << 32) | meta);
    }

    public void register(PanelSyncManager syncManager, String name) {
        syncManager.syncValue(name, sync);
    }
}
