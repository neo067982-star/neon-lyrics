package dev.neonlyrics.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

// $VF: Compiled from MultiBufferSource.java
public interface MultiBufferSource {
   VertexConsumer getBuffer(RenderType var1);

   // $VF: Compiled from MultiBufferSource.java
   interface BufferSource extends MultiBufferSource {
      void endBatch(RenderType var1);
   }
}
