package dev.neonlyrics.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Small mappings bridge for the client API changes between 26.1 and 26.2. */
public final class MinecraftCompat {
   private static boolean guiHidden;

   private MinecraftCompat() {
   }

   public static Screen screen(Minecraft minecraft) {
      try {
         Field field = Minecraft.class.getField("screen");
         return (Screen) field.get(minecraft);
      } catch (ReflectiveOperationException ignored) {
         try {
            return (Screen) minecraft.gui.getClass().getMethod("screen").invoke(minecraft.gui);
         } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access the current Minecraft screen", error);
         }
      }
   }

   public static void setScreen(Minecraft minecraft, Screen screen) {
      try {
         Method method = Minecraft.class.getMethod("setScreen", Screen.class);
         method.invoke(minecraft, screen);
      } catch (ReflectiveOperationException ignored) {
         try {
            minecraft.gui.getClass().getMethod("setScreen", Screen.class).invoke(minecraft.gui, screen);
         } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to change the Minecraft screen", error);
         }
      }
   }

   public static Camera mainCamera(Minecraft minecraft) {
      try {
         return (Camera) minecraft.gameRenderer.getClass().getMethod("getMainCamera").invoke(minecraft.gameRenderer);
      } catch (ReflectiveOperationException ignored) {
         try {
            return (Camera) minecraft.gameRenderer.getClass().getMethod("mainCamera").invoke(minecraft.gameRenderer);
         } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access the Minecraft camera", error);
         }
      }
   }

   public static ChatComponent chat(Minecraft minecraft) {
      try {
         Object chat = minecraft.gui.getClass().getMethod("getChat").invoke(minecraft.gui);
         return (ChatComponent) chat;
      } catch (ReflectiveOperationException ignored) {
         try {
            Field hud = minecraft.gui.getClass().getField("hud");
            Object hudObject = hud.get(minecraft.gui);
            return (ChatComponent) hudObject.getClass().getMethod("getChat").invoke(hudObject);
         } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access Minecraft chat", error);
         }
      }
   }

   public static boolean guiHidden(Minecraft minecraft) {
      try {
         Field field = minecraft.options.getClass().getField("hideGui");
         return field.getBoolean(minecraft.options);
      } catch (ReflectiveOperationException ignored) {
         return guiHidden;
      }
   }

   public static void toggleGuiHidden(Minecraft minecraft) {
      try {
         Field field = minecraft.options.getClass().getField("hideGui");
         field.setBoolean(minecraft.options, !field.getBoolean(minecraft.options));
      } catch (ReflectiveOperationException ignored) {
         guiHidden = !guiHidden;
      }
   }
}
