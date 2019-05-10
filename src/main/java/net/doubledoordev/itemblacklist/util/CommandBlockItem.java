package net.doubledoordev.itemblacklist.util;

import cpw.mods.fml.common.registry.GameRegistry;
import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.data.BanList;
import net.doubledoordev.itemblacklist.data.BanListEntry;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static net.minecraft.util.EnumChatFormatting.*;

/**
 * @author Dries007
 */
public class CommandBlockItem extends CommandBase
{
   public static class Pair<K, V>
   {
      public K k;
      public V v;

      public Pair(K k, V v)
      {
         this.k = k;
         this.v = v;
      }
   }

   @Override
   public String getCommandName()
   {
      return "blockitem";
   }

   @Override
   public List getCommandAliases()
   {
      return Arrays.asList("itemblacklist", "blacklist","banitem");
   }

   @Override
   public String getCommandUsage(ICommandSender p_71518_1_)
   {
      return "Use '/blockitem help' for more info.";
   }

   @Override
   public void processCommand(ICommandSender sender, String[] args)
   {
      if (args.length == 0)
      {
         sender.addChatMessage(new ChatComponentText("Possible subcommands:").setChatStyle(new ChatStyle().setColor(GOLD)));
         sender.addChatMessage(makeHelpText("reload", "Reloads the config file from disk."));
         sender.addChatMessage(makeHelpText("pack [player]", "Lock banned items in targets inventory."));
         sender.addChatMessage(makeHelpText("unpack [player]", "Unlock banned items in targets inventory."));
         sender.addChatMessage(makeHelpText("list [dim|player]", "List banned items of all, player, or dim"));
         sender.addChatMessage(makeHelpText("ban [dim list] [item[:*|meta]]", "Ban an item."));
         sender.addChatMessage(makeHelpText("unban [dim list] [item[:*|meta]]", "Unban an item."));
         return;
      }
      String arg0 = args[0].toLowerCase();
      boolean unpack = false;
      switch (arg0)
      {
         default:
            throw new WrongUsageException("Unknown subcommand. Use '/blockitem' to get some help.");
         case "reload":
            GlobalBanList.init();
            try {
               File mtDir = new File("scripts");
               if (!mtDir.exists()){
                  ItemBlacklist.logger.error("Não foi possivel encontrar o diretorio do mt");
                  return;
               }
               File script = new File("scripts" + File.separator + "banitem.zs");
               script.createNewFile();
               PrintWriter writer = new PrintWriter(script);
               writer.print("");
               HashSet<BanList> worldSet = new HashSet<>(GlobalBanList.worldInstance.dimesionMap.values());
               worldSet.add(GlobalBanList.worldInstance.getGlobal());
               for (BanList list : worldSet)
               {
                  String dim = list.getDimension();
                  String tooltip;
                  if (dim.equals(GlobalBanList.GLOBAL_NAME))tooltip = "Item Banido";
                  else tooltip = "Item banido no :" + DimensionManager.getWorld(Integer.valueOf(dim)).provider.getDimensionName();
                  for (HashMap.Entry<Item,BanListEntry> entry: list.banListEntryMap.entries())
                  {
                     writer.append("<"+GameRegistry.findUniqueIdentifierFor(entry.getKey())+":"+String.valueOf( entry.getValue().getDamage())+">.addTooltip(format.red(\""+tooltip+"\"));\n");
                  }
               }
               writer.close();

            }
            catch (Exception e){
               e.printStackTrace();
            }
            // No break
            sender.addChatMessage(new ChatComponentText("Reloaded!").setChatStyle(new ChatStyle().setColor(GREEN)));
         case "list":
            list(sender, args);
            break;
         case "unpack":
            unpack = true;
         case "pack":
            EntityPlayer player = args.length > 1 ? getPlayer(sender, args[1]) : getCommandSenderAsPlayer(sender);
            int count = GlobalBanList.process(player.dimension, player.inventory, unpack);
            sender.addChatMessage(new ChatComponentText((unpack ? "Unlocked " : "Locked ") + count + " items."));
            break;
         case "ban":
            try
            {
               Pair<String, BanListEntry> toBan = parse(sender, args);
               GlobalBanList.worldInstance.add(toBan.k, toBan.v);
               sender.addChatMessage(new ChatComponentText("Banned " + toBan.v.toString() + " in " + toBan.k).setChatStyle(new ChatStyle().setColor(GREEN)));
            }
            catch (Exception e)
            {
               if (e instanceof CommandException) throw e;
               e.printStackTrace();
               throw new WrongUsageException(e.getMessage());
            }
            break;
         case "unban":
            try
            {
               Pair<String, BanListEntry> toBan = parse(sender, args);
               if (GlobalBanList.worldInstance.remove(toBan.k, toBan.v)) sender.addChatMessage(new ChatComponentText("Unbanned " + toBan.v.toString() + " in " + toBan.k).setChatStyle(new ChatStyle().setColor(GREEN)));
               else sender.addChatMessage(new ChatComponentText("Can't unban " + toBan.v.toString() + " in " + toBan.k).setChatStyle(new ChatStyle().setColor(RED)));
            }
            catch (Exception e)
            {
               if (e instanceof CommandException) throw e;
               e.printStackTrace();
               throw new WrongUsageException(e.getMessage());
            }
            break;
      }
   }

