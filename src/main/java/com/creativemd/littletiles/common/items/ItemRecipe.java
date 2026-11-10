package com.creativemd.littletiles.common.items;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
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
import com.creativemd.creativecore.common.utils.CubeObject;
import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.LittleTilesConfig;
import com.creativemd.littletiles.client.LittleTilesClient;
import com.creativemd.littletiles.client.render.ITilesRenderer;
import com.creativemd.littletiles.common.blocks.ILittleTile;
import com.creativemd.littletiles.common.gui.StructureConfigWidget;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.LittleStructure.LittleStructureEntry;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.utils.LittleTile;
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
        if (tiles.size() > LittleTilesConfig.maxRecipeTiles) {
            player.addChatMessage(
                    new ChatComponentTranslation(
                            "littletiles.recipe.too_many_tiles",
                            LittleTilesConfig.maxRecipeTiles));
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
                            "littletiles.recipe.too_many_tiles",
                            LittleTilesConfig.maxRecipeTiles));
            return;
        }
        stack.stackTagCompound = (NBTTagCompound) drop.stackTagCompound.copy();
        stack.stackTagCompound.setBoolean(UNLOCKED, true);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        // onItemRightClick(stack, world, player);
        /*
         * if(stack.stackTagCompound != null && !stack.stackTagCompound.hasKey("x")) {
         * if(player.capabilities.isCreativeMode) { ItemStack multiTiles = new ItemStack(LittleTiles.multiTiles);
         * multiTiles.stackTagCompound = stack.stackTagCompound; WorldUtils.dropItem(player, multiTiles); return true; }
         * return false; }
         */

        // if(stack.stackTagCompound == null)
        // stack.stackTagCompound = new NBTTagCompound();

        if (stack.stackTagCompound != null && stack.stackTagCompound.hasKey("x")) {
            if (!world.isRemote) {
                int firstX = stack.stackTagCompound.getInteger("x");
                int firstY = stack.stackTagCompound.getInteger("y");
                int firstZ = stack.stackTagCompound.getInteger("z");
                int minX = Math.min(firstX, x);
                int maxX = Math.max(firstX, x);
                int minY = Math.min(firstY, y);
                int maxY = Math.max(firstY, y);
                int minZ = Math.min(firstZ, z);
                int maxZ = Math.max(firstZ, z);

                ArrayList<LittleTile> tiles = new ArrayList<>();

                // Every block of the area is looked at, which loads its chunk, so the area is limited as well.
                long blocks = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
                if (blocks > LittleTilesConfig.maxRecipeBlocks) {
                    player.addChatMessage(
                            new ChatComponentTranslation(
                                    "littletiles.recipe.area_too_big",
                                    LittleTilesConfig.maxRecipeBlocks));
                    return true;
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
                                    return true;
                                }
                            }
                        }
                    }
                }
                // Only removed once the recipe is saved, a refused selection keeps the first position to try again.
                stack.stackTagCompound.removeTag("x");
                stack.stackTagCompound.removeTag("y");
                stack.stackTagCompound.removeTag("z");
                player.addChatMessage(new ChatComponentText("Second position: x=" + x + ",y=" + y + ",z=" + z));
                saveTiles(world, tiles, stack);
            }
            return true;
        } else if (stack.stackTagCompound == null) {
            if (!world.isRemote) {
                stack.stackTagCompound = new NBTTagCompound();
                stack.stackTagCompound.setInteger("x", x);
                stack.stackTagCompound.setInteger("y", y);
                stack.stackTagCompound.setInteger("z", z);
                player.addChatMessage(new ChatComponentText("First position: x=" + x + ",y=" + y + ",z=" + z));
            }
            return true;
        }
        // A saved recipe places its tiles directly, the recipe itself is kept.
        return Item.getItemFromBlock(LittleTiles.blockTile)
                .onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public ArrayList<LittleTilePreview> getLittlePreview(ItemStack stack) {
        return hasTiles(stack) ? getPreview(stack) : null;
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
        int tiles = stack.stackTagCompound.getInteger("tiles");
        for (int i = 0; i < tiles; i++) {
            NBTTagCompound nbt = stack.stackTagCompound.getCompoundTag("tile" + i);
            LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(nbt);
            LittleTilePreview.flipPreview(nbt, direction);
            if (nbt.hasKey("cutoutOrientation")) {
                new LittleToolHandler(nbt).handleFlip(direction, oldSize);
            }
            stack.stackTagCompound.setTag("tile" + i, nbt);
        }
    }

    public static void rotatePreview(ItemStack stack, ForgeDirection direction) {
        if (!stack.hasTagCompound()) return;
        int tiles = stack.stackTagCompound.getInteger("tiles");
        for (int i = 0; i < tiles; i++) {
            NBTTagCompound nbt = stack.stackTagCompound.getCompoundTag("tile" + i);
            LittleTileSize oldSize = LittleTilePreview.getSizeFromNBT(nbt);
            LittleTilePreview.rotatePreview(nbt, direction);
            if (nbt.hasKey("cutoutOrientation")) {
                new LittleToolHandler(nbt).handleRotation(direction, oldSize);
            }
            stack.stackTagCompound.setTag("tile" + i, nbt);
        }
    }

    public static ArrayList<LittleTilePreview> getPreview(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        ArrayList<LittleTilePreview> result = new ArrayList<>();
        int tiles = stack.stackTagCompound.getInteger("tiles");
        for (int i = 0; i < tiles; i++) {
            NBTTagCompound nbt = stack.stackTagCompound.getCompoundTag("tile" + i);
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
        int tiles = stack.stackTagCompound.getInteger("tiles");
        for (int i = 0; i < tiles; i++) {
            NBTTagCompound nbt = stack.stackTagCompound.getCompoundTag("tile" + i);
            LittleTile tile = LittleTile.CreateandLoadTile(te, te.getWorldObj(), nbt);
            // if(tile != null && tile.isValid())
            result.add(tile);
        }
        return result;
    }

    public static void saveTiles(World world, List<LittleTile> tiles, ItemStack stack) {
        stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("tiles", tiles.size());
        for (int i = 0; i < tiles.size(); i++) {
            NBTTagCompound nbt = new NBTTagCompound();
            tiles.get(i).boundingBox.writeToNBT("bBox", nbt);
            tiles.get(i).saveTileForItem(nbt);
            stack.stackTagCompound.setTag("tile" + i, nbt);
        }
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
        if (stack.stackTagCompound != null) {
            if (stack.stackTagCompound.hasKey("x")) list.add(
                    "First pos: x=" + stack.stackTagCompound.getInteger("x")
                            + ",y="
                            + stack.stackTagCompound.getInteger("y")
                            + ",z="
                            + stack.stackTagCompound.getInteger("z"));
            else {
                String id = "none";
                if (stack.stackTagCompound.hasKey("structure"))
                    id = stack.stackTagCompound.getCompoundTag("structure").getString("id");
                list.add("Structure: " + id);
                list.add("Contains " + stack.stackTagCompound.getInteger("tiles") + " tiles");
            }
        }
        if (hasTiles(stack)) {
            if (isLocked(stack)) list.add("Locked");
            else list.add("Open: middle click on tiles replaces the content (sneak: whole block)");
            list.add("Right click to place");
            list.add(keyName(LittleTilesClient.toolConfig) + ": configure structure, lock or clear");
            list.add("Rotate and flip with the LittleTiles keys");
        } else if (stack.stackTagCompound != null) {
            list.add("Right click on a block to set the second position");
        } else {
            list.add("Right click on a block to set the first position");
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
