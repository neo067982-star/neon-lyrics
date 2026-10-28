package dev.neonlyrics.mixin;

import dev.neonlyrics.sky.WorldSky;
import dev.neonlyrics.util.MinecraftCompat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
public abstract class SkyRendererMixin {
   @Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
   private void neonlyrics$applySkyColor(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      int color = WorldSky.vanillaSkyColor();
      if (color >= 0) {
         MinecraftCompat.setSkyColor(state, color);
      }
   }
}
