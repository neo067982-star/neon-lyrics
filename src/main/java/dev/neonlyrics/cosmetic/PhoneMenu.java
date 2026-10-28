package dev.neonlyrics.cosmetic;

import dev.neonlyrics.util.MinecraftCompat;

import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.compat.MultiBufferSource;
import dev.neonlyrics.gui.LyricsScreen;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

// $VF: Compiled from PhoneMenu.java
public final class PhoneMenu {
   private static WorldPanel panel;
   private static boolean attackHeld;
   private static boolean useHeld;

   private PhoneMenu() {
   }

   public static boolean interactive() {
      return NeonLyrics.cfg().menuMode == 1;
   }

   public static void open(Minecraft mc) {
      if (panel != null) {
         if (!panel.closing()) {
            panel.close();
         }
      } else if (interactive() && mc.level != null && mc.player != null) {
         panel = new WorldPanel(mc);
      } else {
         MinecraftCompat.setScreen(mc, new LyricsScreen());
      }
   }

   public static void closeNow() {
      panel = null;
      attackHeld = false;
      useHeld = false;
   }

   public static void menuClosed() {
   }

   public static boolean hidesHand() {
      return false;
   }

   public static void tick(Minecraft mc) {
      if (panel != null) {
         if (mc.level != null && mc.player != null && !panel.finished() && !(mc.player.position().distanceTo(panel.pos()) > 32.0)) {
            panel.tick(mc);
         } else {
            panel = null;
         }
      }
   }

   public static void startTick(Minecraft mc) {
      if (panel != null && MinecraftCompat.screen(mc) == null && panel.hovered()) {
         boolean attackDown = mc.options.keyAttack.isDown();
         boolean useDown = mc.options.keyUse.isDown();
         boolean attackClick = mc.options.keyAttack.consumeClick();
         boolean useClick = mc.options.keyUse.consumeClick();
         if (attackDown && !attackHeld) {
            attackClick = true;
         }

         if (useDown && !useHeld) {
            useClick = true;
         }

         attackHeld = attackDown;
         useHeld = useDown;
         while (attackClick) {
            panel.click();
            attackClick = false;
         }

         while (useClick) {
            panel.click();
            useClick = false;
         }
      } else {
         attackHeld = mc.options.keyAttack.isDown();
         useHeld = mc.options.keyUse.isDown();
      }
   }

   public static void render(Minecraft mc, MultiBufferSource buffers, Matrix4f base, float pt) {
      if (panel != null && mc.player != null) {
         panel.render(mc, buffers, base);
      }
   }
}
