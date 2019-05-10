package net.doubledoordev.itemblacklist.client;

import net.doubledoordev.itemblacklist.util.ItemBlacklisted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import org.lwjgl.opengl.GL11;

public class Renderer implements IItemRenderer {
   private EntityItem entityItem;
   private RenderItem rendererItem;

   public Renderer() {
      this.entityItem = new EntityItem(Minecraft.getMinecraft().theWorld);
      this.rendererItem = new RenderItem() {
         public boolean shouldSpreadItems() {
            return false;
         }

         public boolean shouldBob() {
            return false;
         }
      };
      this.rendererItem.setRenderManager(RenderManager.instance);
   }

   public boolean handleRenderType(ItemStack item, ItemRenderType type) {
      if (!ItemBlacklisted.canUnpack(item)) {
         return false;
      } else {
         ItemStack unpacked = ItemBlacklisted.unpack(item);
         if (unpacked == item) {
            return false;
         } else {
            IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
            if (renderer != null) {
               return renderer.handleRenderType(unpacked, type);
            } else {
               return unpacked.getItem().getSpriteNumber() != ItemBlacklisted.I.getSpriteNumber();
            }
         }
      }
   }

   public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
      ItemStack unpacked = ItemBlacklisted.unpack(item);
      if (unpacked == item) {
         return helper != ItemRendererHelper.INVENTORY_BLOCK;
      } else {
         IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
         if (renderer != null) {
            return renderer.shouldUseRenderHelper(type, unpacked, helper);
         } else {
            return helper != ItemRendererHelper.INVENTORY_BLOCK;
         }
      }
   }

   public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
      ItemStack unpacked = ItemBlacklisted.unpack(item);
      if (unpacked != item) {
         if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.INVENTORY) {
            unpacked.stackSize = 1;
         }

         IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
         if (renderer != null) {
            renderer.renderItem(type, unpacked, data);
         } else {
            try {
               GL11.glPushMatrix();
               float scale = 4.0F;
               if (type == ItemRenderType.INVENTORY) {
                  scale = 4.0F;
                  GL11.glPushMatrix();
                  GL11.glTranslatef(-2.0F, 3.0F, -3.0F);
                  GL11.glScalef(10.0F, 10.0F, 10.0F);
                  GL11.glTranslatef(1.0F, 0.5F, 1.0F);
                  GL11.glScalef(1.0F, 1.0F, -1.0F);
                  GL11.glRotatef(210.0F, 1.0F, 0.0F, 0.0F);
                  GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
               } else if (type == ItemRenderType.ENTITY) {
                  scale = 2.0F;
               } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                  GL11.glTranslatef(0.6F, 0.65F, 0.5F);
               } else if (type == ItemRenderType.EQUIPPED) {
                  GL11.glTranslatef(0.6F, 0.3F, 0.6F);
               }

               GL11.glScalef(scale, scale, scale);
               this.entityItem.setEntityItemStack(unpacked);
               RenderHelper.enableStandardItemLighting();
               this.rendererItem.doRender(this.entityItem, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
               RenderHelper.disableStandardItemLighting();
               if (type == ItemRenderType.INVENTORY) {
                  GL11.glPopMatrix();
                  GL11.glPushMatrix();
                  GL11.glDisable(2929);
                  Minecraft.getMinecraft().getTextureManager().bindTexture(Minecraft.getMinecraft().getTextureManager().getResourceLocation(ItemBlacklisted.I.getSpriteNumber()));
                  this.rendererItem.renderIcon(0, 0, item.getIconIndex(), 16, 16);
                  GL11.glEnable(2929);
                  GL11.glPopMatrix();
               }

               GL11.glPopMatrix();
            } catch (Exception var7) {
               var7.printStackTrace();
            }

         }
      }
   }
}