   private Pair<String, BanListEntry> parse(ICommandSender sender, String[] args)
   {
      String dimensions = null;
      boolean wildcardOverride = false;
      int meta = OreDictionary.WILDCARD_VALUE;
      BanListEntry banListEntry = null;

      for (int i = 1; i < args.length; i++)
      {
         if (args[i].equals(GlobalBanList.GLOBAL_NAME))
         {
            dimensions = GlobalBanList.GLOBAL_NAME;
            continue;
         }
         try
         {
            Helper.parseDimIds(args[i]);
            if (dimensions != null) throw new WrongUsageException("Double dimension specifiers: " + dimensions + " AND " + args[i]);
            dimensions = args[i];
            continue;
         }
         catch (Exception ignored) {}
         try
         {
            String[] split = args[i].split(":");
            if (split.length > 3) throw new WrongUsageException("Item name not valid.");
            meta = split.length == 3 ? parseInt(sender, split[2]) : OreDictionary.WILDCARD_VALUE;
            if (banListEntry != null) throw new WrongUsageException("Double item specifiers: " + banListEntry + " AND " + args[i]);
            banListEntry = new BanListEntry(split[0] + ":" + split[1], meta);
            continue;
         }
         catch (Exception ignored) {}
         if (args[i].equals("*"))
         {
            wildcardOverride = true;
            continue;
         }
         throw new IllegalArgumentException("Not a dimension specifier or valid item: " + args[i]);
      }
      // Default to current dimension and held item
      if (dimensions == null) dimensions = String.valueOf(getCommandSenderAsPlayer(sender).dimension);
      if (banListEntry == null)
      {
         EntityPlayer player = getCommandSenderAsPlayer(sender);
         ItemStack stack = player.getHeldItem();
         if (stack == null) throw new WrongUsageException("No item specified and no item held.");
         if (wildcardOverride) meta = OreDictionary.WILDCARD_VALUE;
         banListEntry = new BanListEntry(GameRegistry.findUniqueIdentifierFor(stack.getItem()), meta);
      }
      return new Pair<>(dimensions, banListEntry);
   }

   private void list(ICommandSender sender, HashSet<BanList> set)
   {
      for (BanList list : set)
      {
         sender.addChatMessage(new ChatComponentText("Dimension " + list.getDimension()).setChatStyle(new ChatStyle().setColor(AQUA)));
         for (BanListEntry entry : list.banListEntryMap.values())
         {
            sender.addChatMessage(new ChatComponentText(entry.toString()));
         }
      }
   }

   private void list(ICommandSender sender, String[] args)
   {
      HashSet<BanList> packSet = new HashSet<>();
      HashSet<BanList> worldSet = new HashSet<>();
      if (args.length == 1)
      {
         worldSet.addAll(GlobalBanList.worldInstance.dimesionMap.values());
         worldSet.add(GlobalBanList.worldInstance.getGlobal());

         if (GlobalBanList.packInstance != null)
         {
            packSet.addAll(GlobalBanList.packInstance.dimesionMap.values());
            packSet.add(GlobalBanList.packInstance.getGlobal());
         }
      }
      else
      {
         if (args[1].equalsIgnoreCase(GlobalBanList.GLOBAL_NAME)) worldSet.add(GlobalBanList.worldInstance.getGlobal());
         else worldSet.addAll(GlobalBanList.worldInstance.dimesionMap.get(getDimension(sender, args[1])));

         if (GlobalBanList.packInstance != null)
         {
            if (args[1].equalsIgnoreCase(GlobalBanList.GLOBAL_NAME)) packSet.add(GlobalBanList.packInstance.getGlobal());
            else packSet.addAll(GlobalBanList.packInstance.dimesionMap.get(getDimension(sender, args[1])));
         }
      }
      if (worldSet.isEmpty()) sender.addChatMessage(new ChatComponentText("No world banned items.").setChatStyle(new ChatStyle().setColor(YELLOW)));
      else
      {
         sender.addChatMessage(new ChatComponentText("World banned items:").setChatStyle(new ChatStyle().setColor(YELLOW)));
         list(sender, worldSet);
      }

      if (packSet.isEmpty()) sender.addChatMessage(new ChatComponentText("No pack banned items. ").setChatStyle(new ChatStyle().setColor(YELLOW)).appendSibling(new ChatComponentText("[unchangeable]").setChatStyle(new ChatStyle().setColor(RED))));
      else
      {
         sender.addChatMessage(new ChatComponentText("Pack banned items: ").setChatStyle(new ChatStyle().setColor(YELLOW)).appendSibling(new ChatComponentText("[unchangeable]").setChatStyle(new ChatStyle().setColor(RED))));
         list(sender, packSet);
      }
   }

   private int getDimension(ICommandSender sender, String arg)
   {
      try
      {
         return parseInt(sender, arg);
      }
      catch (Exception e)
      {
         return getPlayer(sender, arg).dimension;
      }
   }

   public IChatComponent makeHelpText(String name, String text)
   {
      return new ChatComponentText(name).setChatStyle(new ChatStyle().setColor(AQUA)).appendSibling(new ChatComponentText(": " + text).setChatStyle(new ChatStyle().setColor(WHITE)));
   }

   @Override
   public List addTabCompletionOptions(ICommandSender sender, String[] args)
   {
      if (isUsernameIndex(args, args.length)) return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
      if (args.length == 1) return getListOfStringsMatchingLastWord(args, "reload", "pack", "unpack", "list", "ban", "unban");
      if (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban"))
      {
         //noinspection unchecked
         HashSet set = new HashSet();
         //noinspection unchecked
         set.add(GlobalBanList.GLOBAL_NAME);
         //noinspection unchecked
         set.addAll(Item.itemRegistry.getKeys());
         return getListOfStringsFromIterableMatchingLastWord(args, set);
      }
      return null;
   }

   @Override
   public boolean isUsernameIndex(String[] args, int arg)
   {
      if (args.length == 0) return false;
      switch (args[0].toLowerCase())
      {
         case "unpack":
         case "pack":
         case "list":
            return arg == 2;
      }
      return false;
   }
}