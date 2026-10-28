package dev.neonlyrics.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Small mappings bridge for the client API changes between 26.1 and 26.2. */
public final class MinecraftCompat {
   private static boolean guiHidden;

   private MinecraftCompat() {
   }

   public static InputConstants.Type keyType() {
      try {
         return (InputConstants.Type) InputConstants.Type.class.getField("KEYSYM").get(null);
      } catch (ReflectiveOperationException ignored) {
         try {
            return (InputConstants.Type) InputConstants.Type.class.getField("KEYBOARD").get(null);
         } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to access the keyboard input type", error);
         }
      }
   }

   public static boolean primaryMouseButton(MouseButtonEvent event) {
      String version = Minecraft.getInstance().getLaunchedVersion();
      int primary = version != null && version.startsWith("26.3") ? 1 : 0;
      return event.button() == primary;
   }

   public static void setSkyColor(SkyRenderState state, int color) {
      try {
         Field field = state.getClass().getField("skyColor");
         if (field.getType() == int.class) {
            field.setInt(state, color);
         } else {
            float r = ((color >> 16) & 255) / 255.0F;
            float g = ((color >> 8) & 255) / 255.0F;
            float b = (color & 255) / 255.0F;
            field.set(state, new Vector3f(r, g, b));
         }
      } catch (ReflectiveOperationException error) {
         throw new IllegalStateException("Unable to set the sky color", error);
      }
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
