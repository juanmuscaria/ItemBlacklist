package net.doubledoordev.itemblacklist.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

public class ItemBlacklisted extends Item {
   public static final String NAME = "blacklisted";
   public static final ItemBlacklisted I = new ItemBlacklisted();
   private IIcon itemIconError;

   private ItemBlacklisted() {
      this.setUnlocalizedName("blacklisted");
      this.setTextureName("ItemBlacklist".concat(":").concat("blacklisted").toLowerCase());
      this.setMaxStackSize(1);
   }

   public static ItemStack pack(ItemStack in) {
      ItemStack out = new ItemStack(I);
      out.setTagInfo("item", in.writeToNBT(new NBTTagCompound()));
      return out;
   }

   public static boolean canUnpack(ItemStack in) {
      return in != null && in.hasTagCompound() && in.getTagCompound().hasKey("item");
   }

   public static ItemStack unpack(ItemStack in) {
      ItemStack out = null;

      try {
         out = ItemStack.loadItemStackFromNBT(in.getTagCompound().getCompoundTag("item"));
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      return out == null ? in : out;
   }

   public String getUnlocalizedName(ItemStack in) {
      if (!canUnpack(in)) {
         return "_ERROR_";
      } else {
         ItemStack unpack = unpack(in);
         return unpack != in && unpack != null ? unpack.getUnlocalizedName() : "_ERROR_";
      }
   }

   public boolean requiresMultipleRenderPasses() {
      return true;
   }

   @SideOnly(Side.CLIENT)
   public void registerIcons(IIconRegister iconRegister) {
      super.registerIcons(iconRegister);
      this.itemIconError = iconRegister.registerIcon(this.getIconString().concat("_error"));
   }

   @SideOnly(Side.CLIENT)
   public IIcon getIcon(ItemStack stack, int pass) {
      if (canUnpack(stack) && pass == 0) {
         ItemStack unpack = unpack(stack);
         if (unpack.getItemSpriteNumber() == this.getSpriteNumber()) {
            IIcon icon = unpack.getItem().getIcon(unpack, 0);
            if (icon != null) {
               return icon;
            }

            return this.itemIconError;
         }
      }

      return this.itemIcon;
   }
}
