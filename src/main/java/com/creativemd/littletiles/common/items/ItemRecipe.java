package com.creativemd.littletiles.common.items;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.DropDownMenu;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.LittleTilesConfig;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.client.render.PreviewRenderer;
import com.creativemd.littletiles.common.blocks.BlockTile;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.gui.StructureConfigWidget;
import com.creativemd.littletiles.common.packet.LittleRecipeSelectPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.LittleStructure.LittleStructureEntry;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
import com.creativemd.littletiles.common.utils.LittleTileBlockPos;
import com.creativemd.littletiles.common.utils.LittleTilePreview;
import com.creativemd.littletiles.common.utils.LittleToolHandler;
import com.creativemd.littletiles.common.utils.small.LittleTileSize;
import com.creativemd.littletiles.common.utils.small.LittleTileVec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemRecipe extends Item implements ITilesRenderer, ILittleTile, IGuiHolder<PlayerInventoryGuiData> {

    private static final String SYNC_STRUCTURE = "lt_recipe_structure";
    private static final String SYNC_CLEAR = "lt_recipe_clear";
    /**
     * Set on an open recipe. A recipe with tiles is locked without it, so one filled by selecting two positions starts
     * locked, while picking keeps it open.
     */
    public static final String UNLOCKED = "unlocked";
    /** An empty recipe selects its area in whole blocks. */
    public static final int GRID = 16;

    @SideOnly(Side.CLIENT)
    public static IIcon lockedIcon;
    @SideOnly(Side.CLIENT)
    public static IIcon unlockedIcon;

    public ItemRecipe() {
        setCreativeTab(CreativeTabs.tabTools);
        hasSubtypes = true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return LittleTiles.modid + ":LTRecipe";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        lockedIcon = register.registerIcon(LittleTiles.modid + ":LTRecipeLocked");
        unlockedIcon = register.registerIcon(LittleTiles.modid + ":LTRecipeUnlocked");
    }

    /** Whether the recipe holds saved tiles, it can only be configured then. */
    public static boolean hasTiles(ItemStack stack) {
        return stack.stackTagCompound != null && !stack.stackTagCompound.hasKey("x");
    }

    /** Whether the stack is an empty recipe, which selects an area on right click. */
    public static boolean isEmptyRecipe(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemRecipe && !hasTiles(stack);
    }

    /** Whether the recipe protects its tiles from being replaced by picking. */
    public static boolean isLocked(ItemStack stack) {
        return hasTiles(stack) && !stack.stackTagCompound.getBoolean(UNLOCKED);
    }

    /**
     * Copies middle clicked tiles into the recipe, replacing what it held. The recipe stays open, so picking again
     * replaces it until it is locked.
     */
    public static void pickTiles(EntityPlayer player, ItemStack stack, List<LittleTile> tiles) {
        if (stack == null || !(stack.getItem() instanceof ItemRecipe) || isLocked(stack) || tiles.isEmpty()) return;
        // Only an entire block can have more tiles, the same limit as when taking it
        if (!BlockTile.fitsOneItem(tiles)) {
            BlockTile.tellTooManyTiles(player);
            return;
        }
        saveTiles(player.worldObj, tiles, stack);
        stack.stackTagCompound.setBoolean(UNLOCKED, true);
    }

    /**
     * Copies a middle clicked structure into the recipe, from the item it drops, like the creative pick would. Same
     * rules as {@link #pickTiles}.
     */
    public static void pickStructure(EntityPlayer player, ItemStack stack, ItemStack drop) {
        if (stack == null || !(stack.getItem() instanceof ItemRecipe) || isLocked(stack)) return;
        if (drop == null || drop.stackTagCompound == null || !drop.stackTagCompound.hasKey("structure", 10)) return;
        if (drop.stackTagCompound.getInteger("tiles") > LittleTilesConfig.maxRecipeTiles) {
            player.addChatMessage(
                    new ChatComponentTranslation(
                            "littletiles.recipe.structure_too_big",
                            LittleTilesConfig.maxRecipeTiles));
            return;
        }
        stack.stackTagCompound = (NBTTagCompound) drop.stackTagCompound.copy();
        stack.stackTagCompound.setBoolean(UNLOCKED, true);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        if (!hasTiles(stack)) {
            // Selecting is client only, like the chisel's two hits. The server only gets both corners once it is done.
            if (world.isRemote) selectPosition(new LittleTileBlockPos(x, y, z, 0, 0, 0, ForgeDirection.UNKNOWN));
            return true;
        }
        // A saved recipe places its tiles directly, the recipe itself is kept.
        return Item.getItemFromBlock(LittleTiles.blockTile)
                .onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    /**
     * The first right click sets the first corner, the second one finishes the area at the clicked block, or at the
     * marked one. The selection is kept until the recipe is saved, so a refused area can be adjusted and sent again.
     */
    @SideOnly(Side.CLIENT)
    private static void selectPosition(LittleTileBlockPos clicked) {
        LittleTileBlockPos second = PreviewRenderer.markedHit != null ? PreviewRenderer.markedHit : clicked;
        if (PreviewRenderer.firstHit == null && PreviewRenderer.markedHit == null) {
            PreviewRenderer.firstHit = second;
            return;
        }
        LittleTileBlockPos first = PreviewRenderer.firstHit != null ? PreviewRenderer.firstHit : second;
        PacketHandler.sendPacketToServer(
                new LittleRecipeSelectPacket(
                        first.getPosX(),
                        first.getPosY(),
                        first.getPosZ(),
                        second.getPosX(),
                        second.getPosY(),
                        second.getPosZ()));
    }

    /** Saves the tiles of the area between both corners into an empty recipe. */
    public static void selectArea(EntityPlayer player, ItemStack stack, int firstX, int firstY, int firstZ, int secondX,
            int secondY, int secondZ) {
        if (stack == null || !(stack.getItem() instanceof ItemRecipe) || hasTiles(stack)) return;
        World world = player.worldObj;
        int minX = Math.min(firstX, secondX);
        int maxX = Math.max(firstX, secondX);
        int minY = Math.min(firstY, secondY);
        int maxY = Math.max(firstY, secondY);
        int minZ = Math.min(firstZ, secondZ);
        int maxZ = Math.max(firstZ, secondZ);

        ArrayList<LittleTile> tiles = new ArrayList<>();

        // Every block of the area is looked at, which loads its chunk, so the area is limited as well.
        long blocks = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (blocks > LittleTilesConfig.maxRecipeBlocks) {
            player.addChatMessage(
                    new ChatComponentTranslation("littletiles.recipe.area_too_big", LittleTilesConfig.maxRecipeBlocks));
            return;
        }

        for (int posX = minX; posX <= maxX; posX++) {
            for (int posY = minY; posY <= maxY; posY++) {
                for (int posZ = minZ; posZ <= maxZ; posZ++) {
                    TileEntity tileEntity = world.getTileEntity(posX, posY, posZ);
                    if (tileEntity instanceof TileEntityLittleTiles) {
                        LittleTileVec offset = new LittleTileVec(
                                (posX - minX) * 16,
                                (posY - minY) * 16,
                                (posZ - minZ) * 16);
                        TileEntityLittleTiles littleEntity = (TileEntityLittleTiles) tileEntity;
                        List<LittleTile> teTiles = littleEntity.getTiles();
                        for (LittleTile teTile : teTiles) {
                            LittleTile tile = teTile.copy();
                            if (tile.boundingBox != null) {
                                tile.boundingBox.addOffset(offset);
                            }
                            tiles.add(tile);
                        }
                        if (tiles.size() > LittleTilesConfig.maxRecipeTiles) {
                            player.addChatMessage(
                                    new ChatComponentTranslation(
                                            "littletiles.recipe.too_many_tiles",
                                            LittleTilesConfig.maxRecipeTiles));
                            return;
                        }
                    }
                }
            }
        }
        // Empty blocks before the tiles are left out, each tile keeps its place in its block
        int tilesMinX = Integer.MAX_VALUE, tilesMinY = Integer.MAX_VALUE, tilesMinZ = Integer.MAX_VALUE;
        for (LittleTile tile : tiles) {
            if (tile.boundingBox == null) continue;
            tilesMinX = Math.min(tilesMinX, tile.boundingBox.minX);
            tilesMinY = Math.min(tilesMinY, tile.boundingBox.minY);
            tilesMinZ = Math.min(tilesMinZ, tile.boundingBox.minZ);
        }
        if (tilesMinX != Integer.MAX_VALUE) {
            LittleTileVec emptyBlocks = new LittleTileVec(
                    Math.floorDiv(tilesMinX, 16) * 16,
                    Math.floorDiv(tilesMinY, 16) * 16,
                    Math.floorDiv(tilesMinZ, 16) * 16);
            for (LittleTile tile : tiles) {
                if (tile.boundingBox != null) tile.boundingBox.subOffset(emptyBlocks);
            }
        }
        saveTiles(world, tiles, stack);
    }

    @Override
    public ArrayList<LittleTilePreview> getLittlePreview(ItemStack stack) {
        return hasTiles(stack) ? getPreview(stack) : null;
    }

    /** Checked every frame while held: the tiles only have to be read when they are placed or previewed. */
    @Override
    public boolean hasLittlePreview(ItemStack stack) {
        return hasTiles(stack);
    }

    @Override
    public void rotateLittlePreview(ItemStack stack, ForgeDirection direction) {
        rotatePreview(stack, direction);
    }

    @Override
    public void flipLittlePreview(ItemStack stack, ForgeDirection direction) {
        flipPreview(stack, direction);
    }

    @Override
    public LittleStructure getLittleStructure(ItemStack stack) {
        return ItemMultiTiles.getLTStructure(stack);
    }

    public static void flipPreview(ItemStack stack, ForgeDirection direction) {
        if (!stack.hasTagCompound()) return;
        List<NBTTagCompound> tiles = readTiles(stack.stackTagCompound);
        for (NBTTagCompound nbt : tiles) {
            LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(nbt);
            LittleTilePreview.flipPreview(nbt, direction);
            if (nbt.hasKey("cutoutOrientation")) {
                new LittleToolHandler(nbt).handleFlip(direction, oldSize);
            }
        }
        writeTiles(stack.stackTagCompound, tiles);
    }

    public static void rotatePreview(ItemStack stack, ForgeDirection direction) {
        if (!stack.hasTagCompound()) return;
        List<NBTTagCompound> tiles = readTiles(stack.stackTagCompound);
        for (NBTTagCompound nbt : tiles) {
            LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(nbt);
            LittleTilePreview.rotatePreview(nbt, direction);
            if (nbt.hasKey("cutoutOrientation")) {
                new LittleToolHandler(nbt).handleRotation(direction, oldSize);
            }
        }
        writeTiles(stack.stackTagCompound, tiles);
    }

    public static ArrayList<LittleTilePreview> getPreview(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        ArrayList<LittleTilePreview> result = new ArrayList<>();
        for (NBTTagCompound nbt : readTiles(stack.stackTagCompound)) {
            LittleTilePreview preview = LittleTilePreview.getPreviewFromNBT(nbt);
            if (preview != null) result.add(preview);
        }
        return result;
    }

    public static LittleTileSize getSize(ItemStack stack) {
        ArrayList<LittleTilePreview> tiles = getPreview(stack);
        if (tiles == null) {
            return new LittleTileSize(0, 0, 0);
        }
        byte minX = LittleTile.maxPos;
        byte minY = LittleTile.maxPos;
        byte minZ = LittleTile.maxPos;
        byte maxX = LittleTile.minPos;
        byte maxY = LittleTile.minPos;
        byte maxZ = LittleTile.minPos;
        for (LittleTilePreview tile : tiles) {
            minX = (byte) Math.min(minX, tile.box.minX);
            minY = (byte) Math.min(minY, tile.box.minY);
            minZ = (byte) Math.min(minZ, tile.box.minZ);
            maxX = (byte) Math.max(maxX, tile.box.maxX);
            maxY = (byte) Math.max(maxY, tile.box.maxY);
            maxZ = (byte) Math.max(maxZ, tile.box.maxZ);
        }
        return new LittleTileSize(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public static ArrayList<LittleTile> loadTiles(TileEntityLittleTiles te, ItemStack stack) {
        ArrayList<LittleTile> result = new ArrayList<>();
        for (NBTTagCompound nbt : readTiles(stack.stackTagCompound)) {
            LittleTile tile = LittleTile.CreateandLoadTile(te, te.getWorldObj(), nbt);
            // if(tile != null && tile.isValid())
            result.add(tile);
        }
        return result;
    }

    public static void saveTiles(World world, List<LittleTile> tiles, ItemStack stack) {
        List<NBTTagCompound> tileNBTs = new ArrayList<>();
        for (LittleTile tile : tiles) {
            NBTTagCompound nbt = new NBTTagCompound();
            tile.boundingBox.writeToNBT("bBox", nbt);
            tile.saveTileForItem(nbt);
            tileNBTs.add(nbt);
        }
        stack.stackTagCompound = new NBTTagCompound();
        writeTiles(stack.stackTagCompound, tileNBTs);
    }

    /**
     * The tiles, all in one byte array. Items are compared on every tick, and comparing one array is far cheaper than
     * comparing a compound per tile.
     */
    private static final String TILE_DATA = "tileData";
    private static final byte TILE_DATA_VERSION = 1;
    /** The layout before {@link #TILE_DATA}: one compound per tile, "tile0", "tile1" and so on. */
    private static final String OLD_TILE_PREFIX = "tile";

    /** The NBT of every tile, in either layout. Changes to it only reach the item through {@link #writeTiles}. */
    public static List<NBTTagCompound> readTiles(NBTTagCompound tag) {
        List<NBTTagCompound> tiles = new ArrayList<>();
        if (!tag.hasKey(TILE_DATA)) {
            int count = tag.getInteger("tiles");
            for (int i = 0; i < count; i++) tiles.add(tag.getCompoundTag(OLD_TILE_PREFIX + i));
            return tiles;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(tag.getByteArray(TILE_DATA)))) {
            byte version = in.readByte();
            if (version != TILE_DATA_VERSION) throw new IOException("Unknown tile data version " + version);
            // Limited like NBT from a packet, the outer limit only counted the bytes, not the lengths inside them
            NBTTagCompound root = CompressedStreamTools.func_152456_a(in, new NBTSizeTracker(2097152L));
            NBTTagList list = root.getTagList("tiles", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) tiles.add(list.getCompoundTagAt(i));
        } catch (IOException | RuntimeException e) {
            LittleTiles.logger.error("Could not read the tiles of a recipe", e);
            tiles.clear();
        }
        return tiles;
    }

    /** Stores the tiles as {@link #TILE_DATA}, dropping the old layout if the item still had it. */
    public static void writeTiles(NBTTagCompound tag, List<NBTTagCompound> tiles) {
        NBTTagList list = new NBTTagList();
        for (NBTTagCompound nbt : tiles) list.appendTag(nbt);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("tiles", list);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(TILE_DATA_VERSION);
            CompressedStreamTools.write(root, out);
        } catch (IOException e) {
            // Written to memory, which does not fail
            throw new UncheckedIOException(e);
        }

        for (int i = 0; tag.hasKey(OLD_TILE_PREFIX + i); i++) tag.removeTag(OLD_TILE_PREFIX + i);
        tag.setByteArray(TILE_DATA, bytes.toByteArray());
        // Kept next to the data, so the tooltip does not have to read it
        tag.setInteger("tiles", tiles.size());
    }

    /**
     * Moves an item still in the old layout over to {@link #TILE_DATA}. Done on the server while it sits in an
     * inventory, so old items are not compared compound by compound forever.
     */
    public static void convertOldTiles(ItemStack stack, World world) {
        if (world.isRemote || !stack.hasTagCompound()) return;
        NBTTagCompound tag = stack.stackTagCompound;
        if (tag.hasKey(TILE_DATA) || !tag.hasKey(OLD_TILE_PREFIX + 0)) return;
        writeTiles(tag, readTiles(tag));
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isHeld) {
        convertOldTiles(stack, world);
    }

    public static ArrayList<CubeObject> getCubes(ItemStack stack) {
        ArrayList<LittleTilePreview> preview = getPreview(stack);
        ArrayList<CubeObject> cubes = new ArrayList<>();
        if (preview != null) {
            for (LittleTilePreview littleTilePreview : preview) {
                cubes.add(littleTilePreview.getCubeBlock());
            }
        }
        return cubes;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (hasTiles(stack)) {
            String id = "none";
            if (stack.stackTagCompound.hasKey("structure"))
                id = stack.stackTagCompound.getCompoundTag("structure").getString("id");
            list.add("Structure: " + id);
            list.add("Contains " + stack.stackTagCompound.getInteger("tiles") + " tiles");
            if (isLocked(stack)) list.add("Locked");
            else list.add("Open: middle click on tiles replaces the content (sneak: whole block)");
            list.add("Right click to place");
            list.add(keyName(LittleTilesClient.toolConfig) + ": configure structure, lock or clear");
            list.add("Rotate and flip with the LittleTiles keys");
        } else {
            list.add("Right click on two blocks to save the area between them");
            list.add(keyName(LittleTilesClient.mark) + ": mark the second position, move it with the LittleTiles keys");
            list.add("Left click on a marked position to move that one instead");
            list.add("Or middle click on tiles to copy them (sneak: whole block)");
        }
    }

    @SideOnly(Side.CLIENT)
    private static String keyName(KeyBinding key) {
        return "'" + GameSettings.getKeyDisplayString(key.getKeyCode()) + "'";
    }

    @Override
    public boolean hasBackground(ItemStack stack) {
        return true;
    }

    @Override
    public ArrayList<CubeObject> getRenderingCubes(ItemStack stack) {
        if (stack.stackTagCompound != null && !stack.stackTagCompound.hasKey("x")) return getCubes(stack);
        return new ArrayList<>();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(LittleTiles.modid, mainPanel);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSyncedAction(SYNC_STRUCTURE, false, true, buffer -> {
            try {
                applyStructure(data.getUsedItemStack(), buffer.readNBTTagCompoundFromBuffer());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        syncManager.registerSyncedAction(SYNC_CLEAR, false, true, buffer -> {
            ItemStack stack = data.getUsedItemStack();
            if (stack != null && stack.getItem() instanceof ItemRecipe) stack.stackTagCompound = null;
        });

        ModularPanel panel = ModularPanel.defaultPanel("recipe");
        panel.size(210, 175);
        // The structure settings are client only, the server just receives the result.
        if (!syncManager.isClient()) return panel;

        ItemStack stack = data.getUsedItemStack();
        LittleStructure saved = ItemMultiTiles.getLTStructure(stack);

        List<String> ids = new ArrayList<>();
        ids.add("none");
        ids.addAll(LittleStructure.getStructureNames());
        int[] selected = { saved != null ? Math.max(0, ids.indexOf(saved.getIDOfStructure())) : 0 };

        Map<String, StructureConfigWidget> configs = new HashMap<>();
        for (String id : ids) {
            LittleStructureEntry entry = LittleStructure.getEntryByID(id);
            if (entry == null) continue;
            LittleStructure structure = saved != null && saved.getIDOfStructure().equals(id) ? saved : null;
            StructureConfigWidget config = entry.parser.createConfigWidget(stack, structure);
            if (config == null) continue;
            config.pos(5, 30).size(200, 120).setEnabledIf(widget -> ids.get(selected[0]).equals(id));
            panel.child(config);
            configs.put(id, config);
        }

        BoolValue locked = new BoolValue(isLocked(stack));
        panel.child(IKey.str("Locked").asWidget().pos(155, 11));
        ToggleButton lockToggle = new ToggleButton().value(locked);
        lockToggle.pos(190, 8).size(14, 14);
        lockToggle.background(GuiTextures.CHECK_BOX_EMPTY).hoverBackground(GuiTextures.CHECK_BOX_EMPTY);
        lockToggle.selectedBackground(GuiTextures.CHECK_BOX_FULL).selectedHoverBackground(GuiTextures.CHECK_BOX_FULL);
        lockToggle.addTooltipElement("An open recipe can be replaced by middle clicking on tiles");
        panel.child(lockToggle);

        ButtonWidget<?> save = new ButtonWidget<>();
        save.overlay(IKey.str("Save")).pos(155, 152).size(50, 18);
        save.onMousePressed(mouseButton -> {
            String id = ids.get(selected[0]);
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("locked", locked.getBoolValue());
            LittleStructureEntry entry = LittleStructure.getEntryByID(id);
            LittleStructure structure = entry != null ? entry.parser.parseStructure(configs.get(id)) : null;
            if (structure != null) {
                NBTTagCompound structureNBT = new NBTTagCompound();
                structure.writeToNBT(structureNBT);
                nbt.setTag("structure", structureNBT);
            }
            syncManager.callSyncedAction(SYNC_STRUCTURE, buffer -> writeNBT(buffer, nbt));
            panel.closeIfOpen();
            return true;
        });
        panel.child(save);

        IPanelHandler clearDialog = IPanelHandler.simple(panel, (parent, player) -> {
            Dialog<Boolean> dialog = new Dialog<>("recipe_clear", confirmed -> {
                if (!Boolean.TRUE.equals(confirmed)) return;
                syncManager.callSyncedAction(SYNC_CLEAR);
                panel.closeIfOpen();
            });
            dialog.size(160, 60);
            dialog.child(IKey.str("Clear the recipe?").asWidget().pos(8, 10));
            ButtonWidget<?> yes = new ButtonWidget<>();
            yes.overlay(IKey.str("Clear")).pos(8, 34).size(68, 18);
            yes.onMousePressed(mouseButton -> {
                dialog.closeWith(true);
                return true;
            });
            ButtonWidget<?> no = new ButtonWidget<>();
            no.overlay(IKey.str("Cancel")).pos(84, 34).size(68, 18);
            no.onMousePressed(mouseButton -> {
                dialog.closeWith(false);
                return true;
            });
            return dialog.child(yes).child(no);
        }, true);
        ButtonWidget<?> clear = new ButtonWidget<>();
        clear.overlay(IKey.str("Clear")).pos(5, 152).size(50, 18).addTooltipElement("Back to selecting an area");
        clear.onMousePressed(mouseButton -> {
            clearDialog.openPanel();
            return true;
        });
        panel.child(clear);

        // Added last so the opened list is drawn over the structure settings.
        panel.child(IKey.str("Type:").asWidget().pos(5, 11));
        DropDownMenu typePicker = new DropDownMenu();
        typePicker.pos(35, 5).size(100, 20);
        typePicker.background(GuiTextures.BUTTON_CLEAN);
        for (int i = 0; i < ids.size(); i++) {
            int index = i;
            typePicker.addChoice(menu -> selected[0] = index, ids.get(i));
        }
        typePicker.setSelectedIndex(selected[0]);
        panel.child(typePicker);
        return panel;
    }

    private static void writeNBT(PacketBuffer buffer, NBTTagCompound nbt) {
        try {
            buffer.writeNBTTagCompoundToBuffer(nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Saves the structure set up in the gui, checking what the client sent. */
    public static void applyStructure(ItemStack stack, NBTTagCompound nbt) {
        if (stack == null || !(stack.getItem() instanceof ItemRecipe) || nbt == null) return;
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        NBTTagCompound stackTag = stack.stackTagCompound;

        if (nbt.hasKey("locked")) {
            if (nbt.getBoolean("locked")) stackTag.removeTag(UNLOCKED);
            else stackTag.setBoolean(UNLOCKED, true);
        }

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
            if (!edited.hasKey("avx", 3) || !edited.hasKey("avy", 3)
                    || !edited.hasKey("avz", 3)
                    || !edited.hasKey("axis", 3)
                    || !edited.hasKey("ndirection", 3))
                return;
            int axis = edited.getInteger("axis");
            int direction = edited.getInteger("ndirection");
            // Axis IDs are X=0, Y=1, Z=2. The GUI stores the normal axis as the
            // positive ForgeDirection: UP=1, SOUTH=3, EAST=5.
            // Reject a normal parallel to the door's rotation axis.
            if (axis < 0 || axis > 2
                    || (direction != 1 && direction != 3 && direction != 5)
                    || direction == (axis == 0 ? 5 : axis == 1 ? 1 : 3))
                return;
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
