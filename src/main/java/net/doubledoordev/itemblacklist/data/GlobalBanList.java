package net.doubledoordev.itemblacklist.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.util.ItemBlacklisted;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Dries007
 */
public class GlobalBanList
{
    public static final String GLOBAL_NAME = "__GLOBAL__";

    public static GlobalBanList instance;
    public final Multimap<Integer, BanList> dimesionMap = HashMultimap.create();
    private BanList global = new BanList(GLOBAL_NAME);

    public static void init()
    {
        File file = Helper.getDataFile();
        if (file.exists())
        {
            try
            {
                String string = FileUtils.readFileToString(file, "UTF-8");
                instance = Helper.GSON.fromJson(string, GlobalBanList.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException("There was an error loading your config file. To prevent damage, the server will be closed.", e);
            }
        }
        else
        {
            instance = new GlobalBanList();
            ItemBlacklist.logger.warn("No config file present.");
        }
    }

    public static void save()
    {
        try
        {
            FileUtils.writeStringToFile(Helper.getDataFile(), Helper.GSON.toJson(instance), "UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean isBanned(int dimensionId, ItemStack item)
    {
        if (instance == null) throw new IllegalStateException("Ban list not initialized.");
        if (item == null || item.getItem() == null) return false;
        if (instance.global.isBanned(item)) return true;
        for (BanList banList : instance.dimesionMap.get(dimensionId)) if (banList.isBanned(item)) return true;
        return false;
    }

    public static int process(int dim, IInventory inventory)
    {
        return process(dim, inventory, false);
    }

    public static int process(int dim, IInventory inventory, boolean unpackOnly)
    {
        int count = 0;
        final int size = inventory.getSizeInventory();
        for (int i = 0; i < size; i++)
        {
            ItemStack itemStack = inventory.getStackInSlot(i);
            if (itemStack == null) continue;
            ItemStack processed = process(dim, itemStack, unpackOnly);
            if (processed != itemStack)
            {
                count++;
                inventory.setInventorySlotContents(i, processed);
            }
        }
        return count;
    }

    public static ItemStack process(int dim, ItemStack itemStack)
    {
        return process(dim, itemStack, false);
    }

    public static ItemStack process(int dim, ItemStack itemStack, boolean unpackOnly)
    {
        if (itemStack == null) return null;

        boolean packed = itemStack.getItem() == ItemBlacklisted.I && ItemBlacklisted.canUnpack(itemStack);
        ItemStack unpacked = packed ? ItemBlacklisted.unpack(itemStack) : itemStack;
        boolean banned = !unpackOnly && isBanned(dim, unpacked);

        if (packed && !banned) return unpacked;
        else if (banned && !packed) return ItemBlacklisted.pack(itemStack);

        return itemStack;
    }

    public void add(String dimensions, BanListEntry banListEntry)
    {
        BanList match = null;
        if (dimensions.equals(GLOBAL_NAME))
        {
            match = global;
        }
        else
        {
            for (BanList banList : new HashSet<>(GlobalBanList.instance.dimesionMap.values()))
            {
                if (banList.dimension.equals(dimensions))
                {
                    if (match != null) throw new IllegalStateException("Duplicate banlist key. This is a serious issue. You should manually try to fix the json file!");
                    match = banList;
                }
            }
        }
        if (match == null)
        {
            match = new BanList(dimensions);
            for (int i : match.getDimIds())
            {
                dimesionMap.put(i, match);
            }
        }
        if (match.banListEntryMap.containsEntry(banListEntry.getItem(), banListEntry)) throw new IllegalArgumentException("Duplicate ban list entry.");
        match.banListEntryMap.put(banListEntry.getItem(), banListEntry);
        save();
    }

    public boolean remove(String dimensions, BanListEntry banListEntry)
    {
        BanList match = null;
        if (dimensions.equals(GLOBAL_NAME))
        {
            match = global;
        }
        else
        {
            for (BanList banList : new HashSet<>(GlobalBanList.instance.dimesionMap.values()))
            {
                if (banList.dimension.equals(dimensions))
                {
                    if (match != null) throw new IllegalStateException("Duplicate banlist key. This is a serious issue. You should manually try to fix the json file!");
                    match = banList;
                }
            }
        }
        if (match == null) return false;
        if (!match.banListEntryMap.containsEntry(banListEntry.getItem(), banListEntry)) return false;
        match.banListEntryMap.remove(banListEntry.getItem(), banListEntry);
        save();
        return true;
    }

    public BanList getGlobal()
    {
        return global;
    }

    public static class Json implements JsonSerializer<GlobalBanList>, JsonDeserializer<GlobalBanList>
    {
        @Override
        public GlobalBanList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            GlobalBanList list = new GlobalBanList();
            JsonObject object = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
            {
                if (list.global.dimension.equals(entry.getKey()))
                {
                    list.global = context.deserialize(entry.getValue(), BanList.class);
                    list.global.dimension = entry.getKey();
                }
                else
                {
                    BanList banList = context.deserialize(entry.getValue(), BanList.class);
                    banList.dimension = entry.getKey();
                    for (int i : banList.getDimIds())
                    {
                        list.dimesionMap.put(i, banList);
                    }
                }
            }
            return list;
        }

        @Override
        public JsonElement serialize(GlobalBanList src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject root = new JsonObject();
            root.add(src.global.dimension, context.serialize(src.global));
            for (BanList banList : src.dimesionMap.values())
            {
                if (!root.has(banList.dimension)) root.add(banList.dimension, context.serialize(banList));
            }
            return root;
        }
    }
}