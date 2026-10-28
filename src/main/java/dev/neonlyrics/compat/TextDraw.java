package dev.neonlyrics.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.Font.GlyphVisitor;
import net.minecraft.client.gui.Font.PreparedText;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.TextRenderable.Styled;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

// $VF: Compiled from TextDraw.java
public final class TextDraw {
   private TextDraw() {
   }

   public static void drawInBatch(
      Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers, DisplayMode mode, int bg, int light
   ) {
      draw(font.prepareText(text.getVisualOrderText(), x, y, color, shadow, false, bg), pose, buffers, mode, light);
   }

   public static void drawInBatch(
      Font font, String text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers, DisplayMode mode, int bg, int light
   ) {
      draw(font.prepareText(text, x, y, color, shadow, bg), pose, buffers, mode, light);
   }

   private static void draw(PreparedText text, final Matrix4f pose, final MultiBufferSource buffers, final DisplayMode mode, final int light) {
      text.visit(new GlyphVisitor()      // $VF: Compiled from TextDraw.java
 {
         public void acceptGlyph(Styled glyph) {
            this.put(glyph);
         }

         public void acceptEffect(TextRenderable effect) {
            this.put(effect);
         }

         public void acceptRenderable(TextRenderable r) {
            this.put(r);
         }

         private void put(TextRenderable r) {
            r.render(pose, buffers.getBuffer(r.renderType(mode)), light, false);
         }
      });
   }
}
