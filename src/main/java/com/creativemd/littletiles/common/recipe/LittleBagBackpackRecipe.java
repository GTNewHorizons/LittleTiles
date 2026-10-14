package com.creativemd.littletiles.common.recipe;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.creativemd.littletiles.LittleTiles;
import com.creativemd.littletiles.common.items.ItemLittleBag;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import de.eydamos.backpack.Backpack;
import de.eydamos.backpack.item.ItemsBackpack;
import de.eydamos.backpack.misc.BackpackUsageCache;
import de.eydamos.backpack.misc.ConfigurationBackpack;
import de.eydamos.backpack.misc.Constants;
import de.eydamos.backpack.saves.BackpackSave;
import de.eydamos.backpack.util.NBTItemStackUtil;

/**
 * A little bag made from a backpack of the Backpack mod. The bag holds as many stacks as the backpack has slots, so
 * bigger backpacks make bigger bags. Only loaded when the Backpack mod is present.
 */
public class LittleBagBackpackRecipe extends ShapedOreRecipe {

    private static final int BACKPACK_SLOT = 4;
    /** Backpacks are damaged {@code tier * 100 + colour}, the colours are 0 to 16. Anything above is no backpack. */
    private static final int COLOURS = 17;
    private static final int TIERS = 3;

    public LittleBagBackpackRecipe() {
        super(
                ItemLittleBag.create(ConfigurationBackpack.BACKPACK_SLOTS_S),
                " I ",
                "LBL",
                " L ",
                'I',
                new ItemStack(Items.dye, 1, 4),
                'L',
                Items.leather,
                'B',
                new ItemStack(ItemsBackpack.backpack, 1, OreDictionary.WILDCARD_VALUE));
    }

    public static void register() {
        RecipeSorter.register(
                LittleTiles.modid + ":little_bag_backpack",
                LittleBagBackpackRecipe.class,
                RecipeSorter.Category.SHAPED,
                "after:forge:shapedore");
        GameRegistry.addRecipe(new LittleBagBackpackRecipe());
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) return false;

        ItemStack backpack = inventory.getStackInSlot(BACKPACK_SLOT);
        return isPlainBackpack(backpack) && isEmpty(backpack);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        int tier = inventory.getStackInSlot(BACKPACK_SLOT).getItemDamage() / 100;
        int[] slots = { ConfigurationBackpack.BACKPACK_SLOTS_S, ConfigurationBackpack.BACKPACK_SLOTS_M,
                ConfigurationBackpack.BACKPACK_SLOTS_L };
        return ItemLittleBag.create(slots[tier]);
    }

    /** A dyed or undyed backpack of a known tier. The ender backpack shares the item, but has no tier. */
    private static boolean isPlainBackpack(ItemStack backpack) {
        int damage = backpack.getItemDamage();
        return damage / 100 < TIERS && damage % 100 < COLOURS;
    }

    private static boolean isEmpty(ItemStack backpack) {
        if (!NBTItemStackUtil.hasTag(backpack, Constants.NBT.UID)) return true;

        String uid = NBTItemStackUtil.getString(backpack, Constants.NBT.UID);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) return isEmptyOnClient(uid);

        BackpackSave save = new BackpackSave(Backpack.saveFileHandler.loadBackpack(uid));
        return save.getInventory(Constants.NBT.INVENTORY_BACKPACK).tagCount() == 0;
    }

    /**
     * Only decides the preview: vanilla never sends the result slot to the client, but the server crafts on its own
     * decision. The client cannot read the save and asks the server for the used slots, like the backpack tooltip.
     * Until the answer is there the backpack counts as empty; taking a bag the server refuses only puts it back.
     */
    @SideOnly(Side.CLIENT)
    private static boolean isEmptyOnClient(String uid) {
        BackpackUsageCache.BackpackSlotUsageInfo info = BackpackUsageCache.getBackpackInfo(uid);
        if (info == null) {
            BackpackUsageCache.requestBackpackInfo(uid);
            return true;
        }
        return info.usedSlots == 0;
    }
}
